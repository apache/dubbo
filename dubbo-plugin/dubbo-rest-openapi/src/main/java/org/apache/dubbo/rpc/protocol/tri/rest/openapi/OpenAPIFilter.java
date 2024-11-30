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

import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.ApiResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Header;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Node;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.OpenAPI;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Operation;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Parameter;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.PathItem;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.RequestBody;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Schema;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.SecurityScheme;

public interface OpenAPIFilter extends OpenAPIExtension {

    default OpenAPI filterOpenAPI(OpenAPI openAPI, Context context) {
        return openAPI;
    }

    default PathItem filterPathItem(String key, PathItem pathItem, Context context) {
        return pathItem;
    }

    default Operation filterOperation(HttpMethods key, Operation operation, PathItem pathItem, Context context) {
        return operation;
    }

    default Parameter filterParameter(Parameter parameter, Operation operation, Context context) {
        return parameter;
    }

    default RequestBody filterRequestBody(RequestBody body, Operation operation, Context context) {
        return body;
    }

    default ApiResponse filterResponse(ApiResponse response, Operation operation, Context context) {
        return response;
    }

    default Header filterHeader(Header header, ApiResponse response, Operation operation, Context context) {
        return header;
    }

    default Schema filterSchema(Schema schema, Node<?> node, Context context) {
        return schema;
    }

    default Schema filterSchemaProperty(String name, Schema schema, Schema owner, Context context) {
        return schema;
    }

    default SecurityScheme filterSecurityScheme(SecurityScheme securityScheme, Context context) {
        return securityScheme;
    }

    default OpenAPI filterOpenAPICompletion(OpenAPI openAPI, Context context) {
        return openAPI;
    }
}
