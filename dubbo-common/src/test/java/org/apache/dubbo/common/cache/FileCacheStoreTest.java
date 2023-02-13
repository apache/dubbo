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
package org.apache.dubbo.common.cache;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.MD5Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.cache.FileCacheStoreFactory.SUFFIX;
import static org.apache.dubbo.common.cache.FileCacheStoreFactory.safeName;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FileCacheStoreTest {
    private FileCacheStore cacheStore;

    @Test
    void testCache() throws Exception {
        String directoryPath = getDirectoryOfClassPath();
        String filePath = "test-cache.dubbo.cache";
        cacheStore = FileCacheStoreFactory.getInstance(directoryPath, "", filePath);
        Map<String, String> properties = cacheStore.loadCache(10);
        assertEquals(2, properties.size());

        Map<String, String> newProperties = new HashMap<>();
        newProperties.put("newKey1", "newValue1");
        newProperties.put("newKey2", "newValue2");
        newProperties.put("newKey3", "newValue3");
        newProperties.put("newKey4", "newValue4");
        cacheStore = FileCacheStoreFactory.getInstance(directoryPath, "", "non-exit.dubbo.cache");
        cacheStore.refreshCache(newProperties, "test refresh cache", 0);
        Map<String, String> propertiesLimitTo2 = cacheStore.loadCache(2);
        assertEquals(2, propertiesLimitTo2.size());

        Map<String, String> propertiesLimitTo10 = cacheStore.loadCache(10);
        assertEquals(4, propertiesLimitTo10.size());

        cacheStore.destroy();
    }

    @Test
    void testCache2() throws Exception {
        String shortBasePath = "/Users/aming/.dubbo";
        StringBuilder longBasePathBuilder = new StringBuilder();
        longBasePathBuilder.append(shortBasePath);
        String longBasePath = longBasePathBuilder.toString();
        String filePrefix = ".metadata";
        StringBuilder filePrefixBuilder = new StringBuilder();
        for (int i = 0; i < 28; i++) {
            filePrefixBuilder.append("metadata");
        }
        filePrefix = filePrefixBuilder.toString();

        String shortFilePath = filePrefix + "dubbo-demo-api-provider-2.zookeeper.127.0.0.1:2181";
        StringBuilder longFilePathBuilder = new StringBuilder();
        longFilePathBuilder.append(filePrefix);
        for (int i = 0; i < 100; i++) {
            longFilePathBuilder.append("metadata");
        }
        longFilePathBuilder.append(".zookeeper.127.0.0.1:2181");
        String longFilePath = longFilePathBuilder.toString();
        MD5Utils md5Utils = new MD5Utils();
//        FileCacheStore unixShortBashPathWithShortFilePathShortenCacheStore = FileCacheStoreFactory.getInstance(shortBasePath, filePrefix, shortFilePath);
//        FileCacheStore unixShortBashPathWithLongFilePathShortenCacheStore = FileCacheStoreFactory.getInstance(shortBasePath, filePrefix, longFilePath);

//        FileCacheStore unixLongBashPathWithShortFilePathShortenCacheStore = FileCacheStoreFactory.getInstance(longBasePath, filePrefix, shortFilePath);
//
//        System.setProperty("os.name", "Windows");
        if (!System.getProperty("os.name").equalsIgnoreCase("Windows")) {
            System.setProperty(CommonConstants.File_ADDRESS_SHORTENED, "true");
            FileCacheStore unixLongBashPathWithLongFilePathShortenCacheStore = FileCacheStoreFactory.getInstance(longBasePath, filePrefix, longFilePath);
            String expectLongFilePath = safeName(longFilePath);
            if (!expectLongFilePath.endsWith(SUFFIX)) {
                expectLongFilePath = expectLongFilePath + SUFFIX;
            }
            String expectValue = shortBasePath + "/" + filePrefix + "." + md5Utils.getMd5String16Bit(expectLongFilePath);
            Assertions.assertEquals(unixLongBashPathWithLongFilePathShortenCacheStore.getCacheFilePath(), expectValue);

            System.setProperty(CommonConstants.File_ADDRESS_SHORTENED, "false");
            try {
                FileCacheStore unixLongBashPathWithLongFilePathCacheStore = FileCacheStoreFactory.getInstance(longBasePath, filePrefix, longFilePath);
//                Assertions.fail();
            } catch (Exception e) {
//                Assertions.assertTrue(e.getClass().isAssignableFrom(IOException.class));
            }
        }
    }

    @Test
    void testFileSizeExceed() throws Exception {
        String directoryPath = getDirectoryOfClassPath();
        Map<String, String> newProperties = new HashMap<>();
        newProperties.put("newKey1", "newValue1");
        newProperties.put("newKey2", "newValue2");
        newProperties.put("newKey3", "newValue3");
        newProperties.put("newKey4", "newValue4");
        cacheStore = FileCacheStoreFactory.getInstance(directoryPath, "", "non-exit.dubbo.cache");
        cacheStore.refreshCache(newProperties, "test refresh cache", 2);
        Map<String, String> propertiesLimitTo1 = cacheStore.loadCache(2);
        assertEquals(0, propertiesLimitTo1.size());
    }

    private String getDirectoryOfClassPath() throws URISyntaxException {
        URL resource = this.getClass().getResource("/log4j.xml");
        String path = Paths.get(resource.toURI()).toFile().getAbsolutePath();
        int index = path.indexOf("log4j.xml");
        String directoryPath = path.substring(0, index);
        return directoryPath;
    }

}
