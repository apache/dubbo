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
package org.apache.dubbo.demo;


import po.TestPO;

import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.POST;
import javax.ws.rs.FormParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;


@Path("/demoService")
public interface RestDemoService {
    @GET
    @Path("/hello")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    Integer hello(@QueryParam("a") Integer a, @QueryParam("b") Integer b);

    @GET
    @Path("/error")
    String error();

    @POST
    @Path("/say")
    @Consumes({MediaType.TEXT_PLAIN})
    String sayHello(String name);

    @GET
    @Path("/getRemoteApplicationName")
    String getRemoteApplicationName();

    @POST
    @Path("/testBody1")
    @Consumes({MediaType.TEXT_PLAIN})
    Integer testBody(Integer b);

    @POST
    @Path("/testBody2")
    @Consumes({MediaType.TEXT_PLAIN})
    String testBody2(String b);

    @POST
    @Path("/testBody3")
    @Consumes({MediaType.TEXT_PLAIN})
    Boolean testBody2(Boolean b);

    @POST
    @Path("/testBody3")
    @Consumes({MediaType.TEXT_PLAIN})
    TestPO testBody2(TestPO b);


    @POST
    @Path("/testBody5")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    TestPO testBody5(TestPO testPO);

    @POST
    @Path("/testForm1")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    String testForm1(@FormParam("name") String test);


    @POST
    @Path("/testForm2")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_FORM_URLENCODED)
    MultivaluedMap testForm2(MultivaluedMap map);
}
