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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

class FileCacheStoreFactoryTest {

    @Test
    void testSafeName() throws URISyntaxException {
        FileCacheStore store1 = FileCacheStoreFactory.getInstance(getDirectoryOfClassPath(), "../../../dubbo");
        Assertions.assertEquals(getDirectoryOfClassPath() + "..%002f..%002f..%002fdubbo.dubbo.cache", store1.getCacheFilePath());
        store1.destroy();

        FileCacheStore store2 = FileCacheStoreFactory.getInstance(getDirectoryOfClassPath(), "../../../中文");
        Assertions.assertEquals(getDirectoryOfClassPath() + "..%002f..%002f..%002f%4e2d%6587.dubbo.cache", store2.getCacheFilePath());
        store2.destroy();
    }

    @Test
    void testPathIsFile() throws URISyntaxException, IOException {
        String basePath = getDirectoryOfClassPath();
        String filePath = basePath + File.separator + "isFile";
        new File(filePath).createNewFile();

        Assertions.assertThrows(RuntimeException.class, () -> FileCacheStoreFactory.getInstance(filePath, "dubbo"));
    }

    @Test
    void testCacheContains() throws URISyntaxException {
        FileCacheStore store1 = FileCacheStoreFactory.getInstance(getDirectoryOfClassPath(), "testCacheContains");
        Assertions.assertNotNull(store1.getCacheFilePath());

        FileCacheStoreFactory.getCacheMap().remove(store1.getCacheFilePath());
        FileCacheStore store2 = FileCacheStoreFactory.getInstance(getDirectoryOfClassPath(), "testCacheContains");
        Assertions.assertEquals(FileCacheStore.Empty.class, store2.getClass());

        store1.destroy();
        store2.destroy();

        FileCacheStore store3 = FileCacheStoreFactory.getInstance(getDirectoryOfClassPath(), "testCacheContains");
        Assertions.assertNotNull(store3.getCacheFilePath());
        store3.destroy();
    }

    private String getDirectoryOfClassPath() throws URISyntaxException {
        URL resource = this.getClass().getResource("/log4j.xml");
        String path = Paths.get(resource.toURI()).toFile().getAbsolutePath();
        int index = path.indexOf("log4j.xml");
        String directoryPath = path.substring(0, index);
        return directoryPath;
    }
}