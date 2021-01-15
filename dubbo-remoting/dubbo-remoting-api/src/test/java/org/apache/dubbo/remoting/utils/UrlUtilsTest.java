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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UrlUtilsTest {
    @Test
    public void testGetIdleTimeout() {
        URL url1 = URL.valueOf("dubbo://127.0.0.1:12345?heartbeat=10000");
        URL url2 = URL.valueOf("dubbo://127.0.0.1:12345?heartbeat=10000&heartbeat.timeout=50000");
        URL url3 = URL.valueOf("dubbo://127.0.0.1:12345?heartbeat=10000&heartbeat.timeout=10000");
        Assertions.assertEquals(UrlUtils.getIdleTimeout(url1), 30000);
        Assertions.assertEquals(UrlUtils.getIdleTimeout(url2), 50000);
        Assertions.assertThrows(RuntimeException.class, () -> UrlUtils.getIdleTimeout(url3));
    }

    @Test
    public void testGetHeartbeat() {
        URL url = URL.valueOf("dubbo://127.0.0.1:12345?heartbeat=10000");
        Assertions.assertEquals(UrlUtils.getHeartbeat(url), 10000);
    }
}
