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
import org.apache.dubbo.remoting.http12.message.HttpMessageDecoder;
import org.apache.dubbo.remoting.http12.message.HttpMessageEncoder;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CodeUtilsTest {

    @Test
    void testDetermineHttpCodec() {
        CodecUtils codecUtils = new CodecUtils(FrameworkModel.defaultModel());
        HttpHeaders headers = HttpHeaders.create();
        headers.set(HttpHeaderNames.CONTENT_TYPE.getKey(), MediaType.APPLICATION_JSON.getName());
        HttpMessageDecoder decoder =
                codecUtils.determineHttpMessageDecoder(null, headers.getFirst(HttpHeaderNames.CONTENT_TYPE.getKey()));
        Assertions.assertNotNull(decoder);
        Assertions.assertEquals(JsonPbCodec.class, decoder.getClass());

        HttpMessageEncoder encoder;
        // If no Accept header provided, use Content-Type to find encoder
        encoder = codecUtils.determineHttpMessageEncoder(null, MediaType.APPLICATION_JSON.getName());
        Assertions.assertNotNull(encoder);
        Assertions.assertEquals(JsonPbCodec.class, encoder.getClass());

        encoder = codecUtils.determineHttpMessageEncoder(null, MediaType.APPLICATION_JSON.getName());
        Assertions.assertNotNull(encoder);
        Assertions.assertEquals(JsonPbCodec.class, encoder.getClass());
    }
}
