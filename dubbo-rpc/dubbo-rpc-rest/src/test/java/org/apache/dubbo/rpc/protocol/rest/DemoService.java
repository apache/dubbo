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


import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

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
    @Produces({javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED})
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
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

    @GET
    @Path("/primitiveShort")
    long primitiveShort(@QueryParam("a") short a, @QueryParam("b") Long b);
}
