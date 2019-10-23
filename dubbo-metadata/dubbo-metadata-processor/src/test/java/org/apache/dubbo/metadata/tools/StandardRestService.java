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
package org.apache.dubbo.metadata.tools;

import org.apache.dubbo.config.annotation.Service;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.metadata.tools.LoggerUtils.log;

/**
 * JAX-RS {@link RestService}
 */
@Service(version = "1.0.0", protocol = {"dubbo", "rest"})
@Path("/")
public class StandardRestService implements RestService {

    @Override
    @Path("param")
    @GET
    public String param(@QueryParam("param") @DefaultValue("value-param") String param) {
        log("/param", param);
        return param;
    }

    @Override
    @Path("params")
    @POST
    public String params(@QueryParam("a") @DefaultValue("value-a") int a,
                         @QueryParam("b") @DefaultValue("value-b") String b) {
        log("/params", a + b);
        return a + b;
    }

    @Override
    @Path("headers")
    @GET
    public String headers(@HeaderParam("h") @DefaultValue("value-h") String header,
                          @HeaderParam("h2") @DefaultValue("value-h2") String header2,
                          @QueryParam("v") @DefaultValue("1") Integer param) {
        String result = header + " , " + header2 + " , " + param;
        log("/headers", result);
        return result;
    }

    @Override
    @Path("path-variables/{p1}/{p2}")
    @GET
    public String pathVariables(@PathParam("p1") String path1,
                                @PathParam("p2") String path2, @QueryParam("v") String param) {
        String result = path1 + " , " + path2 + " , " + param;
        log("/path-variables", result);
        return result;
    }

    // @CookieParam does not support : https://github.com/OpenFeign/feign/issues/913
    // @CookieValue also does not support

    @Override
    @Path("form")
    @POST
    public String form(@FormParam("f") String form) {
        return String.valueOf(form);
    }

    @Override
    @Path("request/body/map")
    @POST
    @Produces("application/json;charset=UTF-8")
    public User requestBodyMap(Map<String, Object> data,
                               @QueryParam("param") String param) {
        User user = new User();
        user.setId(((Integer) data.get("id")).longValue());
        user.setName((String) data.get("name"));
        user.setAge((Integer) data.get("age"));
        log("/request/body/map", param);
        return user;
    }

    @Path("request/body/user")
    @POST
    @Override
    @Consumes("application/json;charset=UTF-8")
    public Map<String, Object> requestBodyUser(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getName());
        map.put("age", user.getAge());
        return map;
    }
}
