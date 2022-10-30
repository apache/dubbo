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
package org.apache.dubbo.common.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PathUtilsTest {
    @Test
    public void buildPathTest() {
        String rootPath = PathUtils.buildPath("/dubbo/config");
        Assertions.assertEquals(rootPath, "/dubbo/config");

        String rootPathAndSubPath = PathUtils.buildPath("/dubbo/config", "A", "B");
        Assertions.assertEquals(rootPathAndSubPath, "/dubbo/config/A/B");

        String filterEmptySubPath = PathUtils.buildPath("/dubbo/config", null, "B");
        Assertions.assertEquals(filterEmptySubPath, "/dubbo/config/B");
    }

    @Test
    public void normalizeTest() {
        String emptyPath = PathUtils.normalize("");
        Assertions.assertEquals(emptyPath, "/");

        String removeQuestionMask = PathUtils.normalize("/A/B?k1=v1");
        Assertions.assertEquals(removeQuestionMask, "/A/B");

        String multiSlash = PathUtils.normalize("/A/B//C//D");
        Assertions.assertEquals(multiSlash, "/A/B/C/D");
    }
}
