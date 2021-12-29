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
package org.apache.dubbo.common;

import org.apache.dubbo.common.utils.NetUtils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class URLBuilderTest {
    @Test
    public void testNoArgConstructor() {
        URL url = new URLBuilder().build();
        assertThat(url.toString(), equalTo(""));
    }

    @Test
    public void shouldAddParameter() {
        URL url1 = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan");
        URL url2 = URLBuilder.from(url1)
                .addParameter("newKey1", "newValue1") // string
                .addParameter("newKey2", 2) // int
                .addParameter("version", 1) // override
                .build();
        assertThat(url2.getParameter("newKey1"), equalTo("newValue1"));
        assertThat(url2.getParameter("newKey2"), equalTo("2"));
        assertThat(url2.getVersion(), equalTo("1"));
    }

    @Test
    public void shouldSet() {
        URL url1 = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan");
        int port = NetUtils.getAvailablePort();
        URL url2 = URLBuilder.from(url1)
                .setProtocol("rest")
                .setUsername("newUsername")
                .setPassword("newPassword")
                .setHost("newHost")
                .setPath("newContext")
                .setPort(port)
                .build();
        assertThat(url2.getProtocol(), equalTo("rest"));
        assertThat(url2.getUsername(), equalTo("newUsername"));
        assertThat(url2.getPassword(), equalTo("newPassword"));
        assertThat(url2.getHost(), equalTo("newHost"));
        assertThat(url2.getPort(), equalTo(port));
        assertThat(url2.getPath(), equalTo("newContext"));

        int port2 = NetUtils.getAvailablePort();
        url2 = URLBuilder.from(url1)
                .setAddress("newHost2:"+ port2)
                .build();
        assertThat(url2.getHost(), equalTo("newHost2"));
        assertThat(url2.getPort(), equalTo(port2));
    }

    @Test
    public void shouldClearParameters() {
        URL url1 = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan");
        URL url2 = URLBuilder.from(url1)
                .clearParameters()
                .build();
        assertThat(url2.getParameters().size(), equalTo(0));
    }

    @Test
    public void shouldRemoveParameters() {
        URL url1 = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan&key2=v2");
        URL url2 = URLBuilder.from(url1)
                .removeParameters(Arrays.asList("key2", "application"))
                .build();
        assertThat(url2.getParameters().size(), equalTo(1));
        assertThat(url2.getVersion(), equalTo("1.0.0"));
    }

    @Test
    public void shouldAddIfAbsent() {
        URL url1 = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan&key2=v2");
        URL url2 = URLBuilder.from(url1)
                .addParameterIfAbsent("absentKey", "absentValue")
                .addParameterIfAbsent("version", "2.0.0") // should not override
                .build();
        assertThat(url2.getVersion(), equalTo("1.0.0"));
        assertThat(url2.getParameter("absentKey"), equalTo("absentValue"));
    }

    @Test
    public void shouldAddParameters() {
        URL url1 = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan&key2=v2");

        // string pairs test
        URL url2 = URLBuilder.from(url1)
                .addParameters("version", "1.0.0", "absentKey1", "absentValue1")
                .build();
        assertThat(url2.getParameter("version"), equalTo("1.0.0"));
        assertThat(url2.getParameter("absentKey1"), equalTo("absentValue1"));

        // map test
        Map<String, String> parameters = new HashMap<String, String>(){
            {
                this.put("version", "2.0.0");
                this.put("absentKey2", "absentValue2");
            }
        };
        url2 = URLBuilder.from(url1)
                .addParameters(parameters)
                .build();
        assertThat(url2.getParameter("version"), equalTo("2.0.0"));
        assertThat(url2.getParameter("absentKey2"), equalTo("absentValue2"));
    }

    @Test
    public void shouldAddParametersIfAbsent() {
        URL url1 = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path?version=1.0.0&application=morgan&key2=v2");

        Map<String, String> parameters = new HashMap<String, String>(){
            {
                this.put("version", "2.0.0");
                this.put("absentKey", "absentValue");
            }
        };
        URL url2 = URLBuilder.from(url1)
                .addParametersIfAbsent(parameters)
                .build();
        assertThat(url2.getParameter("version"), equalTo("1.0.0"));
        assertThat(url2.getParameter("absentKey"), equalTo("absentValue"));
    }
}
