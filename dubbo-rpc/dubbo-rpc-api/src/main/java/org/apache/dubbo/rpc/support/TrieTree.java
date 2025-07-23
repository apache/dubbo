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

import java.util.Set;

class TrieNode {
    TrieNode[] children;
    boolean isEndOfWord = false;

    // Constructor: Initializes children array
    public TrieNode() {
        this.children = new TrieNode[29]; // 0-25: 'a' - 'z', 26: '-', 27: '_', 28: '.'
    }
}

public class TrieTree {
    private final TrieNode root;

    // Constructor: Initializes the Trie and inserts all words from the given set
    public TrieTree(Set<String> words) {
        root = new TrieNode();
        for (String word : words) {
            insert(word);
        }
    }

    // Inserts a word into the Trie, case-insensitive
    private void insert(String word) {
        TrieNode node = root;
        for (char ch : word.toCharArray()) {
            int index = getCharIndex(ch);
            if (index == -1) {
                return; // Invalid character, skip this word
            }

            if (node.children[index] == null) {
                node.children[index] = new TrieNode();
            }
            node = node.children[index];
        }
        node.isEndOfWord = true;
    }

    // Checks if a word exists in the Trie, case-insensitive
    public boolean search(String word) {
        TrieNode node = root;
        for (char ch : word.toCharArray()) {
            int index = getCharIndex(ch);
            if (index == -1 || node.children[index] == null) {
                return false; // Invalid character or node doesn't exist
            }
            node = node.children[index];
        }
        return node.isEndOfWord;
    }

    // Maps the character to the array index, handling case-insensitivity
    // 'a-z' -> 0-25, '-' -> 26, '_' -> 27, '.' -> 28
    // Returns -1 if the character is invalid
    private int getCharIndex(char ch) {
        // Convert uppercase to lowercase within this function
        if (ch >= 'A' && ch <= 'Z') {
            ch = (char) (ch + 32); // Convert 'A'-'Z' to 'a'-'z'
        }
        if (ch >= 'a' && ch <= 'z') {
            return ch - 'a'; // 'a' -> 0, 'b' -> 1, ..., 'z' -> 25
        } else if (ch == '-') {
            return 26;
        } else if (ch == '_') {
            return 27;
        } else if (ch == '.') {
            return 28;
        } else {
            return -1; // Invalid character
        }
    }
}
