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
package org.apache.dubbo.common.config.configcenter.file;

import org.apache.dubbo.common.URL;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.dubbo.common.URL.valueOf;
import static org.apache.dubbo.common.config.configcenter.DynamicConfiguration.DEFAULT_GROUP;
import static org.apache.dubbo.common.config.configcenter.file.FileSystemDynamicConfiguration.CONFIG_CENTER_DIR_PARAM_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FileSystemDynamicConfiguration} Test
 */
public class FileSystemDynamicConfigurationTest {

    private FileSystemDynamicConfiguration configuration;

    private static final String KEY = "abc-def-ghi";

    private static final String CONTENT = "Hello,World";

    @BeforeEach
    public void init() {
        File rootDirectory = new File(getClassPath(), "config-center");
        rootDirectory.mkdirs();
        URL url = valueOf("dubbo://127.0.0.1:20880").addParameter(CONFIG_CENTER_DIR_PARAM_NAME, rootDirectory.getAbsolutePath());
        configuration = new FileSystemDynamicConfiguration(url);
        deleteQuietly(configuration.getRootDirectory());
    }

    @AfterEach
    public void destroy() throws Exception {
        configuration.close();
    }

    private String getClassPath() {
        return getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
    }

    @Test
    public void testInit() {

        assertEquals(new File(getClassPath(), "config-center"), configuration.getRootDirectory());
        assertEquals("UTF-8", configuration.getEncoding());
        assertEquals(ThreadPoolExecutor.class, configuration.getWorkersThreadPool().getClass());
        assertEquals(1, (configuration.getWorkersThreadPool()).getCorePoolSize());
        assertEquals(1, (configuration.getWorkersThreadPool()).getMaximumPoolSize());
        assertNotNull(configuration.getWatchEventsLoopThreadPool());
        assertEquals(1, (configuration.getWatchEventsLoopThreadPool()).getCorePoolSize());
        assertEquals(1, (configuration.getWatchEventsLoopThreadPool()).getMaximumPoolSize());

        if (configuration.isBasedPoolingWatchService()) {
            assertEquals(2, configuration.getDelay());
        } else {
            assertNull(configuration.getDelay());
        }
    }

    @Test
    public void testPublishAndGetConfig() {
        assertTrue(configuration.publishConfig(KEY, CONTENT));
        assertTrue(configuration.publishConfig(KEY, CONTENT));
        assertTrue(configuration.publishConfig(KEY, CONTENT));
        assertEquals(CONTENT, configuration.getConfig(KEY, DEFAULT_GROUP));
    }

    @Test
    public void testAddAndRemoveListener() throws InterruptedException {

        configuration.publishConfig(KEY, "A");

        AtomicBoolean processedEvent = new AtomicBoolean();

        configuration.addListener(KEY, event -> {

            processedEvent.set(true);
            assertEquals(KEY, event.getKey());
            System.out.printf("[%s] " + event + "\n", Thread.currentThread().getName());
        });


        configuration.publishConfig(KEY, "B");
        while (!processedEvent.get()) {
            Thread.sleep(1 * 1000L);
        }

        processedEvent.set(false);
        configuration.publishConfig(KEY, "C");
        while (!processedEvent.get()) {
            Thread.sleep(1 * 1000L);
        }

        processedEvent.set(false);
        configuration.publishConfig(KEY, "D");
        while (!processedEvent.get()) {
            Thread.sleep(1 * 1000L);
        }

        configuration.addListener("test", "test", event -> {
            processedEvent.set(true);
            assertEquals("test", event.getKey());
            System.out.printf("[%s] " + event + "\n", Thread.currentThread().getName());
        });
        processedEvent.set(false);
        configuration.publishConfig("test", "test", "TEST");
        while (!processedEvent.get()) {
            Thread.sleep(1 * 1000L);
        }

        configuration.publishConfig("test", "test", "TEST");
        configuration.publishConfig("test", "test", "TEST");
        configuration.publishConfig("test", "test", "TEST");


        processedEvent.set(false);
        File keyFile = configuration.configFile(KEY, DEFAULT_GROUP);
        FileUtils.deleteQuietly(keyFile);
        while (!processedEvent.get()) {
            Thread.sleep(1 * 1000L);
        }
    }
}
