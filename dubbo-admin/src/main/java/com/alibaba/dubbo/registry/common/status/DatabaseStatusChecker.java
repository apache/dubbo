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
package com.alibaba.dubbo.registry.common.status;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.status.Status;
import com.alibaba.dubbo.common.status.StatusChecker;

import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

/**
 * DatabaseStatus
 *
 */
public class DatabaseStatusChecker implements StatusChecker {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseStatusChecker.class);

    private int version;

    private String message;

    @Autowired
    private DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        check(); // init
    }

    public Status check() {
        boolean ok;
        try {
            Connection connection = dataSource.getConnection();
            try {
                DatabaseMetaData metaData = connection.getMetaData();
                ResultSet resultSet = metaData.getTypeInfo();
                try {
                    ok = resultSet.next();
                } finally {
                    resultSet.close();
                }
                if (message == null) {
                    message = metaData.getURL()
                            + " (" + metaData.getDatabaseProductName()
                            + " " + metaData.getDatabaseProductVersion()
                            + ", " + getIsolation(metaData.getDefaultTransactionIsolation()) + ")";
                }
                if (version == 0) {
                    version = metaData.getDatabaseMajorVersion();
                }
            } finally {
                connection.close();
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            ok = false;
        }
        return new Status(!ok ? Status.Level.ERROR : (version < 5 ? Status.Level.WARN : Status.Level.OK), message);
    }

    private String getIsolation(int i) {
        if (i == Connection.TRANSACTION_READ_COMMITTED) {
            return "READ_COMMITTED";
        }
        if (i == Connection.TRANSACTION_READ_UNCOMMITTED) {
            return "READ_UNCOMMITTED";
        }
        if (i == Connection.TRANSACTION_REPEATABLE_READ) {
            return "REPEATABLE_READ";
        }
        if (i == Connection.TRANSACTION_SERIALIZABLE) {
            return "SERIALIZABLE)";
        }
        return "NONE";
    }

}
