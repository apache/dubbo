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
package org.apache.dubbo.common.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_KEY;

/**
 *
 */
public class ConfigurationUtilsTest {

    @Test
    public void testGetServerShutdownTimeout () {
        System.setProperty(SHUTDOWN_WAIT_KEY, " 10000");
        Assertions.assertEquals(10000, ConfigurationUtils.getServerShutdownTimeout());
        System.clearProperty(SHUTDOWN_WAIT_KEY);
    }

    @Test
    public void testGetProperty () {
        System.setProperty(SHUTDOWN_WAIT_KEY, " 10000");
        Assertions.assertEquals("10000", ConfigurationUtils.getProperty(SHUTDOWN_WAIT_KEY));
        System.clearProperty(SHUTDOWN_WAIT_KEY);
    }

    @Test
    public void testParseSingleProperties() throws Exception {
        String p1 = "aaa=bbb";
        Map<String, String> result = ConfigurationUtils.parseProperties(p1);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("bbb", result.get("aaa"));
    }

    @Test
    public void testParseMultipleProperties() throws Exception {
        String p1 = "aaa=bbb\nccc=ddd";
        Map<String, String> result = ConfigurationUtils.parseProperties(p1);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("bbb", result.get("aaa"));
        Assertions.assertEquals("ddd", result.get("ccc"));
    }

    @Test
    public void testEscapedNewLine() throws Exception {
        String p1 = "dubbo.registry.address=zookeeper://127.0.0.1:2181\\\\ndubbo.protocol.port=20880";
        Map<String, String> result = ConfigurationUtils.parseProperties(p1);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("zookeeper://127.0.0.1:2181\\ndubbo.protocol.port=20880", result.get("dubbo.registry.address"));
    }
}
