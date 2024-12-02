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
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.ExternalDocs;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Info;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.OpenAPI;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Operation;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.PathItem;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Schema;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Schema.Type;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Tag;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;

import static org.apache.dubbo.rpc.protocol.tri.rest.openapi.Helper.setBoolValue;
import static org.apache.dubbo.rpc.protocol.tri.rest.openapi.Helper.setValue;
import static org.apache.dubbo.rpc.protocol.tri.rest.openapi.Helper.trim;

@Activate(order = 100)
public final class BasicOpenAPIDefinitionResolver
        implements OpenAPIDefinitionResolver, OpenAPISchemaResolver, OpenAPISchemaPredicate {

    private static final String HIDDEN = "hidden";

    @Override
    public OpenAPI resolve(OpenAPI openAPI, ServiceMeta serviceMeta, OpenAPIChain chain) {
        AnnotationMeta<?> annoMeta = serviceMeta.findAnnotation(Annotations.OpenAPI);
        if (annoMeta == null) {
            return chain.resolve(openAPI, serviceMeta);
        }
        if (annoMeta.getBoolean(HIDDEN)) {
            return null;
        }

        Info info = openAPI.getInfo();
        if (info == null) {
            openAPI.setInfo(info = new Info());
        }

        Map<String, String> tags = Helper.toProperties(annoMeta.getStringArray("tags"));
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            openAPI.addTag(new Tag().setName(entry.getKey()).setDescription(entry.getValue()));
        }

        String group = trim(annoMeta.getString("group"));
        if (group != null) {
            openAPI.setGroup(group);
        }

        String title = trim(annoMeta.getString("infoTitle"));
        if (title != null) {
            info.setTitle(title);
        }
        String description = trim(annoMeta.getString("infoDescription"));
        if (description != null) {
            info.setDescription(description);
        }
        String version = trim(annoMeta.getString("infoVersion"));
        if (version != null) {
            info.setVersion(version);
        }

        String docDescription = trim(annoMeta.getString("docDescription"));
        String docUrl = trim(annoMeta.getString("docUrl"));
        if (docDescription != null || docUrl != null) {
            openAPI.setExternalDocs(
                    new ExternalDocs().setDescription(docDescription).setUrl(docUrl));
        }

        openAPI.setPriority(annoMeta.getNumber("order"));
        openAPI.setExtensions(Helper.toProperties(annoMeta.getStringArray("extensions")));

        return chain.resolve(openAPI, serviceMeta);
    }

    @Override
    public Collection<HttpMethods> resolve(PathItem pathItem, MethodMeta methodMeta, OperationContext context) {
        AnnotationMeta<?> annoMeta = methodMeta.findAnnotation(Annotations.Operation);
        if (annoMeta == null) {
            return null;
        }
        String method = trim(annoMeta.getString("method"));
        if (method == null) {
            return null;
        }
        return Collections.singletonList(HttpMethods.of(method.toUpperCase()));
    }

    @Override
    public Operation resolve(Operation operation, MethodMeta methodMeta, OperationContext ctx, OperationChain chain) {
        AnnotationMeta<?> annoMeta = methodMeta.findAnnotation(Annotations.Operation);
        if (annoMeta == null) {
            return chain.resolve(operation, methodMeta, ctx);
        }
        if (annoMeta.getBoolean(HIDDEN)) {
            return null;
        }

        String[] tags = trim(annoMeta.getStringArray("tags"));
        if (tags != null) {
            operation.setTags(new LinkedHashSet<>(Arrays.asList(tags)));
        }

        String summary = trim(annoMeta.getValue());
        if (summary == null) {
            summary = trim(annoMeta.getString("summary"));
        }
        operation
                .setGroup(trim(annoMeta.getString("group")))
                .setVersion(trim(annoMeta.getString("version")))
                .setOperationId(trim(annoMeta.getString("id")))
                .setSummary(summary)
                .setDescription(trim(annoMeta.getString("description")))
                .setDeprecated(annoMeta.getBoolean("deprecated"))
                .setExtensions(Helper.toProperties(annoMeta.getStringArray("extensions")));

        return chain.resolve(operation, methodMeta, ctx);
    }

    @Override
    public Schema resolve(ParameterMeta parameter, SchemaContext context, SchemaChain chain) {
        AnnotationMeta<?> annoMeta = parameter.getAnnotation(Annotations.Schema);
        if (annoMeta == null) {
            return chain.resolve(parameter, context);
        }
        if (annoMeta.getBoolean(HIDDEN)) {
            return null;
        }

        Class<?> impl = annoMeta.getClass("implementation");
        Schema schema = impl == Void.class ? chain.resolve(parameter, context) : context.resolve(impl);

        setValue(annoMeta, "group", schema::setGroup);
        setValue(annoMeta, "version", schema::setVersion);
        setValue(annoMeta, "type", v -> schema.setType(Type.valueOf(v)));
        setValue(annoMeta, "format", schema::setFormat);
        setValue(annoMeta, "name", schema::setName);
        String title = trim(annoMeta.getValue());
        schema.setTitle(title == null ? trim(annoMeta.getString("title")) : title);
        setValue(annoMeta, "description", schema::setDescription);
        setValue(annoMeta, "defaultValue", schema::setDefaultValue);
        setValue(annoMeta, "max", v -> schema.setMaxLength(Integer.parseInt(v)));
        setValue(annoMeta, "min", v -> schema.setMinLength(Integer.parseInt(v)));
        setValue(annoMeta, "pattern", schema::setPattern);
        setValue(annoMeta, "example", schema::setExample);
        String[] enumItems = trim(annoMeta.getStringArray("enumeration"));
        if (enumItems != null) {
            schema.setEnumeration(Arrays.asList(enumItems));
        }
        setBoolValue(annoMeta, "required", schema::setRequired);
        setBoolValue(annoMeta, "readOnly", schema::setReadOnly);
        setBoolValue(annoMeta, "writeOnly", schema::setWriteOnly);
        setBoolValue(annoMeta, "nullable", schema::setNullable);
        setBoolValue(annoMeta, "deprecated", schema::setDeprecated);
        schema.setExtensions(Helper.toProperties(annoMeta.getStringArray("extensions")));

        return chain.resolve(parameter, context);
    }

    @Override
    public Boolean acceptProperty(BeanMeta bean, PropertyMeta property) {
        AnnotationMeta<?> annoMeta = property.getAnnotation(Annotations.Schema);
        return annoMeta == null ? null : annoMeta.getBoolean(HIDDEN);
    }
}
