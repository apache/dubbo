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

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;

/**
 * Create {@link Process} to start zookeeper on Windows OS.
 */
public class StartZookeeperWindowsProcessor extends ZookeeperWindowsProcessor {

    private static final Logger logger = LoggerFactory.getLogger(StartZookeeperWindowsProcessor.class);

    /**
     * Maximum time to wait for each zookeeper instance to accept connections.
     */
    private static final long READY_TIMEOUT_MILLIS = 30_000;

    /**
     * Delay between successive readiness checks.
     */
    private static final long POLL_INTERVAL_MILLIS = 200;

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
        // Actively wait for each zookeeper instance to start accepting connections,
        // instead of blindly sleeping for a fixed duration. This fails fast when a
        // port never comes up, and doesn't waste time once a port is already ready.
        waitForZookeeperReady(context.getClientPorts());
    }

    /**
     * Blocks until every given zookeeper client port is accepting connections, or throws
     * if any of them fails to become ready within {@link #READY_TIMEOUT_MILLIS}.
     */
    private void waitForZookeeperReady(int[] clientPorts) throws DubboTestException {
        for (int clientPort : clientPorts) {
            long deadline = System.currentTimeMillis() + READY_TIMEOUT_MILLIS;
            boolean ready = false;

            while (System.currentTimeMillis() < deadline) {
                if (isPortOpen(clientPort)) {
                    ready = true;
                    break;
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(POLL_INTERVAL_MILLIS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new DubboTestException("Interrupted while waiting for zookeeper to start", e);
                }
            }

            if (!ready) {
                throw new DubboTestException(String.format(
                        "Zookeeper on port %d did not become ready within %d milliseconds",
                        clientPort, READY_TIMEOUT_MILLIS));
            }
            logger.info(String.format("The zookeeper-%d is ready.", clientPort));
        }
    }

    /**
     * Returns true if a TCP connection to 127.0.0.1:port can be opened, meaning
     * something (expected to be zookeeper) is already listening there.
     */
    private boolean isPortOpen(int port) {
        try (Socket socket = new Socket("127.0.0.1", port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
