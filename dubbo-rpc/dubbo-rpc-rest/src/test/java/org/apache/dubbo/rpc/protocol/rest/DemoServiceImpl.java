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


import org.apache.dubbo.rpc.RpcContext;

import java.util.Map;

public class DemoServiceImpl implements DemoService {
    private static Map<String, String> context;
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
        context = RpcContext.getContext().getAttachments();
        return a + b;
    }

    @Override
    public String error() {
        throw new RuntimeException();
    }

    public static Map<String, String> getAttachments() {
        return context;
    }

    @Override
    public String getRemoteApplicationName() {
        return RpcContext.getContext().getRemoteApplicationName();
    }
}
