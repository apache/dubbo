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

import org.apache.dubbo.common.convert.ConverterUtil;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.google.common.base.Charsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestCodecs {

    @Test
    void testMultipartForm() {
        InputStream in = new ByteArrayInputStream(
                ("--example-part-boundary\r\n" + "Content-Disposition: form-data; name=\"username\"\r\n"
                                + "Content-Type: text/plain\r\n"
                                + "\r\n"
                                + "LuYue\r\n"
                                + "--example-part-boundary\r\n"
                                + "Content-Disposition: form-data; name=\"userdetail\"\r\n"
                                + "Content-Type: application/json\r\n"
                                + "\r\n"
                                + "{\"location\":\"beijing\",\"username\":\"LuYue\"}\r\n"
                                + "--example-part-boundary\r\n"
                                + "Content-Disposition: form-data; name=\"userimg\"; filename=\"user.jpeg\"\r\n"
                                + "Content-Type: image/jpeg\r\n"
                                + "\r\n"
                                + "<binary-image data>\r\n"
                                + "--example-part-boundary--\r\n")
                        .getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaderNames.CONTENT_TYPE.getName(), "multipart/form-data; boundary=example-part-boundary");
        HttpMessageCodec codec = new MultipartCodec(null, FrameworkModel.defaultModel(), headers);
        Object[] result = codec.decode(in, new Class[] {String.class, User.class, byte[].class});
        Assertions.assertEquals("LuYue", result[0]);
        Assertions.assertTrue(result[1] instanceof User);
        Assertions.assertEquals("LuYue", ((User) result[1]).getUsername());
        Assertions.assertEquals("beijing", ((User) result[1]).getLocation());
        Assertions.assertEquals("<binary-image data>", new String((byte[]) result[2], Charsets.UTF_8));
    }

    @Test
    void testUrlForm() {
        InputStream in = new ByteArrayInputStream("Hello=World&Apache=Dubbo&id=10086".getBytes());
        HttpMessageCodec codec = new UrlEncodeFormCodec(
                FrameworkModel.defaultModel().getBeanFactory().getBean(ConverterUtil.class));
        Object res = codec.decode(in, Map.class);
        Assertions.assertTrue(res instanceof Map);
        Map<String, String> r = (Map<String, String>) res;
        Assertions.assertEquals("World", r.get("Hello"));
        Assertions.assertEquals("Dubbo", r.get("Apache"));
        Assertions.assertEquals("10086", r.get("id"));
        try {
            in.reset();
        } catch (IOException e) {
        }
        Object[] res2 = codec.decode(in, new Class[] {String.class, String.class, Long.class});
        Assertions.assertEquals("World", res2[0]);
        Assertions.assertEquals("Dubbo", res2[1]);
        Assertions.assertEquals(10086L, res2[2]);
    }

    @Test
    void testXml() {
        InputStream in = new ByteArrayInputStream(("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<user><location>New York</location><username>JohnDoe</username></user>")
                .getBytes());
        HttpMessageCodec codec = new XmlCodec();
        User user = (User) codec.decode(in, User.class);
        Assertions.assertEquals("JohnDoe", user.getUsername());
        Assertions.assertEquals("New York", user.getLocation());
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
        HttpMessageCodec codec = new PlainTextCodec("text/plain; charset=ASCII");
        String res = (String) codec.decode(in, String.class);
        Assertions.assertEquals("Hello, world", res);

        in = new ByteArrayInputStream(utf8Bytes);
        codec = new PlainTextCodec("text/plain; charset=UTF-8");
        res = (String) codec.decode(in, String.class);
        Assertions.assertEquals("你好，世界", res);

        in = new ByteArrayInputStream(utf16Bytes);
        codec = new PlainTextCodec("text/plain; charset=UTF-16");
        res = (String) codec.decode(in, String.class);
        Assertions.assertEquals("你好，世界", res);
    }
}
