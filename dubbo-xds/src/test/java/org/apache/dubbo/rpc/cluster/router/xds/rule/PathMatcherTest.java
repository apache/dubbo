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
package org.apache.dubbo.rpc.cluster.router.xds.rule;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PathMatcherTest {


    @Test
    public void pathMatcherTest() {
        PathMatcher pathMatcher = new PathMatcher();
        String path = "/testService/test";
        pathMatcher.setPath(path);
        assertTrue(pathMatcher.isMatch(path));
        assertTrue(pathMatcher.isMatch(path.toUpperCase()));
        pathMatcher.setCaseSensitive(true);
        assertFalse(pathMatcher.isMatch(path.toUpperCase()));

    }

    @Test
    public void prefixMatcherTest() {
        PathMatcher pathMatcher = new PathMatcher();
        String prefix = "/test";
        String path = "/testService/test";
        pathMatcher.setPrefix(prefix);
        assertTrue(pathMatcher.isMatch(path));
        assertTrue(pathMatcher.isMatch(path.toUpperCase()));
        pathMatcher.setCaseSensitive(true);
        assertFalse(pathMatcher.isMatch(path.toUpperCase()));
    }

    @Test
    public void regexMatcherTest() {
        PathMatcher pathMatcher = new PathMatcher();
        String regex = "/testService/.*";
        String path = "/testService/test";
        pathMatcher.setRegex(regex);
        assertTrue(pathMatcher.isMatch(path));
    }

}
