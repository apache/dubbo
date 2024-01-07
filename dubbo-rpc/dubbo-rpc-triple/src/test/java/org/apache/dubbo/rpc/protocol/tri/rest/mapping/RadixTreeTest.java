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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping;

import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathExpression;
import org.apache.dubbo.rpc.protocol.tri.rest.util.PathUtils;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RadixTreeTest {

    @Test
    void match() {
        RadixTree<String> tree = new RadixTree<>();
        tree.addPath(PathExpression.parse("/a/*"), "abc");
        tree.addPath(PathExpression.parse("/a/{x}/d/e"), "acd");
        tree.addPath(PathExpression.parse("/a/{v:.*}/e"), "acd");
        List<RadixTree.Match<String>> match = tree.match("/a/b/d/e");
        Assertions.assertFalse(match.isEmpty());
    }

    @Test
    void match1() {
        RadixTree<String> tree = new RadixTree<>();
        tree.addPath(PathExpression.parse(PathUtils.normalize("")), "abc");
        List<RadixTree.Match<String>> match = tree.match(PathUtils.normalize(""));
        Assertions.assertFalse(match.isEmpty());
    }

    @Test
    void clear() {
        RadixTree<String> tree = new RadixTree<>();
        tree.addPath(PathExpression.parse("/a/*"), "abc");
        tree.addPath(PathExpression.parse("/a/{x}/d/e"), "acd");
        tree.addPath(PathExpression.parse("/a/{v:.*}/e"), "acd");
        tree.remove(s -> "abc".equals(s) || "acd".equals(s));
        Assertions.assertTrue(tree.isEmpty());
    }
}
