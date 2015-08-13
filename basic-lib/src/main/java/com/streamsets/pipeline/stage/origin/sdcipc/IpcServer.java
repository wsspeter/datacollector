/**
 * (c) 2015 StreamSets, Inc. All rights reserved. May not
 * be copied, modified, or distributed in whole or part without
 * written consent of StreamSets, Inc.
 */
package com.streamsets.pipeline.stage.origin.sdcipc;

import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.Stage;
import com.streamsets.pipeline.stage.destination.sdcipc.Constants;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class IpcServer {
  private static final Logger LOG = LoggerFactory.getLogger(IpcServer.class);

  private final Stage.Context context;
  private final Configs configs;
  private Server httpServer;
  private final BlockingQueue<List<Record>> queue;
  private IpcServlet servlet;

  public IpcServer(Stage.Context context, Configs configs) {
    this.context = context;
    this.configs = configs;
    queue = new SynchronousQueue<>();
  }

  private int getJettyServerMinimumThreads() {
    // per Jetty hardcoded logic, them minimum number of threads we can have is determined by the following formula
    int cores = Runtime.getRuntime().availableProcessors();
    int acceptors = Math.max(1, Math.min(4,cores/8));
    int selectors = (cores + 1) / 2;
    return acceptors + selectors + 1;
  }

  public void start() throws Exception {
    int numberOfThreads = getJettyServerMinimumThreads();
    QueuedThreadPool threadPool = new QueuedThreadPool(numberOfThreads, numberOfThreads, 60000,
                                                       new ArrayBlockingQueue<Runnable>(20));
    threadPool.setName("sdcipc-server");
    threadPool.setDaemon(true);
    Server server = new Server(threadPool);

    ServerConnector connector;
    if (configs.sslEnabled) {
      LOG.debug("Configuring over HTTPS");
      HttpConfiguration httpsConf = new HttpConfiguration();
      httpsConf.addCustomizer(new SecureRequestCustomizer());
      SslContextFactory sslContextFactory = new SslContextFactory();
      sslContextFactory.setKeyStorePath(configs.getKeyStoreFile(context).getAbsolutePath());
      sslContextFactory.setKeyStorePassword(configs.keyStorePassword);
      sslContextFactory.setKeyManagerPassword(configs.keyStorePassword);
      connector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, "http/1.1"),
                                      new HttpConnectionFactory(httpsConf));
    } else {
      LOG.debug("Configuring over HTTP");
      connector = new ServerConnector(server);
    }
    connector.setPort(configs.port);
    server.setConnectors(new Connector[]{connector});

    servlet = new IpcServlet(context, configs, queue);
    ServletContextHandler context = new ServletContextHandler();
    context.addServlet(new ServletHolder(new PingServlet()), Constants.PING_PATH);
    context.addServlet(new ServletHolder(servlet), Constants.IPC_PATH);
    context.setContextPath("/");
    server.setHandler(context);
    server.start();

    LOG.info("Running, port '{}', SSL '{}'", configs.port, configs.sslEnabled);

    httpServer = server;
  }

  public void stop() {
    LOG.info("Shutting down, port '{}', SSL '{}'", configs.port, configs.sslEnabled);
    if (httpServer != null) {
      try {
        servlet.setShuttingDown();
        try {
          // wait up to 30secs for servlet ot finish POST request then continue with the shutdown
          long start = System.currentTimeMillis();
          while (servlet.isInPost() && System.currentTimeMillis() - start < 30000) {
            Thread.sleep(50);
          }
          if (servlet.isInPost()) {
            LOG.warn("Servlet not completing POST after 30secs, forcing a shutdown");
          }
        } catch (InterruptedException ex) {
          //NOP
        }
        httpServer.stop();
      } catch (Exception ex) {
        LOG.warn("Error while shutting down: {}", ex.toString(), ex);
      }
      httpServer = null;
    }
  }

  public List<Record> poll(long secs) throws InterruptedException {
    return queue.poll(secs, TimeUnit.SECONDS);
  }

  public void cancelBatch() {
    LOG.debug("Cancel batch");
    synchronized (queue) {
      queue.notify();
    }
  }

  public void doneWithBatch() {
    LOG.debug("Done with batch");
    servlet.batchDone();
    synchronized (queue) {
      queue.notify();
    }
  }

}
