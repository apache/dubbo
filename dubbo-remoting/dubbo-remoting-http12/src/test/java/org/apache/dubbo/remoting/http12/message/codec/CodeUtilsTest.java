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
package org.apache.dubbo.remoting.http12.message.codec;

import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CodeUtilsTest {

    @Test
    void testDetermineHttpCodec() {
        HttpHeaders headers = new HttpHeaders();
        headers.put(
                HttpHeaderNames.CONTENT_TYPE.getName(),
                Collections.singletonList(MediaType.APPLICATION_JSON_VALUE.getName()));
        HttpMessageCodec codec =
                CodecUtils.determineHttpMessageCodec(FrameworkModel.defaultModel(), headers, null, true);
        Assertions.assertNotNull(codec);
        Assertions.assertEquals(JsonPbCodec.class, codec.getClass());

        // If no Accept header provided, use Content-Type to find encoder
        codec = CodecUtils.determineHttpMessageCodec(FrameworkModel.defaultModel(), headers, null, false);
        Assertions.assertNotNull(codec);
        Assertions.assertEquals(JsonPbCodec.class, codec.getClass());

        HttpHeaders headers1 = new HttpHeaders();
        headers1.put(
                HttpHeaderNames.CONTENT_TYPE.getName(),
                Collections.singletonList(MediaType.MULTIPART_FORM_DATA.getName()));
        codec = CodecUtils.determineHttpMessageCodec(FrameworkModel.defaultModel(), headers1, null, true);
        Assertions.assertNotNull(codec);
        Assertions.assertEquals(MultipartCodec.class, codec.getClass());
        codec = CodecUtils.determineHttpMessageCodec(FrameworkModel.defaultModel(), headers1, null, false);
        Assertions.assertNull(codec);

        headers1.put(
                HttpHeaderNames.ACCEPT.getName(),
                Collections.singletonList(MediaType.APPLICATION_JSON_VALUE.getName()));
        codec = CodecUtils.determineHttpMessageCodec(FrameworkModel.defaultModel(), headers1, null, false);
        Assertions.assertNotNull(codec);
        Assertions.assertEquals(JsonPbCodec.class, codec.getClass());
    }
}
