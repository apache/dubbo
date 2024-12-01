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
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.OpenAPI;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Operation;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.PathItem;

import java.util.Collection;

public interface OpenAPIDefinitionResolver extends OpenAPIExtension {

    OpenAPI resolve(OpenAPI openAPI, ServiceMeta serviceMeta, OpenAPIChain chain);

    Collection<HttpMethods> resolve(PathItem pathItem, MethodMeta methodMeta, OperationContext context);

    Operation resolve(Operation operation, MethodMeta methodMeta, OperationContext context, OperationChain chain);

    interface OpenAPIChain {

        OpenAPI resolve(OpenAPI openAPI, ServiceMeta serviceMeta);
    }

    interface OperationChain {

        Operation resolve(Operation operation, MethodMeta methodMeta, OperationContext context);
    }

    interface OperationContext {

        String getGroup();

        OpenAPI getOpenAPI();

        SchemaResolver getSchemaResolver();

        ExtensionFactory getExtensionFactory();

        <T> T getAttribute(String name);

        <T> T removeAttribute(String name);

        void setAttribute(String name, Object value);
    }
}
