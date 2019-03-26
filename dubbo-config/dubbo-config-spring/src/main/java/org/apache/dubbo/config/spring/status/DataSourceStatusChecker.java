/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.config.spring.status;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.status.Status;
import org.apache.dubbo.common.status.StatusChecker;
import org.apache.dubbo.config.spring.extension.SpringExtensionFactory;

import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Map;

/**
 * DataSourceStatusChecker
 */
@Activate
public class DataSourceStatusChecker implements StatusChecker {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceStatusChecker.class);

    @Override
    @SuppressWarnings("unchecked")
    public Status check() {
        ApplicationContext context = null;
        for (ApplicationContext c : SpringExtensionFactory.getContexts()) {
            if (c != null) {
                context = c;
                break;
            }
        }

        if (context == null) {
            return new Status(Status.Level.UNKNOWN);
        }

        Map<String, DataSource> dataSources = context.getBeansOfType(DataSource.class, false, false);
        if (dataSources == null || dataSources.size() == 0) {
            return new Status(Status.Level.UNKNOWN);
        }
        Status.Level level = Status.Level.OK;
        StringBuilder buf = new StringBuilder();
        for (Map.Entry<String, DataSource> entry : dataSources.entrySet()) {
            DataSource dataSource = entry.getValue();
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append(entry.getKey());
            try {
                Connection connection = dataSource.getConnection();
                try {
                    DatabaseMetaData metaData = connection.getMetaData();
                    ResultSet resultSet = metaData.getTypeInfo();
                    try {
                        if (!resultSet.next()) {
                            level = Status.Level.ERROR;
                        }
                    } finally {
                        resultSet.close();
                    }
                    buf.append(metaData.getURL());
                    buf.append("(");
                    buf.append(metaData.getDatabaseProductName());
                    buf.append("-");
                    buf.append(metaData.getDatabaseProductVersion());
                    buf.append(")");
                } finally {
                    connection.close();
                }
            } catch (Throwable e) {
                logger.warn(e.getMessage(), e);
                return new Status(level, e.getMessage());
            }
        }
        return new Status(level, buf.toString());
    }

}
