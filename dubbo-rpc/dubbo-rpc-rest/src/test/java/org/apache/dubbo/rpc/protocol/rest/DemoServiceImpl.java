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


import io.netty.handler.codec.http.DefaultFullHttpRequest;
import org.apache.dubbo.rpc.RpcContext;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Map;

@Path("/demoService")
public class DemoServiceImpl implements DemoService {
    private static Map<String, Object> context;
    private boolean called;

    @POST
    @Path("/say")
    @Consumes({MediaType.TEXT_PLAIN})
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
    public void request(DefaultFullHttpRequest defaultFullHttpRequest) {

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
    public List<String> testMapForm(MultivaluedMap<String,String> params) {
        return params.get("form");
    }

    @Override
    public String header(String header) {
        return header;
    }

    @Override
    public int headerInt(int header) {
        return header;
    }

    @Override
    public String noStringParam(String param) {
        return param;
    }


    @Override
    public String noStringHeader(String header) {
        return header;
    }

    @POST
    @Path("/noIntHeader")
    @Consumes({javax.ws.rs.core.MediaType.TEXT_PLAIN})
    @Override
    public int noIntHeader(@HeaderParam("header")int header) {
        return header;
    }

    @POST
    @Path("/noIntParam")
    @Consumes({javax.ws.rs.core.MediaType.TEXT_PLAIN})
    @Override
    public int noIntParam(@QueryParam("header")int header) {
        return header;
    }

    @Override
    public User noBodyArg(User user) {
        return user;
    }

    @GET
    @Path("/hello")
    @Override
    public Integer hello(@QueryParam("a") Integer a, @QueryParam("b") Integer b) {
        context = RpcContext.getServerAttachment().getObjectAttachments();
        return a + b;
    }


    @GET
    @Path("/error")
    @Override
    public String error() {
        throw new RuntimeException("test error");
    }

    public static Map<String, Object> getAttachments() {
        return context;
    }
}
