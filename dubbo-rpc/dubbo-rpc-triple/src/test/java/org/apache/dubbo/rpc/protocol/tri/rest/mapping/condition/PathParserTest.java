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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition;

import org.apache.dubbo.rpc.protocol.tri.rest.mapping.PathUtils;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PathParserTest {

    @Test
    void parse1() {
        List<PathSegment> parse = PathParser.parse("/{asd}/b/{vv}/v");
        Assertions.assertEquals(parse.size(), 4);
    }

    @Test
    void parse2() {
        System.out.println(PathUtils.normalize("/../a/b"));
        System.out.println(PathUtils.normalize("../a/b"));
        System.out.println(PathUtils.normalize("./a/b"));
        System.out.println(PathUtils.normalize("/a/b"));
        System.out.println(PathUtils.normalize("a/b"));
        System.out.println(PathUtils.normalize("a/../b"));
        System.out.println(PathUtils.normalize("a/../../b"));
        System.out.println(PathUtils.normalize("a/../../../b"));
        System.out.println(PathUtils.normalize("  a/../../../b   \r"));
    }

    @Test
    void parse3() {
        System.out.println(PathUtils.normalize("/a/b/c"));
    }
}
