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
package org.apache.dubbo.rpc.protocol.rest.integration.swagger;

import org.apache.dubbo.config.annotation.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.jaxrs.listing.BaseApiListingResource;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Application;

@Service
public class DubboSwaggerApiListingResource extends BaseApiListingResource implements DubboSwaggerService {

    @Context
    ServletContext context;

    @Override
    public Response getListingJson(Application app, ServletConfig sc,
                                   HttpHeaders headers, UriInfo uriInfo)  throws JsonProcessingException {
        Response response =  getListingJsonResponse(app, context, sc, headers, uriInfo);
        response.getHeaders().add("Access-Control-Allow-Origin", "*");
        response.getHeaders().add("Access-Control-Allow-Headers", "x-requested-with, ssi-token");
        response.getHeaders().add("Access-Control-Max-Age", "3600");
        response.getHeaders().add("Access-Control-Allow-Methods","GET,POST,PUT,DELETE,OPTIONS");
        return response;
    }
}