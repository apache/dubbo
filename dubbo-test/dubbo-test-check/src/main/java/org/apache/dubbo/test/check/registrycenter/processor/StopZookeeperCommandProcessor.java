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

import org.apache.dubbo.test.check.exception.DubboTestException;
import org.apache.dubbo.test.check.registrycenter.initializer.ZookeeperInitializer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Create {@link Process} to stop zookeeper.
 */
public class StopZookeeperCommandProcessor extends ZookeeperCommandProcessor{

    /**
     * The pattern for checking zookeeper is started or not.
     */
    private static final Pattern SERVER_READY_PATTERN = Pattern.compile(".*STOPPED.*");

    @Override
    protected Process start(ZookeeperInitializer.ZookeeperContext context, int clientPort) {
        List<String> commands = new ArrayList<>();
        Path zookeeperBin = Paths.get(context.getSourceFile().getParent().toString(),
            String.valueOf(clientPort),
            String.format("apache-zookeeper-%s-bin", context.getVersion()),
            "bin");
        /*if (this.getOSName().contains("windows")) {
            commands.add(Paths.get(zookeeperBin.toString(), "zkServer.cmd")
                .toAbsolutePath().toString());
        } else {
            commands.add(Paths.get(zookeeperBin.toString(), "zkServer.sh")
                .toAbsolutePath().toString());
        }*/
        commands.add(Paths.get(zookeeperBin.toString(), "zkServer.sh")
            .toAbsolutePath().toString());
        commands.add("stop");
        try {
            return new ProcessBuilder().directory(zookeeperBin.getParent().toFile())
                .command(commands).inheritIO().redirectOutput(ProcessBuilder.Redirect.PIPE).start();
        } catch (IOException e) {
            throw new DubboTestException(String.format("Failed to stop zookeeper server, client port:%d", clientPort), e);
        }
    }

    @Override
    protected boolean check(String message) {
        return SERVER_READY_PATTERN.matcher(message).matches();
    }
}
