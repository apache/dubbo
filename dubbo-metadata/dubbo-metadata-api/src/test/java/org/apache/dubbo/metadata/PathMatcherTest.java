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
package org.apache.dubbo.metadata;

import org.apache.dubbo.metadata.rest.PathMatcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PathMatcherTest {

    @Test
    void testPathMatcher() {
        PathMatcher pathMatherMeta = new PathMatcher("/a/b/c/{path1}/d/{path2}/e");


        PathMatcher requestPathMather = new PathMatcher("/a/b/c/1/d/2/e");
        Assertions.assertEquals(requestPathMather, pathMatherMeta);

        PathMatcher requestPathMather1 = new PathMatcher("/{c}/b/c/1/d/2/e");
        Assertions.assertEquals(requestPathMather, requestPathMather1);

        PathMatcher pathMatcher = new PathMatcher("/{d}/b/c/1/d/2/e");

        pathMatcher.setGroup(null);
        pathMatcher.setPort(null);
        pathMatcher.setVersion(null);
        pathMatcher.setContextPath("");

        Assertions.assertEquals(pathMatherMeta, pathMatcher);
    }

    @Test
    void testEqual() {
        PathMatcher pathMatherMeta = new PathMatcher("/a/b/c");
        pathMatherMeta.setContextPath("/context");
        PathMatcher pathMatherMeta1 = new PathMatcher("/a/b/d");

        pathMatherMeta1.setContextPath("/context");
        Assertions.assertNotEquals(pathMatherMeta, pathMatherMeta1);

        pathMatherMeta1 = new PathMatcher("/a/b/c");
        pathMatherMeta1.setContextPath("/context");

        Assertions.assertEquals(pathMatherMeta, pathMatherMeta1);

        pathMatherMeta.setContextPath("context");

        pathMatherMeta1.setContextPath("context");


        Assertions.assertEquals(pathMatherMeta, pathMatherMeta1);
        Assertions.assertEquals(pathMatherMeta.toString(), pathMatherMeta1.toString());
    }
}
