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
package org.apache.dubbo.rpc.protocol.rest;

import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.rpc.protocol.rest.message.HttpMessageCodecManager;
import org.apache.dubbo.rpc.protocol.rest.message.codec.XMLCodec;
import org.apache.dubbo.rpc.protocol.rest.pair.MessageCodecResultPair;
import org.apache.dubbo.rpc.protocol.rest.rest.RegistrationResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

public class HttpMessageCodecManagerTest {

    @Test
    void testCodec() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        RegistrationResult registrationResult = new RegistrationResult();
        registrationResult.setId(1l);
        HttpMessageCodecManager.httpMessageEncode(byteArrayOutputStream,
            registrationResult, null, MediaType.TEXT_XML, null);

        Object o = HttpMessageCodecManager.httpMessageDecode(byteArrayOutputStream.toByteArray(), RegistrationResult.class, MediaType.TEXT_XML);

        Assertions.assertEquals(registrationResult, o);

        byteArrayOutputStream = new ByteArrayOutputStream();
        MessageCodecResultPair messageCodecResultPair = HttpMessageCodecManager.httpMessageEncode(byteArrayOutputStream, null, null, null, RegistrationResult.class);

        MediaType mediaType = messageCodecResultPair.getMediaType();

        Assertions.assertEquals(MediaType.APPLICATION_JSON_VALUE, mediaType);

        XMLCodec xmlCodec = new XMLCodec();

        Assertions.assertEquals(false, xmlCodec.typeSupport(null));


    }
}
