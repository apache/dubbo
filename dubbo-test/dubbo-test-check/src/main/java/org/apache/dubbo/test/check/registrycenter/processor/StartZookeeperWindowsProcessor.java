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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Create {@link Process} to start zookeeper on Windows OS.
 */
public class StartZookeeperWindowsProcessor extends ZookeeperWindowsProcessor {

    private static final Logger logger = LoggerFactory.getLogger(StartZookeeperWindowsProcessor.class);

    @Override
    protected void doProcess(ZookeeperWindowsContext context) throws DubboTestException {
        for (int clientPort : context.getClientPorts()) {
            logger.info(String.format("The zookeeper-%d is starting...", clientPort));
            List<String> commands = new ArrayList<>();
            Path zookeeperBin = Paths.get(context.getSourceFile().getParent().toString(),
                String.valueOf(clientPort),
                String.format("apache-zookeeper-%s-bin", context.getVersion()),
                "bin");
            commands.add("cmd.exe /c");
            commands.add(Paths.get(zookeeperBin.toString(), "zkServer.cmd")
                .toAbsolutePath().toString());
            ProcessBuilder processBuilder = new ProcessBuilder().directory(zookeeperBin.getParent().toFile())
                .command(commands).inheritIO().redirectOutput(ProcessBuilder.Redirect.PIPE);
            context.getExecutorService().submit(new RunProcess(context, clientPort, processBuilder));
            // wait until zookeeper started successfully
            // we also can check the output log to verify if the zookeeper is ready,
            // however, windows OS is very complicated.
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                //ignored
            }
        }
    }

    /**
     * Define a process to handle the start command.
     */
    public static class RunProcess implements Runnable {

        public RunProcess(ZookeeperWindowsContext context, int clientPort, ProcessBuilder processBuilder) {
            this.context = context;
            this.clientPort = clientPort;
            this.processBuilder = processBuilder;
        }

        private ZookeeperWindowsContext context;
        private int clientPort;
        private ProcessBuilder processBuilder;

        @Override
        public void run() {
            try {
                Process process = processBuilder.start();
                context.register(clientPort, process);
            } catch (IOException e) {
                logger.error(String.format("Failed to run process with the client port %d", this.clientPort), e);
            }
        }
    }
}
