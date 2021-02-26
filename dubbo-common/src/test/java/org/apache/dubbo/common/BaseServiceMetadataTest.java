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

import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BaseServiceMetadataTest {

    @Test
    public void test() {
        BaseServiceMetadata baseServiceMetadata = new BaseServiceMetadata();
        baseServiceMetadata.setGroup("group1");
        baseServiceMetadata.setServiceInterfaceName("org.apache.dubbo.common.TestInterface");
        baseServiceMetadata.setVersion("1.0.0");
        baseServiceMetadata.setServiceKey(BaseServiceMetadata.buildServiceKey("org.apache.dubbo.common.TestInterface", "group1", "1.0.0"));

        assertEquals(baseServiceMetadata.getGroup(), "group1");
        assertEquals(baseServiceMetadata.getServiceInterfaceName(), "org.apache.dubbo.common.TestInterface");
        assertEquals(baseServiceMetadata.getVersion(), "1.0.0");
        assertEquals(baseServiceMetadata.getServiceKey(), "group1/org.apache.dubbo.common.TestInterface:1.0.0");
        assertEquals(baseServiceMetadata.getDisplayServiceKey(), "org.apache.dubbo.common.TestInterface:1.0.0");

        baseServiceMetadata.setServiceKey(BaseServiceMetadata.buildServiceKey("org.apache.dubbo.common.TestInterface", null, null));
        assertEquals(baseServiceMetadata.getServiceKey(), "org.apache.dubbo.common.TestInterface");
        baseServiceMetadata.setServiceKey(BaseServiceMetadata.buildServiceKey("org.apache.dubbo.common.TestInterface", "", ""));
        assertEquals(baseServiceMetadata.getServiceKey(), "org.apache.dubbo.common.TestInterface");


        baseServiceMetadata.setVersion("2.0.0");
        baseServiceMetadata.generateServiceKey();
        assertEquals(baseServiceMetadata.getServiceKey(), "group1/org.apache.dubbo.common.TestInterface:2.0.0");

        assertEquals(BaseServiceMetadata.versionFromServiceKey("group1/org.apache.dubbo.common.TestInterface:1.0.0"), "1.0.0");
        assertEquals(BaseServiceMetadata.groupFromServiceKey("group1/org.apache.dubbo.common.TestInterface:1.0.0"), "group1");
        assertEquals(BaseServiceMetadata.interfaceFromServiceKey("group1/org.apache.dubbo.common.TestInterface:1.0.0"), "org.apache.dubbo.common.TestInterface");

        assertEquals(DEFAULT_VERSION, BaseServiceMetadata.versionFromServiceKey(""));
        assertNull(BaseServiceMetadata.groupFromServiceKey(""));
        assertEquals(BaseServiceMetadata.interfaceFromServiceKey(""), "");

        assertEquals(BaseServiceMetadata.revertDisplayServiceKey("org.apache.dubbo.common.TestInterface:1.0.0").getDisplayServiceKey(),
                "org.apache.dubbo.common.TestInterface:1.0.0");
        assertEquals(BaseServiceMetadata.revertDisplayServiceKey("org.apache.dubbo.common.TestInterface").getDisplayServiceKey(),
                "org.apache.dubbo.common.TestInterface:null");
        assertEquals(BaseServiceMetadata.revertDisplayServiceKey(null).getDisplayServiceKey(),"null:null");
        assertEquals(BaseServiceMetadata.revertDisplayServiceKey("org.apache.dubbo.common.TestInterface:1.0.0:1").getDisplayServiceKey(),"null:null");
    }
}
