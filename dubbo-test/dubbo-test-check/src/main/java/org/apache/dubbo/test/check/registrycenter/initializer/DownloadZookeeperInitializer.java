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
import org.apache.dubbo.test.check.exception.DubboTestException;
import org.apache.dubbo.test.check.registrycenter.context.ZookeeperContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Download zookeeper binary archive.
 */
public class DownloadZookeeperInitializer extends ZookeeperInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DownloadZookeeperInitializer.class);

    /**
     * The zookeeper binary file name format.
     */
    private static final String ZOOKEEPER_FILE_NAME_FORMAT = "apache-zookeeper-%s-bin.tar.gz";

    /**
     * The url format for zookeeper binary file.
     */
    private static final String ZOOKEEPER_BINARY_URL_FORMAT = "https://archive.apache.org/dist/zookeeper/zookeeper-%s/" + ZOOKEEPER_FILE_NAME_FORMAT;

    /**
     * The temporary directory name
     */
    private static final String TEMPORARY_DIRECTORY_NAME = "dubbo-mocked-zookeeper";

    @Override
    protected void doInitialize(ZookeeperContext context) throws DubboTestException {
        String zookeeperFileName = String.format(ZOOKEEPER_FILE_NAME_FORMAT, context.getVersion());
        try {
            context.setSourceFile(Paths.get(Files.createTempDirectory("").getParent().toString(),
                TEMPORARY_DIRECTORY_NAME,
                zookeeperFileName));
        } catch (IOException e) {
            throw new RuntimeException(String.format("Cannot create the temporary directory, related directory:%s/%s",
                TEMPORARY_DIRECTORY_NAME, zookeeperFileName), e);
        }
        // check if the zookeeper binary file exists
        if (context.getSourceFile() != null && context.getSourceFile().toFile().isFile()) {
            return;
        }
        // create the temporary directory path.
        if (!Files.exists(context.getSourceFile())) {
            try {
                Files.createDirectories(context.getSourceFile());
            } catch (IOException e) {
                throw new RuntimeException(String.format("Failed to create the temporary directory to save zookeeper binary file, file path:%s", context.getSourceFile()), e);
            }
        }
        // download zookeeper binary file
        String zookeeperBinaryUrl = String.format(ZOOKEEPER_BINARY_URL_FORMAT, context.getVersion(), context.getVersion());
        try {
            logger.info("It is beginning to download the zookeeper binary archive, it will take several minutes...");
            URL zookeeperBinaryURL = new URL(zookeeperBinaryUrl);
            InputStream inputStream = zookeeperBinaryURL.openStream();
            Files.copy(inputStream, context.getSourceFile(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Download zookeeper binary archive failed, download url:%s, file path:%s",
                zookeeperBinaryUrl, context.getSourceFile()), e);
        }
        // check if the zookeeper binary file exists again.
        if (context.getSourceFile() == null || !context.getSourceFile().toFile().isFile()) {
            throw new IllegalArgumentException(String.format("The zookeeper binary archive file doesn't exist, file path:%s", context.getSourceFile()));
        }
    }
}
