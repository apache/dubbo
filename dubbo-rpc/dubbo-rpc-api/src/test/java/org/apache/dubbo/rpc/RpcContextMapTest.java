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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Map;

public class RpcContextMapTest {
    @BeforeEach
    void setUp() {
        RpcContext.removeContext();
    }

    @AfterEach
    void tearDown() {
        RpcContext.removeContext();
    }

    @Test
    public void testRpcContextMap() {
        Map<String, String> attachments = RpcContext.getContext().getAttachments();
        attachments.put("a", "b");
        attachments.put("c", "d");
        Assertions.assertEquals("b", RpcContext.getClientAttachment().getAttachment("a"));
        Assertions.assertEquals("d", RpcContext.getClientAttachment().getAttachment("c"));

        RpcContext.getClientAttachment().setAttachment("e", "f");
        RpcContext.getClientAttachment().setAttachment("g", "h");
        Assertions.assertEquals("f", attachments.get("e"));
        Assertions.assertEquals("h", attachments.get("g"));

        RpcContext.getServerAttachment().setAttachment("i", "j");
        RpcContext.getServerAttachment().setAttachment("k", "l");
        Assertions.assertEquals("j", attachments.get("i"));
        Assertions.assertEquals("l", attachments.get("k"));

        Iterator<Map.Entry<String, String>> iterator = attachments.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            if (entry.getKey().equals("a")) {
                Assertions.assertEquals("b", entry.getValue());
                iterator.remove();
            }
            if (entry.getKey().equals("k")) {
                Assertions.assertEquals("l", entry.getValue());
                entry.setValue("m");
            }
        }

        Assertions.assertNull(RpcContext.getClientAttachment().getAttachment("a"));
        Assertions.assertNull(RpcContext.getServerAttachment().getAttachment("a"));

        Assertions.assertEquals("m", RpcContext.getServerAttachment().getAttachment("k"));

        Assertions.assertEquals(5, attachments.size());
        Assertions.assertEquals(5, attachments.entrySet().size());

        Assertions.assertTrue(attachments.containsKey("c"));
        Assertions.assertTrue(attachments.containsKey("e"));
        Assertions.assertTrue(attachments.containsKey("g"));
        Assertions.assertTrue(attachments.containsKey("i"));
        Assertions.assertTrue(attachments.containsKey("k"));

        Assertions.assertTrue(attachments.containsValue("d"));
        Assertions.assertTrue(attachments.containsValue("f"));
        Assertions.assertTrue(attachments.containsValue("h"));
        Assertions.assertTrue(attachments.containsValue("j"));
        Assertions.assertTrue(attachments.containsValue("m"));

        Assertions.assertEquals(attachments, RpcContext.getContext().getAttachments());
        RpcContext.removeContext();
        Assertions.assertNotEquals(attachments, RpcContext.getContext().getAttachments());
    }

    @Test
    public void testRpcContextObjectMap() {
        Map<String, Object> attachments = RpcContext.getContext().getObjectAttachments();
        attachments.put("a", "b");
        attachments.put("c", "d");
        Assertions.assertEquals("b", RpcContext.getClientAttachment().getAttachment("a"));
        Assertions.assertEquals("d", RpcContext.getClientAttachment().getAttachment("c"));

        RpcContext.getClientAttachment().setAttachment("e", "f");
        RpcContext.getClientAttachment().setAttachment("g", "h");
        Assertions.assertEquals("f", attachments.get("e"));
        Assertions.assertEquals("h", attachments.get("g"));

        RpcContext.getServerAttachment().setAttachment("i", "j");
        RpcContext.getServerAttachment().setAttachment("k", "l");
        Assertions.assertEquals("j", attachments.get("i"));
        Assertions.assertEquals("l", attachments.get("k"));

        Iterator<Map.Entry<String, Object>> iterator = attachments.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            if (entry.getKey().equals("a")) {
                Assertions.assertEquals("b", entry.getValue());
                iterator.remove();
            }
            if (entry.getKey().equals("k")) {
                Assertions.assertEquals("l", entry.getValue());
                entry.setValue("m");
            }
        }

        Assertions.assertNull(RpcContext.getClientAttachment().getAttachment("a"));
        Assertions.assertNull(RpcContext.getServerAttachment().getAttachment("a"));

        Assertions.assertEquals("m", RpcContext.getServerAttachment().getAttachment("k"));

        Assertions.assertEquals(5, attachments.size());
        Assertions.assertEquals(5, attachments.entrySet().size());

        Assertions.assertTrue(attachments.containsKey("c"));
        Assertions.assertTrue(attachments.containsKey("e"));
        Assertions.assertTrue(attachments.containsKey("g"));
        Assertions.assertTrue(attachments.containsKey("i"));
        Assertions.assertTrue(attachments.containsKey("k"));

        Assertions.assertTrue(attachments.containsValue("d"));
        Assertions.assertTrue(attachments.containsValue("f"));
        Assertions.assertTrue(attachments.containsValue("h"));
        Assertions.assertTrue(attachments.containsValue("j"));
        Assertions.assertTrue(attachments.containsValue("m"));

        Assertions.assertEquals(attachments, RpcContext.getContext().getObjectAttachments());
        RpcContext.removeContext();
        Assertions.assertNotEquals(attachments, RpcContext.getContext().getObjectAttachments());
    }
}
