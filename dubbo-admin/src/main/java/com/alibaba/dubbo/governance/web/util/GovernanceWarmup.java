/**
 * Project: dubbo.registry.console-2.1.0-SNAPSHOT
 * <p>
 * File Created at Nov 1, 2011
 * $Id: GovernanceWarmup.java 182013 2012-06-26 10:32:43Z tony.chenl $
 * <p>
 * Copyright 1999-2100 Alibaba.com Corporation Limited.
 * All rights reserved.
 * <p>
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.governance.web.util;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.status.StatusChecker;
import com.alibaba.dubbo.registry.common.StatusManager;

import org.springframework.beans.factory.InitializingBean;

/**
 * @author ding.lid
 */
public class GovernanceWarmup implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(GovernanceWarmup.class);

    private StatusChecker memoryStatusChecker;

    private StatusChecker threadPoolStatusChecker;

    private StatusChecker cacheStatusChecker;

    private StatusChecker databaseStatusChecker;

    private StatusChecker failureStatusChecker;

    private StatusChecker loadStatusChecker;

    private StatusChecker SocketStatusChecker;

    private StatusChecker timerStatusChecker;

    private StatusChecker warmupStatusChecker;

    public void afterPropertiesSet() throws Exception {
        logger.info("Registry Console warn up.");

        StatusManager statusManager = StatusManager.getInstance();

        statusManager.addStatusHandler("memory", memoryStatusChecker);
        statusManager.addStatusHandler("load", loadStatusChecker);
//        statusManager.addStatusHandler("database",databaseStatusChecker);
//        statusManager.addStatusHandler("cache",cacheStatusChecker);
//        statusManager.addStatusHandler("threadpool",threadPoolStatusChecker);
//        statusManager.addStatusHandler("failure",failureStatusChecker);
//        statusManager.addStatusHandler("socket",SocketStatusChecker);
//        statusManager.addStatusHandler("threadpool",threadPoolStatusChecker);
//        statusManager.addStatusHandler("timer",timerStatusChecker);
//        statusManager.addStatusHandler("warmup",warmupStatusChecker);
    }

    public void setMemoryStatusChecker(StatusChecker memoryStatusChecker) {
        this.memoryStatusChecker = memoryStatusChecker;
    }


    public void setThreadPoolStatusChecker(StatusChecker threadPoolStatusChecker) {
        this.threadPoolStatusChecker = threadPoolStatusChecker;
    }


    public void setCacheStatusChecker(StatusChecker cacheStatusChecker) {
        this.cacheStatusChecker = cacheStatusChecker;
    }


    public void setDatabaseStatusChecker(StatusChecker databaseStatusChecker) {
        this.databaseStatusChecker = databaseStatusChecker;
    }


    public void setFailureStatusChecker(StatusChecker failureStatusChecker) {
        this.failureStatusChecker = failureStatusChecker;
    }


    public void setLoadStatusChecker(StatusChecker loadStatusChecker) {
        this.loadStatusChecker = loadStatusChecker;
    }


    public void setSocketStatusChecker(StatusChecker socketStatusChecker) {
        SocketStatusChecker = socketStatusChecker;
    }


    public void setTimerStatusChecker(StatusChecker timerStatusChecker) {
        this.timerStatusChecker = timerStatusChecker;
    }

    public void setWarmupStatusChecker(StatusChecker warmupStatusChecker) {
        this.warmupStatusChecker = warmupStatusChecker;
    }

}
