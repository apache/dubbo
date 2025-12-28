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

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RpcContextCompatibilityTest {

    @Test
    void testAttachmentCompatibility() {
        RpcContext context = RpcContext.getServiceContext();

        String key = "test_long_key";
        Long value = 12345L;
        context.setObjectAttachment(key, value);

        Assertions.assertEquals(value, context.getObjectAttachment(key));

        Object legacyValue = context.getAttachment(key);
        System.out.println("Legacy getAttachment result: " + legacyValue);

        Map<String, String> allAttachments = context.getAttachments();

        try {
            for (Map.Entry<String, String> entry : allAttachments.entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue(); // 如果 entry.getValue() 实际是 Long，这里可能会崩
                System.out.println(k + " = " + v);
            }
        } catch (ClassCastException e) {
            System.err.println("发现不兼容 Bug！由于底层存了非 String 对象，导致老接口遍历崩溃: " + e.getMessage());
        }
    }
}
