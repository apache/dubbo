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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RpcContextCompatibilityTest {

    @AfterEach
    void tearDown() {
        RpcContext.removeServiceContext();
        RpcContext.removeClientAttachment();
        RpcContext.removeServerAttachment();
    }

    @Test
    void testLegacyGetAttachmentRendersObjectAttachment() {
        RpcContext context = RpcContext.getServiceContext();
        context.setObjectAttachment("test_long_key", 12345L);
        context.setObjectAttachment("test_string_key", "text");

        // the object-based API still returns the original value with its original type
        Assertions.assertEquals(12345L, context.getObjectAttachment("test_long_key"));

        // the legacy String-based API renders it instead of returning null
        Assertions.assertEquals("12345", context.getAttachment("test_long_key"));
        Assertions.assertEquals("text", context.getAttachment("test_string_key"));
        Assertions.assertNull(context.getAttachment("absent_key"));
    }

    @Test
    void testLegacyGetAttachmentKeepsClientPrecedence() {
        RpcContext.getServerAttachment().setObjectAttachment("key", "server_value");
        RpcContext.getClientAttachment().setObjectAttachment("key", 42);

        // before the fix the non-String client value was skipped and the server value leaked through
        Assertions.assertEquals("42", RpcContext.getContext().getAttachment("key"));
    }

    @Test
    void testLegacyGetAttachmentFallsBackToServerAttachment() {
        RpcContext.getServerAttachment().setObjectAttachment("key", 456);

        Assertions.assertEquals("456", RpcContext.getContext().getAttachment("key"));
        Assertions.assertEquals("456", RpcContext.getServiceContext().getAttachment("key"));
    }

    @Test
    void testTypedAttachmentAccessorsAreUnchanged() {
        // RpcContextAttachment keeps the strict String-only contract on purpose
        RpcContext.getClientAttachment().setObjectAttachment("key", 1);

        Assertions.assertNull(RpcContext.getClientAttachment().getAttachment("key"));
        Assertions.assertEquals(1, RpcContext.getClientAttachment().getObjectAttachment("key"));
    }

    @Test
    void testLegacyAttachmentsMapIsStillIterable() {
        RpcContext context = RpcContext.getServiceContext();
        context.setObjectAttachment("test_long_key", 12345L);

        Map<String, String> attachments = context.getAttachments();

        Assertions.assertDoesNotThrow(() -> {
            for (Map.Entry<String, String> entry : attachments.entrySet()) {
                Assertions.assertNotNull(entry.getKey());
                Assertions.assertNotNull(entry.getValue());
            }
        });
    }
}
