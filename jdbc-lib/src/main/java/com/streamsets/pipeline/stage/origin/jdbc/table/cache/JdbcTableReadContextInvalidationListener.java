/**
 * Copyright 2017 StreamSets Inc.
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
package com.streamsets.pipeline.stage.origin.jdbc.table.cache;

import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.streamsets.pipeline.stage.origin.jdbc.table.TableContext;
import com.streamsets.pipeline.stage.origin.jdbc.table.TableContextUtil;
import com.streamsets.pipeline.stage.origin.jdbc.table.TableReadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens for cache invalidation and appropriately closes the result set and statement.
 */
public class JdbcTableReadContextInvalidationListener implements RemovalListener<TableContext, TableReadContext> {

  private static final Logger LOGGER = LoggerFactory.getLogger(JdbcTableReadContextInvalidationListener.class);

  @Override
  public void onRemoval(RemovalNotification<TableContext, TableReadContext> tableReadContextRemovalNotification) {
    TableContext tableContext = tableReadContextRemovalNotification.getKey();
    LOGGER.info(
        "Closing result set for : {}",
        TableContextUtil.getQualifiedTableName(
            tableContext.getSchema(),
            tableContext.getTableName()
        )
    );
    TableReadContext readContext = tableReadContextRemovalNotification.getValue();
    //Destroy and close statement/result set.
    readContext.destroy();
  }
}
