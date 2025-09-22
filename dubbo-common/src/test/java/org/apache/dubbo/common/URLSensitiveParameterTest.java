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
package org.apache.dubbo.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class URLSensitiveParameterTest {
    @BeforeEach
    void setUp() {
        System.clearProperty("dubbo.url.sensitive-parameter-names");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("dubbo.url.sensitive-parameter-names");
    }

    @Test
    void testDefaultSensitiveParameters() {
        URL url = URL.valueOf("nacos://127.0.0.1:8848/registry?password=secret&secretKey=mysecret&timeout=5000");
        String urlString = url.toString();
        assertFalse(urlString.contains("password=secret"));
        assertFalse(urlString.contains("secretKey=mysecret"));
        assertTrue(urlString.contains("timeout=5000"));
    }

    @Test
    void testCustomSensitiveParameter() {
        System.setProperty("dubbo.url.sensitive-parameter-names", "customToken");
        URL url = URL.valueOf("nacos://127.0.0.1:8848/registry?password=secret&customToken=abc123&timeout=5000");
        String urlString = url.toString();
        assertFalse(urlString.contains("password=secret")); // 默认敏感词依然生效
        assertFalse(urlString.contains("customToken=abc123")); // 自定义敏感词生效
        assertTrue(urlString.contains("timeout=5000"));
    }

    @Test
    void testNonSensitiveParameter() {
        URL url = URL.valueOf("nacos://127.0.0.1:8848/registry");
        assertFalse(org.apache.dubbo.common.config.ConfigurationUtils.isSensitiveParameter(url, "timeout"));
    }
}
