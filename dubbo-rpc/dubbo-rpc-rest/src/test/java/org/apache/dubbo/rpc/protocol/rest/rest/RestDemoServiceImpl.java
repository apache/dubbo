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
package org.apache.dubbo.rpc.protocol.rest.rest;

import org.apache.dubbo.rpc.RpcContext;

import javax.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

import org.jboss.resteasy.specimpl.BuiltResponse;

public class RestDemoServiceImpl implements RestDemoService {
    private static Map<String, Object> context;
    private boolean called;

    @Override
    public String sayHello(String name) {
        called = true;
        return "Hello, " + name;
    }

    @Override
    public Long testFormBody(Long number) {
        return number;
    }

    public boolean isCalled() {
        return called;
    }

    @Override
    public String deleteUserByUid(String uid) {
        return uid;
    }

    @Override
    public Integer hello(Integer a, Integer b) {
        context = RpcContext.getServerAttachment().getObjectAttachments();
        return a + b;
    }

    @Override
    public Response findUserById(Integer id) {
        Map<String, Object> content = new HashMap<>();
        content.put("username", "jack");
        content.put("id", id);

        return BuiltResponse.ok(content).build();
    }

    @Override
    public String error() {
        throw new RuntimeException();
    }

    @Override
    public Response deleteUserById(String uid) {
        return Response.status(300).entity("deleted").build();
    }

    public static Map<String, Object> getAttachments() {
        return context;
    }
}
