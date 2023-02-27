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
package org.apache.dubbo.test.check.registrycenter.processor;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.test.check.exception.DubboTestException;
import org.apache.dubbo.test.check.registrycenter.Processor;
import org.apache.dubbo.test.check.registrycenter.context.ZookeeperWindowsContext;

/**
 * Create {@link Process} to stop zookeeper on Windows OS.
 */
public class StopZookeeperWindowsProcessor extends ZookeeperWindowsProcessor {

    private static final Logger logger = LoggerFactory.getLogger(StopZookeeperWindowsProcessor.class);

    /**
     * The {@link Processor} to find the pid of zookeeper instance.
     */
    private final Processor findPidProcessor = new FindPidWindowsProcessor();

    /**
     * The {@link Processor} to kill the pid of zookeeper instance.
     */
    private final Processor killPidProcessor = new KillProcessWindowsProcessor();

    @Override
    protected void doProcess(ZookeeperWindowsContext context) throws DubboTestException {
        logger.info("All of zookeeper instances are stopping...");
        // find pid and save into global context.
        this.findPidProcessor.process(context);
        // kill pid of zookeeper instance if exists
        this.killPidProcessor.process(context);
        // destroy all resources
        context.destroy();
    }
}
