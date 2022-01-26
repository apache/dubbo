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
package org.apache.dubbo.registry.client.metadata.store;

import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.metadata.MetadataInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

public class MetaCacheManagerTest {

    @BeforeEach
    public void setup() throws URISyntaxException {
        String directory = getDirectoryOfClassPath();
        System.setProperty("dubbo.meta.cache.filePath", directory);
        System.setProperty("dubbo.meta.cache.fileName", "test-metadata.dubbo.cache");
    }

    @Test
    public void testCache() {
//        ScheduledExecutorService cacheRefreshExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Dubbo-cache-refresh"));
//        ExecutorRepository executorRepository = Mockito.mock(ExecutorRepository.class);
//        when(executorRepository.getCacheRefreshExecutor()).thenReturn(cacheRefreshExecutor);
//        ExtensionAccessor extensionAccessor = Mockito.mock(ExtensionAccessor.class);
//        when(extensionAccessor.getDefaultExtension(ExecutorRepository.class)).thenReturn(executorRepository);

        MetaCacheManager cacheManager = new MetaCacheManager();
        try {
//        cacheManager.setExtensionAccessor(extensionAccessor);

            MetadataInfo metadataInfo = cacheManager.get("1");
            assertNotNull(metadataInfo);
            assertEquals("demo", metadataInfo.getApp());
            metadataInfo = cacheManager.get("2");
            assertNull(metadataInfo);

            Map<String, MetadataInfo> newMetadatas = new HashMap<>();
            MetadataInfo metadataInfo2 = JsonUtils.getGson().fromJson("{\"app\":\"demo2\",\"services\":{\"greeting/org.apache.dubbo.registry.service.DemoService2:1.0.0:dubbo\":{\"name\":\"org.apache.dubbo.registry.service.DemoService2\",\"group\":\"greeting\",\"version\":\"1.0.0\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.registry.service.DemoService2\",\"params\":{\"application\":\"demo-provider2\",\"sayHello.timeout\":\"7000\",\"version\":\"1.0.0\",\"timeout\":\"5000\",\"group\":\"greeting\"}},\"greeting/org.apache.dubbo.registry.service.DemoService:1.0.0:dubbo\":{\"name\":\"org.apache.dubbo.registry.service.DemoService\",\"group\":\"greeting\",\"version\":\"1.0.0\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.registry.service.DemoService\",\"params\":{\"application\":\"demo-provider2\",\"version\":\"1.0.0\",\"timeout\":\"5000\",\"group\":\"greeting\"}}}}\n", MetadataInfo.class);
            newMetadatas.put("2", metadataInfo2);

            cacheManager.update(newMetadatas);
            metadataInfo = cacheManager.get("1");
            assertNotNull(metadataInfo);
            assertEquals("demo", metadataInfo.getApp());
            metadataInfo = cacheManager.get("2");
            assertNotNull(metadataInfo);
            assertEquals("demo2", metadataInfo.getApp());
        } finally {
            cacheManager.destroy();
        }
    }


    @Test
    public void testCacheDump() {
        System.setProperty("dubbo.meta.cache.fileName", "not-exist.dubbo.cache");
        MetadataInfo metadataInfo3 = JsonUtils.getGson().fromJson("{\"app\":\"demo3\",\"services\":{\"greeting/org.apache.dubbo.registry.service.DemoService2:1.0.0:dubbo\":{\"name\":\"org.apache.dubbo.registry.service.DemoService2\",\"group\":\"greeting\",\"version\":\"1.0.0\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.registry.service.DemoService2\",\"params\":{\"application\":\"demo-provider2\",\"sayHello.timeout\":\"7000\",\"version\":\"1.0.0\",\"timeout\":\"5000\",\"group\":\"greeting\"}},\"greeting/org.apache.dubbo.registry.service.DemoService:1.0.0:dubbo\":{\"name\":\"org.apache.dubbo.registry.service.DemoService\",\"group\":\"greeting\",\"version\":\"1.0.0\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.registry.service.DemoService\",\"params\":{\"application\":\"demo-provider2\",\"version\":\"1.0.0\",\"timeout\":\"5000\",\"group\":\"greeting\"}}}}\n", MetadataInfo.class);
        MetaCacheManager cacheManager = new MetaCacheManager();
        try {
            cacheManager.put("3", metadataInfo3);

            try {
                MetaCacheManager.CacheRefreshTask task = new MetaCacheManager.CacheRefreshTask(cacheManager.cacheStore, cacheManager.cache);
                task.run();
            } catch (Exception e) {
                fail();
            } finally {
                cacheManager.destroy();
            }

            MetaCacheManager newCacheManager = null;
            try {
                newCacheManager = new MetaCacheManager();
                MetadataInfo metadataInfo = newCacheManager.get("3");
                assertNotNull(metadataInfo);
                assertEquals("demo3", metadataInfo.getApp());
            } finally {
                newCacheManager.destroy();
            }
        } finally {
            cacheManager.destroy();
        }
    }


    private String getDirectoryOfClassPath() throws URISyntaxException {
        URL resource = this.getClass().getResource("/log4j.xml");
        String path = Paths.get(resource.toURI()).toFile().getAbsolutePath();
        int index = path.indexOf("log4j.xml");
        String directoryPath = path.substring(0, index);
        return directoryPath;
    }
}
