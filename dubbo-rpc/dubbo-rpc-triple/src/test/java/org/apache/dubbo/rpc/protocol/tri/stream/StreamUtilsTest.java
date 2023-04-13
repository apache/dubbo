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

package org.apache.dubbo.rpc.protocol.tri.stream;

import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;

import io.netty.handler.codec.http2.DefaultHttp2Headers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class StreamUtilsTest {


    @Test
    void encodeBase64ASCII() {
        String content = "😯";
        Assertions.assertArrayEquals(content.getBytes(StandardCharsets.UTF_8),
            StreamUtils.decodeASCIIByte(StreamUtils.encodeBase64ASCII(content.getBytes(
                StandardCharsets.UTF_8))));
    }

    @Test
    void testConvertAttachment() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        DefaultHttp2Headers headers = new DefaultHttp2Headers();
        headers.add("key", "value");

        Map<String, Object> attachments = new HashMap<>();
        attachments.put(TripleHeaderEnum.PATH_KEY.getHeader(), "value");
        attachments.put("key1111", "value");
        attachments.put("Upper", "Upper");
        attachments.put("obj", new Object());

        StreamUtils.convertAttachment(headers, attachments, false);
        Assertions.assertNull(headers.get(TripleHeaderEnum.PATH_KEY.getHeader()));
        Assertions.assertNull(headers.get("Upper"));
        Assertions.assertNull(headers.get("obj"));

        headers = new DefaultHttp2Headers();
        headers.add("key", "value");

        StreamUtils.convertAttachment(headers, attachments, true);
        Assertions.assertNull(headers.get(TripleHeaderEnum.PATH_KEY.getHeader()));
        Assertions.assertNull(headers.get("Upper"));
        Assertions.assertNull(headers.get("obj"));
        String jsonRaw = headers.get(TripleHeaderEnum.TRI_HEADER_CONVERT.getHeader()).toString();
        String json = TriRpcStatus.decodeMessage(jsonRaw);
        System.out.println(jsonRaw + "---" + json);
        Map<String, String> upperMap = JsonUtils.toJavaObject(json, Map.class);
        Assertions.assertArrayEquals("Upper".getBytes(StandardCharsets.UTF_8), upperMap.get("upper").getBytes(StandardCharsets.UTF_8));

        int count = 10000;
        CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            String randomKey = "key" + i;
            String randomValue = "value" + i;
            Map<String, Object> attachments2 = new HashMap<>();
            attachments2.put(TripleHeaderEnum.PATH_KEY.getHeader(), "value");
            attachments2.put("key1111", "value");
            attachments2.put("Upper", "Upper");
            attachments2.put("obj", new Object());
            attachments2.put(randomKey, randomValue);
            executorService.execute(() -> {
                DefaultHttp2Headers headers2 = new DefaultHttp2Headers();
                headers2.add("key", "value");
                StreamUtils.convertAttachment(headers2, attachments2, true);

                if (headers2.get(TripleHeaderEnum.PATH_KEY.getHeader()) != null) {
                    return;
                }
                if (headers2.get("Upper") != null) {
                    return;
                }
                if (headers2.get("obj") != null) {
                    return;
                }
                if (!headers2.get(randomKey).toString().equals(randomValue)) {
                    return;
                }
                latch.countDown();
            });
        }
        latch.await(10, TimeUnit.SECONDS);
        Assertions.assertEquals(0, latch.getCount());
        executorService.shutdown();
    }


}
