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
import org.apache.dubbo.test.check.registrycenter.context.ZookeeperContext;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Create {@link Process} to start zookeeper on Unix OS.
 */
public class StartZookeeperUnixProcessor extends ZookeeperUnixProcessor {

    private static final Logger logger = LoggerFactory.getLogger(StartZookeeperUnixProcessor.class);
    /**
     * The pattern for checking if zookeeper instances started.
     */
    private static final Pattern PATTERN_STARTED = Pattern.compile(".*STARTED.*");

    @Override
    protected Process doProcess(ZookeeperContext context, int clientPort) throws DubboTestException {
        logger.info(String.format("The zookeeper-%d is starting...", clientPort));
        List<String> commands = new ArrayList<>();
        Path zookeeperBin = Paths.get(context.getSourceFile().getParent().toString(),
                String.valueOf(clientPort),
                context.getUnpackedDirectory(),
                "bin");
        commands.add(Paths.get(zookeeperBin.toString(), "zkServer.sh")
                .toAbsolutePath().toString());
        commands.add("start");
        commands.add(Paths.get(zookeeperBin.getParent().toString(),
                "conf",
                "zoo.cfg").toAbsolutePath().toString());
        try {
            return new ProcessBuilder().directory(zookeeperBin.getParent().toFile())
                    .command(commands).inheritIO().redirectOutput(ProcessBuilder.Redirect.PIPE).start();
        } catch (IOException e) {
            throw new DubboTestException(String.format("Failed to start zookeeper-%d", clientPort), e);
        }
    }

    @Override
    protected Pattern getPattern() {
        return PATTERN_STARTED;
    }
}
