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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Unpack the downloaded zookeeper binary archive.
 */
public class UnpackZookeeperInitializer extends ZookeeperInitializer {

    private static final Logger logger = LoggerFactory.getLogger(UnpackZookeeperInitializer.class);

    /**
     * Unpack the zookeeper binary file.
     *
     * @param context    the global context of zookeeper.
     * @param clientPort the client port
     * @throws DubboTestException when an exception occurred
     */
    private void unpack(ZookeeperContext context, int clientPort) throws DubboTestException {
        File sourceFile = context.getSourceFile().toFile();
        Path targetPath = Paths.get(context.getSourceFile().getParent().toString(),
            String.valueOf(clientPort));
        // check if it's unpacked.
        if (targetPath.toFile() != null && targetPath.toFile().isDirectory()) {
            logger.info(String.format("The file has been unpacked, target path:%s", targetPath.toString()));
            return;
        }
        try (FileInputStream fileInputStream = new FileInputStream(sourceFile);
             GzipCompressorInputStream gzipCompressorInputStream = new GzipCompressorInputStream(fileInputStream);
             TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(gzipCompressorInputStream, "UTF-8")) {
            File targetFile = targetPath.toFile();
            TarArchiveEntry entry;
            while ((entry = tarArchiveInputStream.getNextTarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                File curFile = new File(targetFile, entry.getName());
                File parent = curFile.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                try (FileOutputStream outputStream = new FileOutputStream(curFile)) {
                    IOUtils.copy(tarArchiveInputStream, outputStream);
                }
            }
        } catch (IOException e) {
            throw new DubboTestException(String.format("Failed to unpack the zookeeper binary file"), e);
        }
    }

    @Override
    protected void doInitialize(ZookeeperContext context) throws DubboTestException {
        for (int clientPort : context.getClientPorts()) {
            this.unpack(context, clientPort);
            // get the file name, just like apache-zookeeper-{version}-bin
            // the version we maybe unknown if the zookeeper archive binary file is copied by user self.
            Path parentPath = Paths.get(context.getSourceFile().getParent().toString(),
                String.valueOf(clientPort));
            if (!Files.exists(parentPath) ||
                !parentPath.toFile().isDirectory() ||
                parentPath.toFile().listFiles().length != 1) {
                throw new IllegalStateException("There is something wrong in unpacked file!");
            }
            // rename directory
            File sourceFile = parentPath.toFile().listFiles()[0];
            File targetFile = Paths.get(parentPath.toString(), context.getUnpackedDirectory()).toFile();
            sourceFile.renameTo(targetFile);
            if (!Files.exists(targetFile.toPath()) || !targetFile.isDirectory()) {
                throw new IllegalStateException(String.format("Failed to rename the directory. source directory: %s, target directory: %s",
                    sourceFile.toPath().toString(),
                    targetFile.toPath().toString()));
            }
            // get the bin path
            Path zookeeperBin = Paths.get(targetFile.toString(), "bin");
            // update file permission
            for (File file : zookeeperBin.toFile().listFiles()) {
                file.setExecutable(true, false);
                file.setReadable(true, false);
                file.setWritable(false, false);
            }
        }
    }
}
