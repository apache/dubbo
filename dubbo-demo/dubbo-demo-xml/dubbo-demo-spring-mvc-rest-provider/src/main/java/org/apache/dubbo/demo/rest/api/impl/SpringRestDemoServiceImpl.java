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
package org.apache.dubbo.demo.rest.api.impl;

import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.demo.rest.api.SpringRestDemoService;

import java.util.List;
import java.util.Map;

import org.springframework.util.MultiValueMap;
import po.User;

@DubboService(interfaceClass = SpringRestDemoService.class, protocol = "rest")
public class SpringRestDemoServiceImpl implements SpringRestDemoService {

    @Override
    public String sayHello(String name) {
        return "Hello, " + name;
    }

    @Override
    public Long testFormBody(Long number) {
        return number;
    }

    @Override
    public User testJavaBeanBody(User user) {
        return user;
    }

    @Override
    public int primitiveInt(int a, int b) {
        return a + b;
    }

    @Override
    public long primitiveLong(long a, Long b) {
        return a + b;
    }

    @Override
    public long primitiveByte(byte a, Long b) {
        return a + b;
    }

    @Override
    public long primitiveShort(short a, Long b, int c) {
        return a + b;
    }

    @Override
    public String testMapParam(Map<String, String> params) {
        return params.get("param");
    }

    @Override
    public String testMapHeader(Map<String, String> headers) {
        return headers.get("header");
    }

    @Override
    public List<String> testMapForm(MultiValueMap<String, String> params) {
        return params.get("form");
    }

    @Override
    public int headerInt(int header) {
        return header;
    }

    @Override
    public Integer hello(Integer a, Integer b) {
        return a + b;
    }

    @Override
    public String error() {
        throw new RuntimeException("test error");
    }
}
