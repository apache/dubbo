/**
 * Project: dubbo.registry.server-1.1.0-SNAPSHOT
 * 
 * File Created at 2009-12-27
 * $Id: DatabaseStatusChecker.java 181192 2012-06-21 05:05:47Z tony.chenl $
 * 
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.registry.common.status;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.status.Status;
import com.alibaba.dubbo.common.status.StatusChecker;

/**
 * DatabaseStatus
 * 
 * @author william.liangf
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
        return new Status(! ok ? Status.Level.ERROR : (version < 5 ? Status.Level.WARN : Status.Level.OK), message);
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
