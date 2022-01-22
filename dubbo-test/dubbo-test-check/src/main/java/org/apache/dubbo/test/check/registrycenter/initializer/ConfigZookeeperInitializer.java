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
package org.apache.dubbo.test.check.registrycenter.initializer;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.test.check.exception.DubboTestException;
import org.apache.dubbo.test.check.registrycenter.context.ZookeeperContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Update the config file of zookeeper.
 */
public class ConfigZookeeperInitializer extends ZookeeperInitializer {

    private static final Logger logger = LoggerFactory.getLogger(ConfigZookeeperInitializer.class);

    /**
     * Update the config file with the given client port and admin server port.
     *
     * @param clientPort      the client port
     * @param adminServerPort the admin server port
     * @throws DubboTestException when an exception occurred
     */
    private void updateConfig(ZookeeperContext context, int clientPort, int adminServerPort) throws DubboTestException {
        Path zookeeperConf = Paths.get(context.getSourceFile().getParent().toString(),
                String.valueOf(clientPort),
                context.getUnpackedDirectory(),
                "conf");
        File zooSample = Paths.get(zookeeperConf.toString(), "zoo_sample.cfg").toFile();
        int availableAdminServerPort = NetUtils.getAvailablePort(adminServerPort);
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(zooSample));
            properties.setProperty("clientPort", String.valueOf(clientPort));
            properties.setProperty("admin.serverPort", String.valueOf(availableAdminServerPort));
            Path dataDir = Paths.get(zookeeperConf.getParent().toString(), "data");
            if (!Files.exists(dataDir)) {
                try {
                    logger.info("It is creating the data directory...");
                    Files.createDirectories(dataDir);
                } catch (IOException e) {
                    throw new RuntimeException(String.format("Failed to create the data directory to save zookeeper binary file, file path:%s", context.getSourceFile()), e);
                }
            }
            properties.setProperty("dataDir", dataDir.toString());
            FileOutputStream oFile = null;
            try {
                oFile = new FileOutputStream(Paths.get(zookeeperConf.toString(), "zoo.cfg").toFile());
                properties.store(oFile, "");
            } finally {
                try {
                    oFile.close();
                } catch (IOException e) {
                    throw new DubboTestException("Failed to close file", e);
                }
            }
            logger.info("The configuration information of zoo.cfg are as below,\n" +
                    "which located in " + zooSample.getAbsolutePath() + "\n" +
                    propertiesToString(properties));
        } catch (IOException e) {
            throw new DubboTestException(String.format("Failed to update %s file", zooSample), e);
        }

        File log4j = Paths.get(zookeeperConf.toString(), "log4j.properties").toFile();
        try {
            properties.load(new FileInputStream(log4j));
            Path logDir = Paths.get(zookeeperConf.getParent().toString(), "logs");
            if (!Files.exists(logDir)) {
                try {
                    logger.info("It is creating the log directory...");
                    Files.createDirectories(logDir);
                } catch (IOException e) {
                    throw new RuntimeException(String.format("Failed to create the log directory to save zookeeper binary file, file path:%s", context.getSourceFile()), e);
                }
            }
            properties.setProperty("zookeeper.log.dir", logDir.toString());
            FileOutputStream oFile = null;
            try {
                oFile = new FileOutputStream(Paths.get(zookeeperConf.toString(), "log4j.properties").toFile());
                properties.store(oFile, "");
            } finally {
                try {
                    oFile.close();
                } catch (IOException e) {
                    throw new DubboTestException("Failed to close file", e);
                }
            }
            logger.info("The configuration information of log4j.properties are as below,\n" +
                    "which located in " + log4j.getAbsolutePath() + "\n" +
                    propertiesToString(properties));
        } catch (IOException e) {
            throw new DubboTestException(String.format("Failed to update %s file", zooSample), e);
        }
    }

    /**
     * Convert the {@link Properties} instance to {@link String}.
     *
     * @param properties the properties to convert.
     * @return the string converted from {@link Properties} instance.
     */
    private String propertiesToString(Properties properties) {
        StringBuilder builder = new StringBuilder();
        for (Object key : properties.keySet()) {
            builder.append(key);
            builder.append(": ");
            builder.append(properties.get(key));
            builder.append("\n");
        }
        return builder.toString();
    }

    @Override
    protected void doInitialize(ZookeeperContext context) throws DubboTestException {
        for (int i = 0; i < context.getClientPorts().length; i++) {
            this.updateConfig(context, context.getClientPorts()[i], context.getAdminServerPorts()[i]);
        }
    }
}
