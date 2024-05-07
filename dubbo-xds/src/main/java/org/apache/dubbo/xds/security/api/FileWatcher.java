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
package org.apache.dubbo.xds.security.api;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.IOUtils;
import org.apache.dubbo.common.utils.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

public class FileWatcher {

    private Map<String, Pair<byte[], FileAlterationMonitor>> fileToWatch = new ConcurrentHashMap<>();

    private ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    public void registerWatch(String path) throws Exception {
        registerWatch(path, 3000);
    }

    public byte[] readWatchedFile(String path) {
        Pair<byte[], FileAlterationMonitor> pair = fileToWatch.get(path);
        return pair == null ? null : pair.getLeft();
    }

    public void registerWatch(String path, long checkInterval) throws Exception {
        FileAlterationObserver observer = new FileAlterationObserver(path);
        FileAlterationMonitor monitor = new FileAlterationMonitor(checkInterval);
        FileAlterationListener listener = new FileAlterationListenerAdaptor() {
            @Override
            public void onStart(FileAlterationObserver observer) {
                try {
                    fileToWatch.put(
                            path, new Pair<>(IOUtils.toByteArray(Files.newInputStream(Paths.get(path))), monitor));
                } catch (IOException e) {
                    logger.warn("", "", "", "Failed to read file in path=" + path);
                }
            }

            @Override
            public void onFileChange(File file) {
                try {
                    fileToWatch.put(
                            path, new Pair<>(IOUtils.toByteArray(Files.newInputStream(file.toPath())), monitor));
                } catch (IOException e) {
                    logger.error("", e.getCause().toString(), "", "Failed to read changed file.", e);
                }
            }

            @Override
            public void onFileCreate(File file) {
                try {
                    fileToWatch.put(
                            path, new Pair<>(IOUtils.toByteArray(Files.newInputStream(file.toPath())), monitor));
                } catch (IOException e) {
                    logger.error("", e.getCause().toString(), "", "Failed to read newly create file.", e);
                }
            }

            @Override
            public void onFileDelete(File file) {
                Pair<byte[], FileAlterationMonitor> removed = fileToWatch.remove(path);
                try {
                    removed.getRight().stop();
                } catch (Exception e) {
                    logger.error("", e.getCause().toString(), "", "Failed to stop watch deleted file.", e);
                    ;
                }
            }
        };
        observer.addListener(listener);
        monitor.addObserver(observer);
        monitor.start();
    }
}
