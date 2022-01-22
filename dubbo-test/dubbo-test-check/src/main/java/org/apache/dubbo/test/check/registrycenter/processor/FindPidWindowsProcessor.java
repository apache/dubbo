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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.test.check.exception.DubboTestException;
import org.apache.dubbo.test.check.registrycenter.context.ZookeeperWindowsContext;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Create a {@link org.apache.dubbo.test.check.registrycenter.Processor} to find pid on Windows OS.
 */
public class FindPidWindowsProcessor extends ZookeeperWindowsProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FindPidWindowsProcessor.class);

    @Override
    protected void doProcess(ZookeeperWindowsContext context) throws DubboTestException {
        for (int clientPort : context.getClientPorts()) {
            this.findPid(context, clientPort);
        }
    }

    /**
     * Find the pid of zookeeper instance.
     *
     * @param context    the global context.
     * @param clientPort the client port of zookeeper instance.
     */
    private void findPid(ZookeeperWindowsContext context, int clientPort) {
        logger.info(String.format("Find the pid of the zookeeper with port %d", clientPort));
        Executor executor = new DefaultExecutor();
        executor.setExitValues(null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream ins = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(ins.toByteArray());
        executor.setStreamHandler(new PumpStreamHandler(out, null, in));
        CommandLine cmdLine = new CommandLine("cmd.exe");
        cmdLine.addArgument("/c");
        cmdLine.addArgument("netstat -ano | findstr " + clientPort);
        try {
            executor.execute(cmdLine);
            String result = out.toString();
            logger.info(String.format("Find result: %s", result));
            if (StringUtils.isNotEmpty(result)) {
                String[] values = result.split("\\r\\n");
                // values sample:
                // Protocol Local address          Foreign address        Status          PID
                //   TCP    127.0.0.1:2182         127.0.0.1:56672        ESTABLISHED     4020
                //   TCP    127.0.0.1:56672        127.0.0.1:2182         ESTABLISHED     1980
                //   TCP    127.0.0.1:56692        127.0.0.1:2182         ESTABLISHED     1980
                //   TCP    127.0.0.1:56723        127.0.0.1:2182         ESTABLISHED     1980
                //   TCP    [::]:2182              [::]:0                 LISTENING       4020
                if (values != null && values.length > 0) {
                    for (int i = 0; i < values.length; i++) {
                        List<String> segments = Arrays.stream(values[i].trim().split(" "))
                            .filter(str -> !"".equals(str))
                            .collect(Collectors.toList());
                        // segments sample:
                        // TCP
                        // 127.0.0.1:2182
                        // 127.0.0.1:56672
                        // ESTABLISHED
                        // 4020
                        if (segments != null && segments.size() == 5) {
                            if (this.check(segments.get(1), clientPort)) {
                                int pid = Integer.valueOf(segments.get(segments.size() - 1).trim());
                                context.register(clientPort, pid);
                                return;
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new DubboTestException(String.format("Failed to find the PID of zookeeper with port %d", clientPort), e);
        }
    }

    /**
     * Checks if segment is valid ip and port pair.
     *
     * @param segment    the segment to check
     * @param clientPort the client port of zookeeper instance
     * @return {@code true} if segment is valid pair of ip and port, otherwise {@code false}
     */
    private boolean check(String segment, int clientPort) {
        return ("[::]:" + clientPort).equalsIgnoreCase(segment)
            || ("0.0.0.0:" + clientPort).equalsIgnoreCase(segment)
            || ("127.0.0.1:" + clientPort).equalsIgnoreCase(segment);
    }
}
