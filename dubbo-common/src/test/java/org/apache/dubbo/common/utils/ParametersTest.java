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
package org.apache.dubbo.common.utils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParametersTest {
    final String ServiceName = "org.apache.dubbo.rpc.service.GenericService";
    final String ServiceVersion = "1.0.15";
    final String LoadBalance = "lcr";

    public void testMap2Parameters() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("name", "org.apache.dubbo.rpc.service.GenericService");
        map.put("version", "1.0.15");
        map.put("lb", "lcr");
        map.put("max.active", "500");
        assertEquals(map.get("name"), ServiceName);
        assertEquals(map.get("version"), ServiceVersion);
        assertEquals(map.get("lb"), LoadBalance);
    }

    public void testString2Parameters() throws Exception {
        String qs = "name=org.apache.dubbo.rpc.service.GenericService&version=1.0.15&lb=lcr";
        Map<String, String> map = StringUtils.parseQueryString(qs);
        assertEquals(map.get("name"), ServiceName);
        assertEquals(map.get("version"), ServiceVersion);
        assertEquals(map.get("lb"), LoadBalance);
    }
}