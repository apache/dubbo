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

import org.apache.dubbo.metadata.annotation.processing.model.Model;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.concurrent.TimeUnit;

/**
 * Test Service
 *
 * @since 2.7.6
 */
@Path("/echo")
public interface TestService {

    @GET
    <T> String echo(@PathParam("message") @DefaultValue("mercyblitz") String message);

    @POST
    Model model(@PathParam("model") Model model);

    // Test primitive
    @PUT
    String testPrimitive(boolean z, int i);

    // Test enumeration
    @PUT
    Model testEnum(TimeUnit timeUnit);

    // Test Array
    @GET
    String testArray(String[] strArray, int[] intArray, Model[] modelArray);
}
