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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;

/**
 * Create {@link Process} to start zookeeper on Windows OS.
 */
public class StartZookeeperWindowsProcessor extends ZookeeperWindowsProcessor {

    private static final Logger logger = LoggerFactory.getLogger(StartZookeeperWindowsProcessor.class);

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
        // find pid and save into global context.
        this.findPidProcessor.process(context);
        // kill pid of zookeeper instance if exists
        this.killPidProcessor.process(context);
        for (int clientPort : context.getClientPorts()) {
            logger.info(String.format("The zookeeper-%d is starting...", clientPort));
            Path zookeeperBin = Paths.get(
                    context.getSourceFile().getParent().toString(),
                    String.valueOf(clientPort),
                    context.getUnpackedDirectory(),
                    "bin");
            Executor executor = new DefaultExecutor();
            executor.setExitValues(null);
            executor.setWatchdog(context.getWatchdog());
            CommandLine cmdLine = new CommandLine("cmd.exe");
            cmdLine.addArgument("/c");
            cmdLine.addArgument(Paths.get(zookeeperBin.toString(), "zkServer.cmd")
                    .toAbsolutePath()
                    .toString());
            context.getExecutorService().submit(() -> executor.execute(cmdLine));
        }
        logger.info("Waiting for all Zookeeper instances to be ready...");
        for (int clientPort : context.getClientPorts()) {
            waitForZookeeperReady(clientPort);
        }
    }

    private void waitForZookeeperReady(int port) throws DubboTestException {
        String connectString = "127.0.0.1:" + port;

        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(connectString)
                .retryPolicy(new RetryOneTime(1000))
                .connectionTimeoutMs(5000)
                .sessionTimeoutMs(5000)
                .build();

        try {
            logger.info("Waiting for Zookeeper to be ready at {}", connectString);

            client.start();
            boolean connected = client.blockUntilConnected(10, TimeUnit.SECONDS);

            if (!connected) {
                throw new DubboTestException("Zookeeper startup timeout at " + connectString);
            }

            logger.info("Zookeeper is ready at {}", connectString);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DubboTestException("Interrupted while waiting for Zookeeper startup at " + connectString, e);
        } finally {
            client.close();
        }
    }
}
