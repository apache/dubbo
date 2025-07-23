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

import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import com.google.common.base.Charsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CodecTest {

    CodecUtils codecUtils;

    @BeforeEach
    void beforeAll() {
        codecUtils = FrameworkModel.defaultModel().getBeanFactory().getOrRegisterBean(CodecUtils.class);
    }

    @Test
    void testXml() {
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<user><location>New York</location><username>JohnDoe</username></user>";
        InputStream in = new ByteArrayInputStream(content.getBytes());
        HttpMessageCodec codec = new XmlCodecFactory().createCodec(null, FrameworkModel.defaultModel(), null);
        User user = (User) codec.decode(in, User.class);
        Assertions.assertEquals("JohnDoe", user.getUsername());
        Assertions.assertEquals("New York", user.getLocation());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        codec.encode(outputStream, user);
        String res = outputStream.toString();
        Assertions.assertEquals(content, res);
    }

    @Test
    void testPlainText() {
        byte[] asciiBytes = new byte[] {
            0x48, 0x65, 0x6C, 0x6C,
            0x6F, 0x2C, 0x20, 0x77,
            0x6F, 0x72, 0x6C, 0x64
        };
        byte[] utf8Bytes = new byte[] {
            (byte) 0xE4, (byte) 0xBD, (byte) 0xA0,
            (byte) 0xE5, (byte) 0xA5, (byte) 0xBD,
            (byte) 0xEF, (byte) 0xBC, (byte) 0x8C,
            (byte) 0xE4, (byte) 0xB8, (byte) 0x96,
            (byte) 0xE7, (byte) 0x95, (byte) 0x8C
        };
        byte[] utf16Bytes = new byte[] {0x4F, 0x60, 0x59, 0x7D, (byte) 0xFF, 0x0C, 0x4E, 0x16, 0x75, 0x4C};
        InputStream in = new ByteArrayInputStream(asciiBytes);
        HttpMessageCodec codec = new PlainTextCodecFactory()
                .createCodec(null, FrameworkModel.defaultModel(), "text/plain; charset=ASCII");
        String res = (String) codec.decode(in, String.class);
        Assertions.assertEquals("Hello, world", res);

        in = new ByteArrayInputStream(utf8Bytes);
        codec = PlainTextCodec.INSTANCE;
        res = (String) codec.decode(in, String.class, Charsets.UTF_8);
        Assertions.assertEquals("你好，世界", res);

        in = new ByteArrayInputStream(utf16Bytes);
        res = (String) codec.decode(in, String.class, Charsets.UTF_16);
        Assertions.assertEquals("你好，世界", res);
    }
}
