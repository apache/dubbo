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
package org.apache.dubbo.rpc.support;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TrieTreeTest {

    private TrieTree trie;

    @BeforeEach
    void setUp() {
        // Initialize the set of words before each test
        Set<String> words = new HashSet<>();
        words.add("apple");
        words.add("App-le");
        words.add("apply");
        words.add("app_le.juice");
        words.add("app-LE_juice");

        // Initialize TrieTree
        trie = new TrieTree(words);
    }

    @Test
    void testSearchValidWords() {
        // Test valid words
        assertTrue(trie.search("apple"));
        assertTrue(trie.search("App-LE"));
        assertTrue(trie.search("apply"));
        assertTrue(trie.search("app_le.juice"));
        assertTrue(trie.search("app-LE_juice"));
    }

    @Test
    void testSearchInvalidWords() {
        // Test invalid words
        assertFalse(trie.search("app"));
        // Invalid character test
        assertFalse(trie.search("app%le"));
    }
}
