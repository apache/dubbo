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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.URL.valueOf;
import static org.apache.dubbo.common.config.configcenter.DynamicConfiguration.DEFAULT_GROUP;
import static org.apache.dubbo.common.config.configcenter.file.FileSystemDynamicConfiguration.CONFIG_CENTER_DIR_PARAM_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FileSystemDynamicConfiguration} Test
 */
// Test often failed on GitHub Actions Platform because of file system on Azure
// Change to Disabled because DisabledIfEnvironmentVariable does not work on GitHub.
@Disabled
class FileSystemDynamicConfigurationTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private FileSystemDynamicConfiguration configuration;

    private static final String KEY = "abc-def-ghi";

    private static final String CONTENT = "Hello,World";

    @BeforeEach
    public void init() {
        File rootDirectory = new File(getClassPath(), "config-center");
        rootDirectory.mkdirs();
        try {
            FileUtils.cleanDirectory(rootDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }
        URL url = valueOf("dubbo://127.0.0.1:20880").addParameter(CONFIG_CENTER_DIR_PARAM_NAME, rootDirectory.getAbsolutePath());
        configuration = new FileSystemDynamicConfiguration(url);
    }

    @AfterEach
    public void destroy() throws Exception {
        FileUtils.deleteQuietly(configuration.getRootDirectory());
        configuration.close();
    }

    private String getClassPath() {
        return getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
    }

    @Test
    void testInit() {

        assertEquals(new File(getClassPath(), "config-center"), configuration.getRootDirectory());
        assertEquals("UTF-8", configuration.getEncoding());
        assertEquals(ThreadPoolExecutor.class, configuration.getWorkersThreadPool().getClass());
        assertEquals(1, (configuration.getWorkersThreadPool()).getCorePoolSize());
        assertEquals(1, (configuration.getWorkersThreadPool()).getMaximumPoolSize());

        if (configuration.isBasedPoolingWatchService()) {
            assertEquals(2, configuration.getDelay());
        } else {
            assertNull(configuration.getDelay());
        }
    }

    @Test
    void testPublishAndGetConfig() {
        assertTrue(configuration.publishConfig(KEY, CONTENT));
        assertTrue(configuration.publishConfig(KEY, CONTENT));
        assertTrue(configuration.publishConfig(KEY, CONTENT));
        assertEquals(CONTENT, configuration.getConfig(KEY, DEFAULT_GROUP));
    }

    @Test
    void testAddAndRemoveListener() throws InterruptedException {

        configuration.publishConfig(KEY, "A");

        AtomicBoolean processedEvent = new AtomicBoolean();

        configuration.addListener(KEY, event -> {

            processedEvent.set(true);
            assertEquals(KEY, event.getKey());
            logger.info(String.format("[%s] " + event + "\n", Thread.currentThread().getName()));
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
            logger.info(String.format("[%s] " + event + "\n", Thread.currentThread().getName()));
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
        configuration.getRootDirectory();
        File keyFile = new File(KEY, DEFAULT_GROUP);
        FileUtils.deleteQuietly(keyFile);
        while (!processedEvent.get()) {
            Thread.sleep(1 * 1000L);
        }
    }

    @Test
    void testRemoveConfig() throws Exception {

        assertTrue(configuration.publishConfig(KEY, DEFAULT_GROUP, "A"));

        assertEquals("A", FileUtils.readFileToString(configuration.configFile(KEY, DEFAULT_GROUP), configuration.getEncoding()));

        assertTrue(configuration.removeConfig(KEY, DEFAULT_GROUP));

        assertFalse(configuration.configFile(KEY, DEFAULT_GROUP).exists());

    }
//
//    @Test
//    public void testGetConfigKeys() throws Exception {
//
//        assertTrue(configuration.publishConfig("A", DEFAULT_GROUP, "A"));
//
//        assertTrue(configuration.publishConfig("B", DEFAULT_GROUP, "B"));
//
//        assertTrue(configuration.publishConfig("C", DEFAULT_GROUP, "C"));
//
//        assertEquals(new TreeSet(asList("A", "B", "C")), configuration.getConfigKeys(DEFAULT_GROUP));
//    }
}
