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
import org.apache.dubbo.test.check.registrycenter.context.ZookeeperWindowsContext;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.IOException;

/**
 * Create a {@link org.apache.dubbo.test.check.registrycenter.Processor} to kill pid on Windows OS.
 */
public class KillProcessWindowsProcessor extends ZookeeperWindowsProcessor {

    private static final Logger logger = LoggerFactory.getLogger(KillProcessWindowsProcessor.class);

    @Override
    protected void doProcess(ZookeeperWindowsContext context) throws DubboTestException {
        for (int clientPort : context.getClientPorts()) {
            Integer pid = context.getPid(clientPort);
            if (pid == null) {
                logger.info("There is no PID of zookeeper instance with the port " + clientPort);
                continue;
            }
            logger.info(String.format("Kill the pid %d of the zookeeper with port %d", pid, clientPort));
            Executor executor = new DefaultExecutor();
            executor.setExitValues(null);
            executor.setStreamHandler(new PumpStreamHandler(null, null, null));
            CommandLine cmdLine = new CommandLine("cmd.exe");
            cmdLine.addArgument("/c");
            cmdLine.addArgument("taskkill /PID " + pid + " -t -f");
            try {
                executor.execute(cmdLine);
                // clear pid
                context.removePid(clientPort);
            } catch (IOException e) {
                throw new DubboTestException(String.format("Failed to kill the pid %d of zookeeper with port %d", pid, clientPort), e);
            }
        }
    }
}
