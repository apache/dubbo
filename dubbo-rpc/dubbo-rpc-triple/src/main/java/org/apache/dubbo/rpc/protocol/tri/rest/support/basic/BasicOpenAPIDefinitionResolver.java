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
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.BeanMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.BeanMeta.PropertyMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.Helper;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.OpenAPIDefinitionResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.OpenAPISchemaPredicate;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.OpenAPISchemaResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.ResolveContext;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.ExternalDocs;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Info;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.OpenAPI;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Operation;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Schema;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Schema.Type;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Tag;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Consumer;

import static org.apache.dubbo.rpc.protocol.tri.rest.openapi.Helper.trim;

@Activate(order = 100)
public final class BasicOpenAPIDefinitionResolver
        implements OpenAPIDefinitionResolver, OpenAPISchemaResolver, OpenAPISchemaPredicate {

    private static final String HIDDEN = "hidden";

    @Override
    public boolean hidden(ServiceMeta serviceMeta) {
        AnnotationMeta<?> openAPI = serviceMeta.findAnnotation(Annotations.OpenAPI);
        return openAPI != null && openAPI.getBoolean(HIDDEN);
    }

    @Override
    public OpenAPI resolve(ServiceMeta serviceMeta) {
        AnnotationMeta<?> openAPI = serviceMeta.findAnnotation(Annotations.OpenAPI);
        if (openAPI == null) {
            return null;
        }

        OpenAPI model = new OpenAPI();
        Map<String, String> tags = Helper.toProperties(openAPI.getStringArray("tags"));
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            model.addTag(new Tag().setName(entry.getKey()).setDescription(entry.getValue()));
        }
        model.setGroup(trim(openAPI.getString("group")));

        String title = trim(openAPI.getString("infoTitle"));
        String description = trim(openAPI.getString("infoDescription"));
        String version = trim(openAPI.getString("infoVersion"));
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
        AnnotationMeta<?> operation = methodMeta.findAnnotation(Annotations.Operation);
        return operation != null && operation.getBoolean(HIDDEN);
    }

    @Override
    public Operation resolve(MethodMeta methodMeta, OpenAPI openAPI, ResolveContext context) {
        AnnotationMeta<?> operation = methodMeta.findAnnotation(Annotations.Operation);
        if (operation == null) {
            return null;
        }

        Operation model = new Operation();
        String method = trim(operation.getString("method"));
        if (method != null) {
            model.setHttpMethod(HttpMethods.of(method.toUpperCase()));
        }

        String[] tags = trim(operation.getStringArray("tags"));
        if (tags != null) {
            model.setTags(new LinkedHashSet<>(Arrays.asList(tags)));
        }

        model.setGroup(trim(operation.getString("group")));
        model.setVersion(trim(operation.getString("version")));
        model.setOperationId(trim(operation.getString("id")));
        String summary = trim(operation.getValue());
        model.setSummary(summary == null ? trim(operation.getString("summary")) : summary);
        model.setDescription(trim(operation.getString("description")));
        model.setDeprecated(operation.getBoolean("deprecated"));
        model.setExtensions(Helper.toProperties(operation.getStringArray("extensions")));
        return model;
    }

    @Override
    public Schema resolve(ParameterMeta parameter, Context context, Chain chain) {
        AnnotationMeta<?> schema = parameter.getAnnotation(Annotations.Schema);
        if (schema == null) {
            return chain.resolve(parameter, context);
        }

        Class<?> impl = schema.getClass("implementation");
        Schema model = impl == null ? chain.resolve(parameter, context) : context.getSchema(impl);

        setValue(schema, "group", model::setGroup);
        setValue(schema, "version", model::setVersion);
        setValue(schema, "type", v -> model.setType(Type.valueOf(v)));
        setValue(schema, "format", model::setFormat);
        setValue(schema, "name", model::setName);
        String title = trim(schema.getValue());
        model.setTitle(title == null ? trim(schema.getString("title")) : title);
        setValue(schema, "title", model::setTitle);
        setValue(schema, "description", model::setDescription);
        setValue(schema, "max", v -> model.setMaxLength(Integer.parseInt(v)));
        setValue(schema, "min", v -> model.setMinLength(Integer.parseInt(v)));
        setValue(schema, "pattern", model::setPattern);
        setValue(schema, "example", model::setExample);
        String[] enumItems = trim(schema.getStringArray("enumeration"));
        if (enumItems != null) {
            model.setEnumeration(Arrays.asList(enumItems));
        }
        model.setRequired(schema.getBoolean("required"));
        setValue(schema, "defaultValue", model::setDefaultValue);
        model.setReadOnly(schema.getBoolean("readOnly"));
        model.setWriteOnly(schema.getBoolean("writeOnly"));
        model.setNullable(schema.getBoolean("nullable"));
        model.setDeprecated(schema.getBoolean("deprecated"));
        model.setExtensions(Helper.toProperties(schema.getStringArray("extensions")));
        return model;
    }

    private static void setValue(AnnotationMeta<?> schema, String key, Consumer<String> setter) {
        String value = trim(schema.getString(key));
        if (value != null) {
            setter.accept(value);
        }
    }

    @Override
    public Boolean acceptClass(Class<?> clazz, ParameterMeta parameter) {
        AnnotationMeta<?> schema = parameter.getAnnotation(Annotations.Schema);
        return schema == null ? null : schema.getBoolean(HIDDEN);
    }

    @Override
    public Boolean acceptProperty(ParameterMeta parameter, BeanMeta bean, PropertyMeta property) {
        AnnotationMeta<?> schema = parameter.getAnnotation(Annotations.Schema);
        return schema == null ? null : schema.getBoolean(HIDDEN);
    }
}
