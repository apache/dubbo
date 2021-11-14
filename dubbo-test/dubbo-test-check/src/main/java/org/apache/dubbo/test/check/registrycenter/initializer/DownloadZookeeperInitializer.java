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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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
     * The target name of zookeeper binary file.
     */
    private static final String TARGET_ZOOKEEPER_FILE_NAME = "apache-zookeeper-bin.tar.gz";

    /**
     * The target directory.
     * The zookeeper binary file named {@link #TARGET_ZOOKEEPER_FILE_NAME} will be saved in
     * {@link #TARGET_DIRECTORY} if it downloaded successfully.
     */
    private static final String TARGET_DIRECTORY = "test" + File.separator + "zookeeper";

    /**
     * The url format for zookeeper binary file.
     */
    private static final String ZOOKEEPER_BINARY_URL_FORMAT = "https://archive.apache.org/dist/zookeeper/zookeeper-%s/" + ZOOKEEPER_FILE_NAME_FORMAT;

    /**
     * The path of target zookeeper binary file.
     */
    private static final Path TARGET_FILE_PATH = getTargetFilePath();

    /**
     * Returns the target file path.
     */
    private static Path getTargetFilePath() {
        String currentWorkDirectory = System.getProperty("user.dir");
        logger.info("Current work directory: " + currentWorkDirectory);
        int index = currentWorkDirectory.lastIndexOf(File.separator + "dubbo" + File.separator);
        Path targetFilePath = Paths.get(currentWorkDirectory.substring(0, index),
            "dubbo",
            TARGET_DIRECTORY,
            TARGET_ZOOKEEPER_FILE_NAME);
        logger.info("Target file's absolute directory: " + targetFilePath.toString());
        return targetFilePath;
    }

    /**
     * Returns {@code true} if the file exists with the given file path, otherwise {@code false}.
     *
     * @param filePath the file path to check.
     */
    private boolean checkFile(Path filePath) {
        return Files.exists(filePath) && filePath.toFile().isFile();
    }

    @Override
    protected void doInitialize(ZookeeperContext context) throws DubboTestException {
        // checks the zookeeper binary file exists or not
        if (checkFile(TARGET_FILE_PATH)) {
            context.setSourceFile(TARGET_FILE_PATH);
            return;
        }
        String zookeeperFileName = String.format(ZOOKEEPER_FILE_NAME_FORMAT, context.getVersion());
        Path temporaryFilePath;
        try {
            temporaryFilePath = Paths.get(Files.createTempDirectory("").getParent().toString(),
                TARGET_DIRECTORY,
                zookeeperFileName);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Cannot create the temporary directory, related directory:%s/%s",
                TARGET_DIRECTORY, zookeeperFileName), e);
        }

        // delete the downloaded zookeeper binary file in temporary, because it maybe a broken file.
        if (Files.exists(temporaryFilePath)) {
            try {
                Files.deleteIfExists(temporaryFilePath);
            } catch (IOException e) {
                //ignored
                logger.warn("Failed to delete the zookeeper binary file, file path: " + temporaryFilePath.toString(), e);
            }
        }

        // create the temporary directory path.
        try {
            Files.createDirectories(temporaryFilePath.getParent());
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to create the temporary directory to save zookeeper binary file, file path:%s", temporaryFilePath.getParent()), e);
        }

        // download zookeeper binary file in temporary directory.
        String zookeeperBinaryUrl = String.format(ZOOKEEPER_BINARY_URL_FORMAT, context.getVersion(), context.getVersion());
        try {
            logger.info("It is beginning to download the zookeeper binary archive, it will take several minutes...");
            URL zookeeperBinaryURL = new URL(zookeeperBinaryUrl);
            InputStream inputStream = zookeeperBinaryURL.openStream();
            Files.copy(inputStream, temporaryFilePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Download zookeeper binary archive failed, download url:%s, file path:%s",
                zookeeperBinaryUrl, context.getSourceFile()), e);
        }

        // check downloaded zookeeper binary file in temporary directory.
        if (!checkFile(temporaryFilePath)) {
            throw new IllegalArgumentException(String.format("There are some unknown problem occurred when downloaded the zookeeper binary archive file, file path:%s", temporaryFilePath));
        }

        // move the downloaded zookeeper binary file into the target file path
        try {
            // make sure the action of MOVE is atomic.
            Files.move(temporaryFilePath, TARGET_FILE_PATH, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Failed to move file, the source file path: %s, the target file path: %s", temporaryFilePath, TARGET_FILE_PATH));
        }

        // checks the zookeeper binary file exists or not again
        if (!checkFile(TARGET_FILE_PATH)) {
            throw new IllegalArgumentException(String.format("The zookeeper binary archive file doesn't exist, file path:%s", TARGET_FILE_PATH));
        }

        // set the source file's path.
        context.setSourceFile(TARGET_FILE_PATH);
    }
}
