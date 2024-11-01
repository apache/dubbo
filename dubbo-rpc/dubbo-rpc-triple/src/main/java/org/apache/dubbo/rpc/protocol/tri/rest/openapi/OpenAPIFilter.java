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
package org.apache.dubbo.rpc.protocol.tri.rest.openapi;

import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.ApiResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.OpenAPI;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Operation;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Parameter;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.PathItem;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.RequestBody;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Schema;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.SecurityScheme;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Server;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Tag;

public interface OpenAPIFilter extends OpenAPIExtension {

    default OpenAPI filterOpenAPI(OpenAPI openAPI, FilterContext context) {
        return openAPI;
    }

    default PathItem filterPathItem(PathItem pathItem, FilterContext context) {
        return pathItem;
    }

    default Operation filterOperation(Operation operation, FilterContext context) {
        return operation;
    }

    default Parameter filterParameter(Parameter parameter, FilterContext context) {
        return parameter;
    }

    default RequestBody filterRequestBody(RequestBody requestBody, FilterContext context) {
        return requestBody;
    }

    default ApiResponse filterResponse(ApiResponse apiResponse, FilterContext context) {
        return apiResponse;
    }

    default Schema filterSchema(Schema schema, FilterContext context) {
        return schema;
    }

    default Schema filterSchemaProperty(Schema schema, FilterContext context) {
        return schema;
    }

    default Server filterServer(Server server, FilterContext context) {
        return server;
    }

    default SecurityScheme filterSecurityScheme(SecurityScheme securityScheme, FilterContext context) {
        return securityScheme;
    }

    default Tag filterTag(Tag tag, FilterContext context) {
        return tag;
    }

    default OpenAPI filterOpenAPICompletion(OpenAPI openAPI, FilterContext context) {
        return openAPI;
    }
}
