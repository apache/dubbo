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
package org.apache.dubbo.demo.provider;


import org.apache.dubbo.demo.RestDemoService;
import org.apache.dubbo.rpc.RpcContext;
import po.TestPO;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Map;

public class RestDemoServiceImpl implements RestDemoService {
    private static Map<String, Object> context;
    private boolean called;

    public String sayHello(String name) {
        called = true;
        return "Hello, " + name;
    }


    public boolean isCalled() {
        return called;
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
    public String getRemoteApplicationName() {
        return RpcContext.getServiceContext().getRemoteApplicationName();
    }

    @Override
    public Integer testBody(Integer b) {
        return b;
    }

    @Override
    public String testBody2(String b) {
        return b;
    }

    @Override
    public Boolean testBody2(Boolean b) {
        return b;
    }

    @Override
    public TestPO testBody2(TestPO b) {
        return b;
    }

    @Override
    public TestPO testBody5(TestPO testPO) {
        return testPO;
    }


    public String testForm1(String test) {
        return test;
    }


    public MultivaluedMap testForm2(MultivaluedMap map) {
        return map;
    }
}
