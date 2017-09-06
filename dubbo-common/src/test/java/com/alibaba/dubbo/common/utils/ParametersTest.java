/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common.utils;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class ParametersTest extends TestCase {
    final String ServiceName = "com.alibaba.dubbo.rpc.service.GenericService";
    final String ServiceVersion = "1.0.15";
    final String LoadBalance = "lcr";

    public void testMap2Parameters() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("name", "com.alibaba.dubbo.rpc.service.GenericService");
        map.put("version", "1.0.15");
        map.put("lb", "lcr");
        map.put("max.active", "500");
        assertEquals(map.get("name"), ServiceName);
        assertEquals(map.get("version"), ServiceVersion);
        assertEquals(map.get("lb"), LoadBalance);
    }

    public void testString2Parameters() throws Exception {
        String qs = "name=com.alibaba.dubbo.rpc.service.GenericService&version=1.0.15&lb=lcr";
        Map<String, String> map = StringUtils.parseQueryString(qs);
        assertEquals(map.get("name"), ServiceName);
        assertEquals(map.get("version"), ServiceVersion);
        assertEquals(map.get("lb"), LoadBalance);
    }
}