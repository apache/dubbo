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
import java.net.HttpURLConnection;
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
     * The url format for zookeeper binary file.
     */
    private static final String ZOOKEEPER_BINARY_URL_FORMAT = "https://archive.apache.org/dist/zookeeper/zookeeper-%s/" + ZOOKEEPER_FILE_NAME_FORMAT;

    /**
     * The temporary directory.
     */
    private static final String TEMPORARY_DIRECTORY = "zookeeper";

    /**
     * The timeout when download zookeeper binary archive file.
     */
    private static final int CONNECT_TIMEOUT = 30 * 1000;

    /**
     * The timeout when read the input stream to save in target path.
     */
    private static final int READ_TIMEOUT = 10 * 1000;

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
        if (checkFile(context.getSourceFile())) {
            return;
        }
        String zookeeperFileName = String.format(ZOOKEEPER_FILE_NAME_FORMAT, context.getVersion());
        Path temporaryFilePath;
        try {
            temporaryFilePath = Paths.get(Files.createTempDirectory("").getParent().toString(),
                TEMPORARY_DIRECTORY,
                zookeeperFileName);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Cannot create the temporary directory, file path: %s", TEMPORARY_DIRECTORY), e);
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
            logger.info("It is beginning to download the zookeeper binary archive, it will take several minutes..." +
                "\nThe zookeeper binary archive file will be download from " + zookeeperBinaryUrl + "," +
                "\nwhich will be saved in " + temporaryFilePath.toString() + "," +
                "\nalso it will be renamed to 'apache-zookeeper-bin.tar.gz' and moved into {project.dir}.tmp/zookeeper directory.\n");
            URL url = new URL(zookeeperBinaryUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // set timeout when download the zookeeper binary archive file.
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            // set timeout when read downloaded input stream to save in temporary file path.
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestMethod("GET");
            // use cache first
            connection.setUseCaches(true);
            // only read input stream from HttpURLConnection
            connection.setDoInput(true);
            connection.connect();
            InputStream inputStream = connection.getInputStream();
            Files.copy(inputStream, temporaryFilePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Download zookeeper binary archive failed, download url:%s, file path:%s." +
                    "\nOr you can do something to avoid this problem as below:" +
                    "\n1. Download zookeeper binary archive manually regardless of the version" +
                    "\n2. Rename the downloaded file named 'apache-zookeeper-{version}-bin.tar.gz' to 'apache-zookeeper-bin.tar.gz'" +
                    "\n3. Put the renamed file in {project.dir}.tmp/zookeeper directory, you maybe need to create the directory if necessary.\n",
                zookeeperBinaryUrl, temporaryFilePath), e);
        }

        // check downloaded zookeeper binary file in temporary directory.
        if (!checkFile(temporaryFilePath)) {
            throw new IllegalArgumentException(String.format("There are some unknown problem occurred when downloaded the zookeeper binary archive file, file path:%s", temporaryFilePath));
        }

        // create target directory if necessary
        if (!Files.exists(context.getSourceFile())) {
            try {
                Files.createDirectories(context.getSourceFile().getParent());
            } catch (IOException e) {
                throw new IllegalArgumentException(String.format("Failed to create target directory, the directory path: %s", context.getSourceFile().getParent()), e);
            }
        }

        // copy the downloaded zookeeper binary file into the target file path
        try {
            Files.copy(temporaryFilePath, context.getSourceFile(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Failed to copy file, the source file path: %s, the target file path: %s", temporaryFilePath, context.getSourceFile()), e);
        }

        // checks the zookeeper binary file exists or not again
        if (!checkFile(context.getSourceFile())) {
            throw new IllegalArgumentException(String.format("The zookeeper binary archive file doesn't exist, file path:%s", context.getSourceFile()));
        }
    }
}
