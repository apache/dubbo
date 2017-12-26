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
package com.alibaba.dubbo.governance.web.util;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.status.StatusChecker;
import com.alibaba.dubbo.registry.common.StatusManager;

import org.springframework.beans.factory.InitializingBean;

/**
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
