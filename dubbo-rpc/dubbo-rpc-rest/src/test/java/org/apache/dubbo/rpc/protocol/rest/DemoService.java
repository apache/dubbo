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

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Map;

@Path("/demoService")
public interface DemoService {
    @GET
    @Path("/hello")
    Integer hello(@QueryParam("a") Integer a, @QueryParam("b") Integer b);

    @GET
    @Path("/error")
    @Consumes({javax.ws.rs.core.MediaType.TEXT_PLAIN})
    @Produces({javax.ws.rs.core.MediaType.TEXT_PLAIN})
    String error();

    @POST
    @Path("/say")
    @Consumes({javax.ws.rs.core.MediaType.TEXT_PLAIN})
    String sayHello(String name);

    @POST
    @Path("number")
    Long testFormBody(@FormParam("number") Long number);

    boolean isCalled();

    @GET
    @Path("/primitive")
    int primitiveInt(@QueryParam("a") int a, @QueryParam("b") int b);

    @GET
    @Path("/primitiveLong")
    long primitiveLong(@QueryParam("a") long a, @QueryParam("b") Long b);

    @GET
    @Path("/primitiveByte")
    long primitiveByte(@QueryParam("a") byte a, @QueryParam("b") Long b);

    @POST
    @Path("/primitiveShort")
    long primitiveShort(@QueryParam("a") short a, @QueryParam("b") Long b, int c);

    @GET
    @Path("/request")
    void request(DefaultFullHttpRequest defaultFullHttpRequest);

    @GET
    @Path("testMapParam")
    @Produces({MediaType.TEXT_PLAIN})
    @Consumes({MediaType.TEXT_PLAIN})
    String testMapParam(@QueryParam("test") Map<String, String> params);

    @GET
    @Path("testMapHeader")
    @Produces({MediaType.TEXT_PLAIN})
    @Consumes({MediaType.TEXT_PLAIN})
    String testMapHeader(@HeaderParam("test") Map<String, String> headers);

    @POST
    @Path("testMapForm")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    List<String> testMapForm(MultivaluedMap<String, String> params);

    @POST
    @Path("/header")
    @Consumes({javax.ws.rs.core.MediaType.TEXT_PLAIN})
    String header(@HeaderParam("header") String header);

    @POST
    @Path("/headerInt")
    @Consumes({javax.ws.rs.core.MediaType.TEXT_PLAIN})
    int headerInt(@HeaderParam("header") int header);

    @POST
    @Path("/noStringParam")
    @Consumes({javax.ws.rs.core.MediaType.TEXT_PLAIN})
    String noStringParam(@QueryParam("param") String param);

    @POST
    @Path("/noStringHeader")
    @Consumes({javax.ws.rs.core.MediaType.TEXT_PLAIN})
    String noStringHeader(@HeaderParam("header") String header);

    @POST
    @Path("/noIntHeader")
    @Consumes({javax.ws.rs.core.MediaType.TEXT_PLAIN})
    int noIntHeader(int header);

    @POST
    @Path("/noIntParam")
    @Consumes({javax.ws.rs.core.MediaType.TEXT_PLAIN})
    int noIntParam(int header);

    @POST
    @Path("/noBodyArg")
    @Consumes({MediaType.APPLICATION_JSON})
    User noBodyArg(User user);

}
