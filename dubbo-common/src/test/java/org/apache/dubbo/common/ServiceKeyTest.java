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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ServiceKeyTest {
    @Test
    void test() {
        ServiceKey serviceKey = new ServiceKey("DemoService", "1.0.0", "group1");

        Assertions.assertEquals("DemoService", serviceKey.getInterfaceName());
        Assertions.assertEquals("1.0.0", serviceKey.getVersion());
        Assertions.assertEquals("group1", serviceKey.getGroup());

        Assertions.assertEquals("group1/DemoService:1.0.0", serviceKey.toString());
        Assertions.assertEquals("DemoService", new ServiceKey("DemoService", null, null).toString());
        Assertions.assertEquals("DemoService:1.0.0", new ServiceKey("DemoService", "1.0.0", null).toString());
        Assertions.assertEquals("group1/DemoService", new ServiceKey("DemoService", null, "group1").toString());

        Assertions.assertEquals(serviceKey, serviceKey);

        ServiceKey serviceKey1 = new ServiceKey("DemoService", "1.0.0", "group1");
        Assertions.assertEquals(serviceKey, serviceKey1);
        Assertions.assertEquals(serviceKey.hashCode(), serviceKey1.hashCode());

        ServiceKey serviceKey2 = new ServiceKey("DemoService", "1.0.0", "group2");
        Assertions.assertNotEquals(serviceKey, serviceKey2);

        ServiceKey serviceKey3 = new ServiceKey("DemoService", "1.0.1", "group1");
        Assertions.assertNotEquals(serviceKey, serviceKey3);

        ServiceKey serviceKey4 = new ServiceKey("DemoInterface", "1.0.0", "group1");
        Assertions.assertNotEquals(serviceKey, serviceKey4);

        ProtocolServiceKey protocolServiceKey = new ProtocolServiceKey("DemoService", "1.0.0", "group1", "protocol1");
        Assertions.assertNotEquals(serviceKey, protocolServiceKey);
    }
}