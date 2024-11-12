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
package org.apache.dubbo.rpc.protocol.tri.rest.support.basic;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.Helper;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.OpenAPIDefinitionResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.ResolveContext;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.ExternalDocs;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Info;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.OpenAPI;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Operation;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Tag;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;

import static org.apache.dubbo.rpc.protocol.tri.rest.openapi.Helper.trim;

@Activate(order = 100)
public final class BasicOpenAPIDefinitionResolver implements OpenAPIDefinitionResolver {

    @Override
    public boolean hidden(ServiceMeta serviceMeta) {
        AnnotationMeta<?> openAPI = serviceMeta.getAnnotation(Annotations.OpenAPI);
        return openAPI != null && openAPI.getBoolean("hidden");
    }

    @Override
    public OpenAPI resolve(ServiceMeta serviceMeta) {
        AnnotationMeta<?> openAPI = serviceMeta.getAnnotation(Annotations.OpenAPI);
        if (openAPI == null) {
            return null;
        }

        OpenAPI model = new OpenAPI();
        model.setOpenapi(trim(openAPI.getString("version")));
        Map<String, String> tags = Helper.toProperties(openAPI.getStringArray("tags"));
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            model.addTag(new Tag().setName(entry.getKey()).setDescription(entry.getValue()));
        }
        model.setGroup(trim(openAPI.getString("group")));

        String title = trim(openAPI.getString("title"));
        String description = trim(openAPI.getString("description"));
        String version = trim(openAPI.getString("version"));
        if (title != null || description != null || version != null) {
            model.setInfo(new Info().setTitle(title).setDescription(description).setVersion(version));
        }

        String docDescription = trim(openAPI.getString("docDescription"));
        String docUrl = trim(openAPI.getString("docUrl"));
        if (docDescription != null || docUrl != null) {
            model.setExternalDocs(
                    new ExternalDocs().setDescription(docDescription).setUrl(docUrl));
        }

        model.setPriority(openAPI.getNumber("order"));
        model.setExtensions(Helper.toProperties(openAPI.getStringArray("extensions")));
        return model;
    }

    @Override
    public boolean hidden(MethodMeta methodMeta, OpenAPI openAPI, ResolveContext context) {
        AnnotationMeta<?> operation = methodMeta.getAnnotation(Annotations.Operation);
        return operation != null && operation.getBoolean("hidden");
    }

    @Override
    public Operation resolve(MethodMeta methodMeta, OpenAPI openAPI, ResolveContext context) {
        AnnotationMeta<?> operation = methodMeta.getAnnotation(Annotations.Operation);
        if (operation == null) {
            return null;
        }

        Operation model = new Operation();
        String method = trim(operation.getString("method"));
        if (method != null) {
            model.setHttpMethod(HttpMethods.of(method.toUpperCase()));
        }

        String[] tags = Helper.trim(operation.getStringArray("tags"));
        if (tags != null) {
            model.setTags(new LinkedHashSet<>(Arrays.asList(tags)));
        }

        model.setGroup(trim(operation.getString("group")));
        model.setOperationId(trim(operation.getString("operationId")));
        model.setSummary(trim(operation.getString("summary")));
        model.setDescription(trim(operation.getString("description")));
        model.setDeprecated(operation.getBoolean("deprecated"));
        model.setExtensions(Helper.toProperties(operation.getStringArray("extensions")));
        return model;
    }
}
