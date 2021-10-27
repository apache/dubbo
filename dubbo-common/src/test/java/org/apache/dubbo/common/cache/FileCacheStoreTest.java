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

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileCacheStoreTest {
    FileCacheStore cacheStore;

    @Test
    public void testCache() throws Exception {
        String directoryPath = getDirectoryOfClassPath();
        String filePath = "test-cache.dubbo.cache";
        cacheStore = new FileCacheStore(directoryPath, filePath);
        Map<String, String> properties = cacheStore.loadCache(10);
        assertEquals(2, properties.size());

        Map<String, String> newProperties = new HashMap<>();
        newProperties.put("newKey1", "newValue1");
        newProperties.put("newKey2", "newValue2");
        newProperties.put("newKey3", "newValue3");
        newProperties.put("newKey4", "newValue4");
        cacheStore = new FileCacheStore(directoryPath, "non-exit.dubbo.cache");
        cacheStore.refreshCache(newProperties, "test refresh cache");
        Map<String, String> propertiesLimitTo2 = cacheStore.loadCache(2);
        assertEquals(2, propertiesLimitTo2.size());

        Map<String, String> propertiesLimitTo10 = cacheStore.loadCache(10);
        assertEquals(4, propertiesLimitTo10.size());
    }

    private String getDirectoryOfClassPath() throws URISyntaxException {
        URL resource = this.getClass().getResource("/log4j.xml");
        String path = Paths.get(resource.toURI()).toFile().getAbsolutePath();
        int index = path.indexOf("log4j.xml");
        String directoryPath = path.substring(0, index);
        return directoryPath;
    }

}
