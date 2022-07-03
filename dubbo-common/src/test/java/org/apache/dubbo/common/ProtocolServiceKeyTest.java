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

public class ProtocolServiceKeyTest {
    @Test
    public void test() {
        ProtocolServiceKey protocolServiceKey = new ProtocolServiceKey("DemoService", "1.0.0", "group1", "protocol1");
        Assertions.assertEquals("DemoService", protocolServiceKey.getInterfaceName());
        Assertions.assertEquals("1.0.0", protocolServiceKey.getVersion());
        Assertions.assertEquals("group1", protocolServiceKey.getGroup());
        Assertions.assertEquals("protocol1", protocolServiceKey.getProtocol());

        Assertions.assertEquals("group1/DemoService:1.0.0:protocol1", protocolServiceKey.toString());
        Assertions.assertEquals("group1/DemoService:1.0.0", protocolServiceKey.getServiceKeyString());

        Assertions.assertEquals(protocolServiceKey, protocolServiceKey);

        ProtocolServiceKey protocolServiceKey1 = new ProtocolServiceKey("DemoService", "1.0.0", "group1", "protocol1");
        Assertions.assertEquals(protocolServiceKey, protocolServiceKey1);
        Assertions.assertEquals(protocolServiceKey.hashCode(), protocolServiceKey1.hashCode());

        ProtocolServiceKey protocolServiceKey2 = new ProtocolServiceKey("DemoService", "1.0.0", "group1", "protocol2");
        Assertions.assertNotEquals(protocolServiceKey, protocolServiceKey2);

        ProtocolServiceKey protocolServiceKey3 = new ProtocolServiceKey("DemoService", "1.0.0", "group2", "protocol1");
        Assertions.assertNotEquals(protocolServiceKey, protocolServiceKey3);

        ProtocolServiceKey protocolServiceKey4 = new ProtocolServiceKey("DemoService", "1.0.1", "group1", "protocol1");
        Assertions.assertNotEquals(protocolServiceKey, protocolServiceKey4);

        ProtocolServiceKey protocolServiceKey5 = new ProtocolServiceKey("DemoInterface", "1.0.0", "group1", "protocol1");
        Assertions.assertNotEquals(protocolServiceKey, protocolServiceKey5);

        ServiceKey serviceKey = new ServiceKey("DemoService", "1.0.0", "group1");
        Assertions.assertNotEquals(protocolServiceKey, serviceKey);
    }
}
