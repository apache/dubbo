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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
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


    public boolean isCalled() {
        return called;
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
        throw new RuntimeException();
    }

    public static Map<String, Object> getAttachments() {
        return context;
    }
}
