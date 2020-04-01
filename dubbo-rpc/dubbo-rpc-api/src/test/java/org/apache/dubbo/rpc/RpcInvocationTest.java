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
package org.apache.dubbo.rpc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class RpcInvocationTest {

    @Test
    public void testAttachment() {
        RpcInvocation invocation = new RpcInvocation();

        invocation.setAttachment("objectKey1", "value1");
        invocation.setAttachment("objectKey2", "value2");
        invocation.setAttachment("objectKey3", 1); // object

        Assertions.assertEquals("value1", invocation.getObjectAttachment("objectKey1"));
        Assertions.assertEquals("value2", invocation.getAttachment("objectKey2"));
        Assertions.assertNull(invocation.getAttachment("objectKey3"));
        Assertions.assertEquals(1, invocation.getObjectAttachment("objectKey3"));
        Assertions.assertEquals(3, invocation.getObjectAttachments().size());

        HashMap<String, Object> map = new HashMap<>();
        map.put("mapKey1", 1);
        map.put("mapKey2", "mapValue2");
        invocation.setObjectAttachments(map);
        Assertions.assertEquals(map, invocation.getObjectAttachments());
    }
}
