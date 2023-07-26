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
package org.apache.dubbo.remoting.utils;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Constants;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UrlUtilsTest {
    @Test
    void testGetIdleTimeout() {
        URL url1 = URL.valueOf("dubbo://127.0.0.1:12345?heartbeat=10000");
        URL url2 = URL.valueOf("dubbo://127.0.0.1:12345?heartbeat=10000&heartbeat.timeout=50000");
        URL url3 = URL.valueOf("dubbo://127.0.0.1:12345?heartbeat=10000&heartbeat.timeout=10000");
        Assertions.assertEquals(UrlUtils.getIdleTimeout(url1), 30000);
        Assertions.assertEquals(UrlUtils.getIdleTimeout(url2), 50000);
        Assertions.assertThrows(RuntimeException.class, () -> UrlUtils.getIdleTimeout(url3));
    }

    @Test
    void testGetHeartbeat() {
        URL url = URL.valueOf("dubbo://127.0.0.1:12345?heartbeat=10000");
        Assertions.assertEquals(UrlUtils.getHeartbeat(url), 10000);
    }

    @Test
    void testConfiguredHeartbeat() {
        System.setProperty(Constants.HEARTBEAT_CONFIG_KEY, "200");
        URL url = URL.valueOf("dubbo://127.0.0.1:12345");
        Assertions.assertEquals(200, UrlUtils.getHeartbeat(url));
        System.clearProperty(Constants.HEARTBEAT_CONFIG_KEY);
    }

    @Test
    void testGetCloseTimeout() {
        URL url1 = URL.valueOf("dubbo://127.0.0.1:12345?heartbeat=10000");
        URL url2 = URL.valueOf("dubbo://127.0.0.1:12345?heartbeat=10000&heartbeat.timeout=50000");
        URL url3 = URL.valueOf("dubbo://127.0.0.1:12345?heartbeat=10000&heartbeat.timeout=10000");
        URL url4 = URL.valueOf("dubbo://127.0.0.1:12345?close.timeout=30000&heartbeat=10000&heartbeat.timeout=10000");
        URL url5 = URL.valueOf("dubbo://127.0.0.1:12345?close.timeout=40000&heartbeat=10000&heartbeat.timeout=50000");
        URL url6 = URL.valueOf("dubbo://127.0.0.1:12345?close.timeout=10000&heartbeat=10000&heartbeat.timeout=10000");
        Assertions.assertEquals(30000, UrlUtils.getCloseTimeout(url1));
        Assertions.assertEquals(50000, UrlUtils.getCloseTimeout(url2));
        Assertions.assertThrows(RuntimeException.class, () -> UrlUtils.getCloseTimeout(url3));
        Assertions.assertThrows(RuntimeException.class, () -> UrlUtils.getCloseTimeout(url4));
        Assertions.assertEquals(40000, UrlUtils.getCloseTimeout(url5));
        Assertions.assertThrows(RuntimeException.class, () -> UrlUtils.getCloseTimeout(url6));
    }

    @Test
    void testConfiguredClose() {
        System.setProperty(Constants.CLOSE_TIMEOUT_CONFIG_KEY, "180000");
        URL url = URL.valueOf("dubbo://127.0.0.1:12345");
        Assertions.assertEquals(180000, UrlUtils.getCloseTimeout(url));
        System.clearProperty(Constants.HEARTBEAT_CONFIG_KEY);
    }
}
