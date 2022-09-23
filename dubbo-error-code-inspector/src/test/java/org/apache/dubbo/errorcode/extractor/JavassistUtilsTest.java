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

package org.apache.dubbo.errorcode.extractor;

import org.apache.dubbo.errorcode.util.FileUtils;

import javassist.bytecode.ClassFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Unit tests of JavassistUtils.
 */
class JavassistUtilsTest {
    @Test
    void testOpenClassFile() {
        String classFilePath = FileUtils.getResourceFilePath("FileCacheStore.jcls");

        Assertions.assertEquals("org.apache.dubbo.common.cache.FileCacheStore", JavassistUtils.openClassFile(classFilePath).getName());
    }

    @Test
    void testGetConstPoolItem() {
        String classFilePath = FileUtils.getResourceFilePath("FileCacheStore.jcls");
        ClassFile classFile = JavassistUtils.openClassFile(classFilePath);

        List<String> constPoolStringItems = JavassistUtils.getConstPoolStringItems(classFile.getConstPool());

        // COMMON_CACHE_MAX_FILE_SIZE_LIMIT_EXCEED = 0-4
        Assertions.assertTrue(constPoolStringItems.contains("0-4"));
    }
}
