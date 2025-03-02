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

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.SystemPropertyConfigUtils;
import org.apache.dubbo.metadata.MetadataInfo;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class MetaCacheManagerTest {

    @BeforeEach
    public void setup() throws URISyntaxException {
        String directory = getDirectoryOfClassPath();
        SystemPropertyConfigUtils.setSystemProperty(CommonConstants.DubboProperty.DUBBO_META_CACHE_FILEPATH, directory);
        SystemPropertyConfigUtils.setSystemProperty(
                CommonConstants.DubboProperty.DUBBO_META_CACHE_FILENAME, "test-metadata.dubbo.cache");
    }

    @AfterEach
    public void clear() throws URISyntaxException {
        SystemPropertyConfigUtils.clearSystemProperty(CommonConstants.DubboProperty.DUBBO_META_CACHE_FILEPATH);
        SystemPropertyConfigUtils.clearSystemProperty(CommonConstants.DubboProperty.DUBBO_META_CACHE_FILENAME);
    }

    @Test
    void testCache() {
        //        ScheduledExecutorService cacheRefreshExecutor = Executors.newSingleThreadScheduledExecutor(new
        // NamedThreadFactory("Dubbo-cache-refresh"));
        //        ExecutorRepository executorRepository = Mockito.mock(ExecutorRepository.class);
        //        when(executorRepository.getCacheRefreshExecutor()).thenReturn(cacheRefreshExecutor);
        //        ExtensionAccessor extensionAccessor = Mockito.mock(ExtensionAccessor.class);
        //        when(extensionAccessor.getDefaultExtension(ExecutorRepository.class)).thenReturn(executorRepository);

        MetaCacheManager cacheManager = new MetaCacheManager();
        try {
            //        cacheManager.setExtensionAccessor(extensionAccessor);

            MetadataInfo metadataInfo = cacheManager.get("1");
            assertNull(metadataInfo);
            metadataInfo = cacheManager.get("2");
            assertNull(metadataInfo);

            metadataInfo = cacheManager.get("065787862412c2cc0a1b9577bc194c9a");
            assertNotNull(metadataInfo);
            assertEquals("demo", metadataInfo.getApp());

            Map<String, MetadataInfo> newMetadatas = new HashMap<>();
            MetadataInfo metadataInfo2 = JsonUtils.toJavaObject(
                    "{\"app\":\"demo2\",\"services\":{\"greeting/org.apache.dubbo.registry.service.DemoService2:1.0.0:dubbo\":{\"name\":\"org.apache.dubbo.registry.service.DemoService2\",\"group\":\"greeting\",\"version\":\"1.0.0\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.registry.service.DemoService2\",\"params\":{\"application\":\"demo-provider2\",\"sayHello.timeout\":\"7000\",\"version\":\"1.0.0\",\"timeout\":\"5000\",\"group\":\"greeting\"}},\"greeting/org.apache.dubbo.registry.service.DemoService:1.0.0:dubbo\":{\"name\":\"org.apache.dubbo.registry.service.DemoService\",\"group\":\"greeting\",\"version\":\"1.0.0\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.registry.service.DemoService\",\"params\":{\"application\":\"demo-provider2\",\"version\":\"1.0.0\",\"timeout\":\"5000\",\"group\":\"greeting\"}}}}\n",
                    MetadataInfo.class);
            assertNotEquals("2", metadataInfo2.calRevision());
            newMetadatas.put("2", metadataInfo2);

            MetadataInfo metadataInfo3 = JsonUtils.toJavaObject(
                    "{\"app\":\"demo3\",\"services\":{\"greeting/org.apache.dubbo.registry.service.DemoService3:1.0.0:dubbo\":{\"name\":\"org.apache.dubbo.registry.service.DemoService3\",\"group\":\"greeting\",\"version\":\"1.0.0\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.registry.service.DemoService3\",\"params\":{\"application\":\"demo-provider3\",\"sayHello.timeout\":\"7000\",\"version\":\"1.0.0\",\"timeout\":\"5000\",\"group\":\"greeting\"}},\"greeting/org.apache.dubbo.registry.service.DemoService:1.0.0:dubbo\":{\"name\":\"org.apache.dubbo.registry.service.DemoService\",\"group\":\"greeting\",\"version\":\"1.0.0\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.registry.service.DemoService\",\"params\":{\"application\":\"demo-provider3\",\"version\":\"1.0.0\",\"timeout\":\"5000\",\"group\":\"greeting\"}}}}\n",
                    MetadataInfo.class);
            assertEquals("84f10ebf1226b496c9ff102f311918e4", metadataInfo3.calRevision());
            newMetadatas.put("84f10ebf1226b496c9ff102f311918e4", metadataInfo3);

            cacheManager.update(newMetadatas);
            metadataInfo = cacheManager.get("1");
            assertNull(metadataInfo);

            metadataInfo = cacheManager.get("065787862412c2cc0a1b9577bc194c9a");
            assertNotNull(metadataInfo);
            assertEquals("demo", metadataInfo.getApp());

            metadataInfo = cacheManager.get("2");
            assertNull(metadataInfo);

            metadataInfo = cacheManager.get("84f10ebf1226b496c9ff102f311918e4");
            assertNotNull(metadataInfo);
            assertEquals("demo3", metadataInfo.getApp());
            assertTrue(metadataInfo
                    .getServices()
                    .containsKey("greeting/org.apache.dubbo.registry.service.DemoService3:1.0.0:dubbo"));
        } finally {
            cacheManager.destroy();
        }
    }

    @Test
    void testCacheDump() {
        System.setProperty("dubbo.meta.cache.fileName", "not-exist.dubbo.cache");
        MetadataInfo metadataInfo3 = JsonUtils.toJavaObject(
                "{\"app\":\"demo3\",\"services\":{\"greeting/org.apache.dubbo.registry.service.DemoService2:1.0.0:dubbo\":{\"name\":\"org.apache.dubbo.registry.service.DemoService2\",\"group\":\"greeting\",\"version\":\"1.0.0\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.registry.service.DemoService2\",\"params\":{\"application\":\"demo-provider2\",\"sayHello.timeout\":\"7000\",\"version\":\"1.0.0\",\"timeout\":\"5000\",\"group\":\"greeting\"}},\"greeting/org.apache.dubbo.registry.service.DemoService:1.0.0:dubbo\":{\"name\":\"org.apache.dubbo.registry.service.DemoService\",\"group\":\"greeting\",\"version\":\"1.0.0\",\"protocol\":\"dubbo\",\"path\":\"org.apache.dubbo.registry.service.DemoService\",\"params\":{\"application\":\"demo-provider2\",\"version\":\"1.0.0\",\"timeout\":\"5000\",\"group\":\"greeting\"}}}}\n",
                MetadataInfo.class);
        MetaCacheManager cacheManager = new MetaCacheManager();
        try {
            assertEquals("97370ff779b6b6ebb7012bae61710de2", metadataInfo3.calRevision());
            cacheManager.put("97370ff779b6b6ebb7012bae61710de2", metadataInfo3);

            try {
                MetaCacheManager.CacheRefreshTask<MetadataInfo> task = new MetaCacheManager.CacheRefreshTask<>(
                        cacheManager.getCacheStore(), cacheManager.getCache(), cacheManager, 0);
                task.run();
            } catch (Exception e) {
                fail();
            } finally {
                cacheManager.destroy();
            }

            MetaCacheManager newCacheManager = null;
            try {
                newCacheManager = new MetaCacheManager();
                MetadataInfo metadataInfo = newCacheManager.get("97370ff779b6b6ebb7012bae61710de2");
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
        URL resource = this.getClass().getResource("/log4j2-test.xml");
        String path = Paths.get(resource.toURI()).toFile().getAbsolutePath();
        int index = path.indexOf("log4j2-test.xml");
        String directoryPath = path.substring(0, index);
        return directoryPath;
    }
}
