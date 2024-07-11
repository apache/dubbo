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
package org.apache.dubbo.metadata;

import org.apache.dubbo.metadata.swagger.models.OpenAPI;
import org.apache.dubbo.metadata.swagger.models.Operation;
import org.apache.dubbo.metadata.swagger.models.PathItem;
import org.apache.dubbo.metadata.swagger.models.Paths;
import org.apache.dubbo.metadata.swagger.models.info.Info;
import org.apache.dubbo.metadata.swagger.models.info.License;
import org.apache.dubbo.metadata.swagger.models.parameters.Parameter;
import org.apache.dubbo.metadata.swagger.models.responses.ApiResponse;
import org.apache.dubbo.metadata.swagger.models.responses.ApiResponses;
import org.apache.dubbo.metadata.swagger.models.servers.Server;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.DefaultRequestMappingRegistry;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMapping;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.HandlerMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OpenAPIGenerator {

    private final DefaultRequestMappingRegistry requestMappingRegistry;

    public OpenAPIGenerator(DefaultRequestMappingRegistry requestMappingRegistry) {
        this.requestMappingRegistry = requestMappingRegistry;
    }

    public String generateOpenAPIDocument() {
        Map<RequestMapping, HandlerMeta> handlerMetaMap = requestMappingRegistry.getRegistrations();

        Info info = new Info();
        info.setTitle("Generated API");
        info.setDescription("This is a generated API");
        info.setVersion("1.0.0");

        License license = new License();
        license.setName("Apache 2.0");
        license.setUrl("https://www.apache.org/licenses/LICENSE-2.0");

        info.setLicense(license);
        ApiResponses response = new ApiResponses();
        List<Server> servers = new ArrayList<>();
        Paths paths = new Paths();

        handlerMetaMap.forEach((key, value) -> {
            if(value.getMethodDescriptor().getMethodName().contains("metadata")) return;
            Server server = new Server();
            server.setUrl(value.getService().getUrl().toString());
            servers.add(server);

            Operation operation = new Operation();
            ApiResponse apiResponse = new ApiResponse();
            response.addApiResponse("200", apiResponse);
            operation.setResponses(response);

            PathItem pathItem = new PathItem();
            pathItem.setPost(operation);
            pathItem.setDescription("hahhaha！");
            pathItem.setGet(operation);
            pathItem.setServers(servers);

            Parameter parameter = new Parameter();

            paths.addPathItem("/user", pathItem);
        });

        OpenAPI openAPI = new OpenAPI().openapi("3.0.2").info(info).paths(paths);

        // Directly calling toString assuming OpenAPI class has a well-defined toString implementation
        return openAPI.toString();
    }
}
