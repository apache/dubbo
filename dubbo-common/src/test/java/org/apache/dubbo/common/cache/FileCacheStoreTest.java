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
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileCacheStoreTest {
    FileCacheStore cacheStore;

    @Test
    public void testCache() throws Exception {
        String directoryPath = getDirectoryOfClassPath();
        String filePath = "test-cache.dubbo.cache";
        cacheStore = new FileCacheStore(directoryPath, filePath);
        Properties properties = cacheStore.loadCache();
        assertEquals(2, properties.size());

        Properties newProperties = new Properties();
        newProperties.put("newKey", "newValue");
        cacheStore.refreshCache(newProperties);
        properties = cacheStore.loadCache();
        assertEquals(1, properties.size());
    }

    private String getDirectoryOfClassPath() throws URISyntaxException {
        URL resource = this.getClass().getResource("/log4j.xml");
        String path = Paths.get(resource.toURI()).toFile().getAbsolutePath();
        int index = path.indexOf("log4j.xml");
        String directoryPath = path.substring(0, index);
        return directoryPath;
    }

}
