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

import org.apache.dubbo.rpc.protocol.rest.User;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@Path("u")
@Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
public interface AnotherUserRestService {

    @GET
    @Path("{id : \\d+}")
    @Produces({MediaType.APPLICATION_JSON})
    User getUser(@PathParam("id") Long id);

    @POST
    @Path("register")
    @Produces("text/xml; charset=UTF-8")
    RegistrationResult registerUser(User user);

    @GET
    @Path("context")
    @Produces({MediaType.APPLICATION_JSON})
    String getContext();

    @POST
    @Path("bytes")
    @Produces({MediaType.APPLICATION_JSON})
    byte[] bytes(byte[] bytes);

    @POST
    @Path("number")
    @Produces({MediaType.APPLICATION_JSON})
    Long number(Long number);

    @POST
    @Path("headerMap")
    @Produces({MediaType.APPLICATION_JSON})
    String headerMap(@HeaderParam("headers") Map<String,String> headers);
}
