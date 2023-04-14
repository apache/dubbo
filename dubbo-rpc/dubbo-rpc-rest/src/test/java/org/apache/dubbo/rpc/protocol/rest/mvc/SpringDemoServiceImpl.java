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
package org.apache.dubbo.rpc.protocol.rest.mvc;


import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.protocol.rest.User;
import org.springframework.util.LinkedMultiValueMap;

import java.util.List;
import java.util.Map;

public class SpringDemoServiceImpl implements SpringRestDemoService {
    private static Map<String, Object> context;
    private boolean called;


    @Override
    public String sayHello(String name) {
        called = true;
        return "Hello, " + name;
    }


    @Override
    public boolean isCalled() {
        return called;
    }

    @Override
    public String testFormBody(User user) {
        return user.getName();
    }

    @Override
    public List<String> testFormMapBody(LinkedMultiValueMap map) {
        return map.get("form");
    }

    @Override
    public String testHeader(String header) {
        return header;
    }

    @Override
    public String testHeaderInt(int header) {
        return String.valueOf(header);
    }

    @Override
    public Integer hello(Integer a, Integer b) {
        context = RpcContext.getServerAttachment().getObjectAttachments();
        return a + b;
    }


    @Override
    public String error() {
        throw new RuntimeException();
    }

    public static Map<String, Object> getAttachments() {
        return context;
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



}
