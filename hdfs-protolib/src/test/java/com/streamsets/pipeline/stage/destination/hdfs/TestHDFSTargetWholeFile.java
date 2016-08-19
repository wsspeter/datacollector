/**
 * Copyright 2016 StreamSets Inc.
 *
 * Licensed under the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.pipeline.stage.destination.hdfs;

import com.streamsets.pipeline.api.Field;
import com.streamsets.pipeline.api.FileRef;
import com.streamsets.pipeline.api.OnRecordError;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.config.DataFormat;
import com.streamsets.pipeline.config.WholeFileExistsAction;
import com.streamsets.pipeline.lib.io.fileref.FileRefUtil;
import com.streamsets.pipeline.lib.io.fileref.LocalFileRef;
import com.streamsets.pipeline.sdk.RecordCreator;
import com.streamsets.pipeline.sdk.TargetRunner;
import com.streamsets.pipeline.stage.destination.hdfs.util.HdfsTargetUtil;
import com.streamsets.pipeline.stage.destination.hdfs.writer.RecordWriter;
import com.streamsets.pipeline.stage.destination.hdfs.writer.RecordWriterManager;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RecordWriterManager.class})
@PowerMockIgnore({"javax.*", "org.*"})
public class TestHDFSTargetWholeFile {
  private String testDir;
  private URI uri;

  @Before
  public void before() {
    File dir = new File("target", UUID.randomUUID().toString()).getAbsoluteFile();
    Assert.assertTrue(dir.mkdirs());
    testDir = dir.getAbsolutePath();
    try {
      uri = new URI("file:///");
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    System.setProperty("HADOOP_USER_NAME", System.getProperty("user.name"));
  }

  private String getTestDir() {
    return testDir;
  }

  @Test(expected = StageException.class)
  public void testInvalidMaxRecordsPerFileOnWholeFileFormat() throws Exception {
    HdfsTarget hdfsTarget = HdfsTargetUtil.newBuilder()
        .dirPathTemplate(getTestDir() + "/hdfs/${record:attribute('key')}/a/b/c}")
        .timeDriver("${time:now()}")
        .dataForamt(DataFormat.WHOLE_FILE)
        .fileNameEL("${record:value('/fileInfo/fileName')}")
        .maxFileSize(2)
        .maxRecordsPerFile(1)
        .fileType(HdfsFileType.WHOLE_FILE)
        .idleTimeout("-1")
        .lateRecordsAction(LateRecordsAction.SEND_TO_LATE_RECORDS_FILE)
        .build();

    TargetRunner runner = new TargetRunner.Builder(HdfsDTarget.class, hdfsTarget)
        .setOnRecordError(OnRecordError.STOP_PIPELINE)
        .build();
    runner.runInit();
  }

  @Test(expected = StageException.class)
  public void testInvalidMaxFileSizeOnWholeFileFormat() throws Exception {
    HdfsTarget hdfsTarget = HdfsTargetUtil.newBuilder()
        .dirPathTemplate(getTestDir() + "/hdfs/${record:attribute('key')}/a/b/c}")
        .timeDriver("${time:now()}")
        .dataForamt(DataFormat.WHOLE_FILE)
        .fileNameEL("${record:value('/fileInfo/fileName')}")
        .maxRecordsPerFile(2)
        .maxFileSize(0)
        .fileType(HdfsFileType.WHOLE_FILE)
        .idleTimeout("-1")
        .lateRecordsAction(LateRecordsAction.SEND_TO_LATE_RECORDS_FILE)
        .hdfsUri(uri.toString())
        .build();

    TargetRunner runner = new TargetRunner.Builder(HdfsDTarget.class, hdfsTarget)
        .setOnRecordError(OnRecordError.STOP_PIPELINE)
        .build();

    runner.runInit();
  }

  @Test(expected = StageException.class)
  public void testInvalidFileTypeOnWholeFileFormat() throws Exception {
    HdfsTarget hdfsTarget = HdfsTargetUtil.newBuilder()
        .dirPathTemplate(getTestDir() + "/hdfs/${record:attribute('key')}/a/b/c}")
        .timeDriver("${time:now()}")
        .dataForamt(DataFormat.WHOLE_FILE)
        .fileNameEL("${record:value('/fileInfo/fileName')}")
        .maxFileSize(0)
        .maxRecordsPerFile(1)
        .idleTimeout("-1")
        .fileType(HdfsFileType.TEXT)
        .hdfsUri(uri.toString())
        .lateRecordsAction(LateRecordsAction.SEND_TO_LATE_RECORDS_FILE)
        .build();

    TargetRunner runner = new TargetRunner.Builder(HdfsDTarget.class, hdfsTarget)
        .setOnRecordError(OnRecordError.STOP_PIPELINE)
        .build();
    runner.runInit();
  }

  @Test(expected = StageException.class)
  public void testInvalidIdleTimeoutOnWholeFileFormat() throws Exception {
    HdfsTarget hdfsTarget = HdfsTargetUtil.newBuilder()
        .dirPathTemplate(getTestDir() + "/hdfs/${record:attribute('key')}/a/b/c}")
        .timeDriver("${time:now()}")
        .dataForamt(DataFormat.WHOLE_FILE)
        .fileNameEL("${record:value('/fileInfo/fileName')}")
        .maxFileSize(0)
        .maxRecordsPerFile(1)
        .fileType(HdfsFileType.TEXT)
        .hdfsUri(uri.toString())
        .idleTimeout("{1 * HOURS}")
        .lateRecordsAction(LateRecordsAction.SEND_TO_LATE_RECORDS_FILE)
        .build();

    TargetRunner runner = new TargetRunner.Builder(HdfsDTarget.class, hdfsTarget)
        .setOnRecordError(OnRecordError.STOP_PIPELINE)
        .build();
    runner.runInit();
  }

  @Test
  public void testWholeFileCopyMultipleFiles() throws Exception {
    java.nio.file.Path filePath1 = Paths.get(getTestDir() + "/source_testWholeFileCopyMultipleFiles1.txt");
    java.nio.file.Path filePath2 = Paths.get(getTestDir() + "/source_testWholeFileCopyMultipleFiles2.txt");

    Files.write(filePath1, "This is a sample file 1 with some text".getBytes());
    Files.write(filePath2, "This is a sample file 2 with some text".getBytes());

    HdfsTarget hdfsTarget = HdfsTargetUtil.newBuilder()
        .hdfsUri(uri.toString())
        .dirPathTemplate(getTestDir())
        .timeDriver("${time:now()}")
        .dataForamt(DataFormat.WHOLE_FILE)
        .fileType(HdfsFileType.WHOLE_FILE)
        .fileNameEL("${record:value('/fileInfo/filename')}")
        .maxRecordsPerFile(1)
        .maxFileSize(0)
        .uniquePrefix("sdc-")
        .idleTimeout("-1")
        .lateRecordsAction(LateRecordsAction.SEND_TO_LATE_RECORDS_FILE)
        .build();

    TargetRunner runner = new TargetRunner.Builder(HdfsDTarget.class, hdfsTarget)
        .setOnRecordError(OnRecordError.STOP_PIPELINE)
        .build();

    runner.runInit();
    runner.runWrite(Arrays.asList(getFileRefRecordForFile(filePath1), getFileRefRecordForFile(filePath2)));

    for (Path filePath : Arrays.asList(filePath1, filePath2)) {
      String targetFileName = getTestDir() + "/" + "sdc-" + filePath.getFileName();

      //Now check the file is copied as a whole.
      Assert.assertTrue(Files.exists(Paths.get(targetFileName)));

      try (InputStream is1 = new FileInputStream(filePath.toString());
           InputStream is2 = new FileInputStream(targetFileName)) {
        checkFileContent(is1, is2);
      }
    }
    runner.runDestroy();
  }

  @Test
  public void testWholeFilePartialCopy() throws Exception {
    PowerMockito.replace(
        MemberMatcher.method(RecordWriterManager.class, "commitWriter", RecordWriter.class)
    ).with(new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //Close the recordWriter so contents are written.
        RecordWriter recordWriter = (RecordWriter)args[0];
        recordWriter.close();
        //Make it no op so that the _tmp_ file is left behind
        return null;
      }
    });

    java.nio.file.Path filePath = Paths.get(getTestDir() + "/source_testWholeFilePartialCopy.txt");
    Files.write(filePath, "This is a sample file with some text".getBytes());

    HdfsTarget hdfsTarget = HdfsTargetUtil.newBuilder()
        .hdfsUri(uri.toString())
        .dirPathTemplate(getTestDir())
        .timeDriver("${time:now()}")
        .dataForamt(DataFormat.WHOLE_FILE)
        .fileType(HdfsFileType.WHOLE_FILE)
        .fileNameEL("${record:value('/fileInfo/filename')}")
        .maxRecordsPerFile(1)
        .maxFileSize(0)
        .uniquePrefix("sdc-")
        .idleTimeout("-1")
        .lateRecordsAction(LateRecordsAction.SEND_TO_LATE_RECORDS_FILE)
        .build();

    TargetRunner runner = new TargetRunner.Builder(HdfsDTarget.class, hdfsTarget)
        .setOnRecordError(OnRecordError.STOP_PIPELINE)
        .build();

    Record record = getFileRefRecordForFile(filePath);

    runner.runInit();
    runner.runWrite(Collections.singletonList(record));
    runner.runDestroy();

    runner = new TargetRunner.Builder(HdfsDTarget.class, hdfsTarget)
        .setOnRecordError(OnRecordError.STOP_PIPELINE)
        .build();

    final String tmpTargetFileName = getTestDir() + "/" + "_tmp_sdc-" + filePath.getFileName();
    final String targetFileName = getTestDir() + "/" + "sdc-" + filePath.getFileName();

    //Make sure _tmp_ file is there
    Assert.assertTrue(Files.exists(Paths.get(tmpTargetFileName)));
    Assert.assertTrue(Files.size(Paths.get(tmpTargetFileName)) > 0);

    //Make sure the file is not renamed to the final file.
    Assert.assertFalse(Files.exists(Paths.get(targetFileName)));

    PowerMockito.replace(
        MemberMatcher.method(RecordWriterManager.class, "commitWriter", RecordWriter.class)
    ).with(new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //Set the commit Writer back.
        return method.invoke(proxy, args);
      }
    });

    runner.runInit();
    runner.runWrite(Collections.singletonList(record));

    //Make sure _tmp_ file is not there
    Assert.assertFalse(Files.exists(Paths.get(tmpTargetFileName)));

    //Make sure the file is renamed to the final file.
    Assert.assertTrue(Files.exists(Paths.get(targetFileName)));
    Assert.assertTrue(Files.size(Paths.get(targetFileName)) > 0);
    checkFileContent(new FileInputStream(filePath.toString()), new FileInputStream(targetFileName));
    runner.runDestroy();
  }

  @Test
  public void testWholeFilePermission() throws Exception {

    java.nio.file.Path filePath1 = Paths.get(getTestDir() + "/source_testWholeFilePermissionFiles1.txt");
    java.nio.file.Path filePath2 = Paths.get(getTestDir() + "/source_testWholeFilePermissionFiles2.txt");
    java.nio.file.Path filePath3 = Paths.get(getTestDir() + "/source_testWholeFilePermissionFiles3.txt");



    Files.write(filePath1, "This is a sample file 1 with some text".getBytes());
    Files.write(filePath2, "This is a sample file 2 with some text".getBytes());
    Files.write(filePath3, "This is a sample file 3 with some text".getBytes());


    HdfsTarget hdfsTarget = HdfsTargetUtil.newBuilder()
        .hdfsUri(uri.toString())
        .dirPathTemplate(getTestDir())
        .timeDriver("${time:now()}")
        .dataForamt(DataFormat.WHOLE_FILE)
        .fileType(HdfsFileType.WHOLE_FILE)
        .fileNameEL("${record:value('/fileInfo/filename')}")
        .maxRecordsPerFile(1)
        .maxFileSize(0)
        .uniquePrefix("sdc-")
        .idleTimeout("-1")
        .permissionEL("${record:value('/fileInfo/permissions')}")
        .lateRecordsAction(LateRecordsAction.SEND_TO_LATE_RECORDS_FILE)
        .build();

    TargetRunner runner = new TargetRunner.Builder(HdfsDTarget.class, hdfsTarget)
        .setOnRecordError(OnRecordError.STOP_PIPELINE)
        .build();

    runner.runInit();

    try {
      runner.runWrite(
          Arrays.asList(
              getFileRefRecordForFile(filePath1, "755"),
              //posix style
              getFileRefRecordForFile(filePath2, "rwxr--r--"),
              //unix style
              getFileRefRecordForFile(filePath3, "-rw-rw----")
          )
      );

      org.apache.hadoop.fs.Path targetPath1 = new org.apache.hadoop.fs.Path(getTestDir() + "/sdc-" + filePath1.getFileName());
      org.apache.hadoop.fs.Path targetPath2 = new org.apache.hadoop.fs.Path(getTestDir() + "/sdc-" + filePath2.getFileName());
      org.apache.hadoop.fs.Path targetPath3 = new org.apache.hadoop.fs.Path(getTestDir() + "/sdc-" + filePath3.getFileName());



      FileSystem fs = FileSystem.get(uri, new HdfsConfiguration());

      Assert.assertTrue(fs.exists(targetPath1));
      Assert.assertTrue(fs.exists(targetPath2));
      Assert.assertTrue(fs.exists(targetPath3));



      FsPermission actual1 = fs.listStatus(targetPath1)[0].getPermission();
      FsPermission actual2 = fs.listStatus(targetPath2)[0].getPermission();
      FsPermission actual3 = fs.listStatus(targetPath3)[0].getPermission();


      FsPermission expected1 = new FsPermission("755");
      FsPermission expected2 = FsPermission.valueOf("-rwxr--r--");
      FsPermission expected3 = FsPermission.valueOf("-rw-rw----");


      Assert.assertEquals(expected1, actual1);
      Assert.assertEquals(expected2, actual2);
      Assert.assertEquals(expected3, actual3);

    } finally {
      runner.runDestroy();
    }
  }

  @Test
  public void testWholeFileAlreadyExistsToError() throws Exception {
    java.nio.file.Path filePath = Paths.get(getTestDir() + "/source_testWholeFileAlreadyExistsToError.txt");
    Files.write(filePath, "This is a sample file 1 with some text".getBytes());

    HdfsTarget hdfsTarget = HdfsTargetUtil.newBuilder()
        .hdfsUri(uri.toString())
        .dirPathTemplate(getTestDir())
        .timeDriver("${time:now()}")
        .dataForamt(DataFormat.WHOLE_FILE)
        .fileType(HdfsFileType.WHOLE_FILE)
        .fileNameEL("${record:value('"+ FileRefUtil.FILE_INFO_FIELD_PATH +"/filename')}")
        .maxRecordsPerFile(1)
        .maxFileSize(0)
        .uniquePrefix("sdc-")
        .idleTimeout("-1")
        .wholeFileExistsAction(WholeFileExistsAction.TO_ERROR)
        .lateRecordsAction(LateRecordsAction.SEND_TO_LATE_RECORDS_FILE)
        .build();

    TargetRunner runner = new TargetRunner.Builder(HdfsDTarget.class, hdfsTarget)
        .setOnRecordError(OnRecordError.TO_ERROR)
        .build();

    runner.runInit();

    try {
      runner.runWrite(Collections.singletonList(getFileRefRecordForFile(filePath)));
      Assert.assertEquals(0, runner.getErrorRecords().size());

      //Write the same file already exists
      runner.runWrite(Collections.singletonList(getFileRefRecordForFile(filePath)));
      Assert.assertEquals(1, runner.getErrorRecords().size());

      Record record = runner.getErrorRecords().get(0);
      Assert.assertEquals(record.getHeader().getErrorCode(), Errors.HADOOPFS_54.getCode());
    } finally {
      runner.runDestroy();
    }
  }

  @Test
  public void testWholeFileAlreadyExistsOverwrite() throws Exception {
    java.nio.file.Path filePath = Paths.get(getTestDir() + "/source_testWholeFileAlreadyExistsOverwrite.txt");
    Files.write(filePath, "This is a sample file 1 with some text".getBytes());

    HdfsTarget hdfsTarget = HdfsTargetUtil.newBuilder()
        .hdfsUri(uri.toString())
        .dirPathTemplate(getTestDir())
        .timeDriver("${time:now()}")
        .dataForamt(DataFormat.WHOLE_FILE)
        .fileType(HdfsFileType.WHOLE_FILE)
        .fileNameEL("${record:value('"+ FileRefUtil.FILE_INFO_FIELD_PATH +"/filename')}")
        .maxRecordsPerFile(1)
        .maxFileSize(0)
        .uniquePrefix("sdc-")
        .idleTimeout("-1")
        .wholeFileExistsAction(WholeFileExistsAction.OVERWRITE)
        .lateRecordsAction(LateRecordsAction.SEND_TO_LATE_RECORDS_FILE)
        .build();

    TargetRunner runner = new TargetRunner.Builder(HdfsDTarget.class, hdfsTarget)
        .setOnRecordError(OnRecordError.TO_ERROR)
        .build();

    runner.runInit();

    try {
      runner.runWrite(Collections.singletonList(getFileRefRecordForFile(filePath)));
      Assert.assertEquals(0, runner.getErrorRecords().size());

      //Write the same file no error, overwritten
      runner.runWrite(Collections.singletonList(getFileRefRecordForFile(filePath)));
      Assert.assertEquals(0, runner.getErrorRecords().size());
    } finally {
      runner.runDestroy();
    }
  }

  private Record getFileRefRecordForFile(Path filePath, String octalPermission) throws Exception {
    Record fileRefRecord = getFileRefRecordForFile(filePath);
    fileRefRecord.set("/fileInfo/permissions", Field.create(octalPermission));
    return fileRefRecord;
  }

  private Record getFileRefRecordForFile(Path filePath) throws Exception {
    Record fileRefRecord = RecordCreator.create();
    FileRef fileRef =
        new LocalFileRef.Builder()
            .bufferSize(1024)
            .createMetrics(false)
            .verifyChecksum(false)
            .filePath(filePath.toAbsolutePath().toString())
            .build();
    Map<String, Field> fieldMap = new HashMap<>();

    Map<String, Object> metadata = new HashMap<>(Files.readAttributes(filePath, "posix:*"));
    metadata.put("filename", filePath.getFileName());
    metadata.put("file", filePath.toString());
    metadata.put("dir", filePath.getParent().toString());
    metadata.put("permissions", "777");

    fieldMap.put(FileRefUtil.FILE_REF_FIELD_NAME, Field.create(Field.Type.FILE_REF, fileRef));
    fieldMap.put(FileRefUtil.FILE_INFO_FIELD_NAME, FileRefUtil.createFieldForMetadata(metadata));
    fileRefRecord.set(Field.create(fieldMap));

    return fileRefRecord;
  }

  private void checkFileContent(InputStream is1, InputStream is2) throws Exception {
    int totalBytesRead1 = 0, totalBytesRead2 = 0;
    int a = 0, b = 0;
    while (a != -1 || b != -1) {
      totalBytesRead1 = ((a = is1.read()) != -1)? totalBytesRead1 + 1 : totalBytesRead1;
      totalBytesRead2 = ((b = is2.read()) != -1)? totalBytesRead2 + 1 : totalBytesRead2;
      Assert.assertEquals(a, b);
    }
    Assert.assertEquals(totalBytesRead1, totalBytesRead2);
  }

}