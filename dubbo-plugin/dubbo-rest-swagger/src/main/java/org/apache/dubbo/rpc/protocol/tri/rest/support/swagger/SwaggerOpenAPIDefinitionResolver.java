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
package org.apache.dubbo.rpc.protocol.tri.rest.support.swagger;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.BeanMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.BeanMeta.PropertyMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.OpenAPIDefinitionResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.OpenAPISchemaPredicate;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.OpenAPISchemaResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.ResolveContext;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Contact;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.ExternalDocs;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Info;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.License;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.OpenAPI;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Operation;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Schema;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Tag;

import java.util.Map;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;

import static org.apache.dubbo.rpc.protocol.tri.rest.openapi.Helper.trim;

@Activate(order = 50, onClass = "io.swagger.v3.oas.annotations.OpenAPIDefinition")
public final class SwaggerOpenAPIDefinitionResolver
        implements OpenAPIDefinitionResolver, OpenAPISchemaResolver, OpenAPISchemaPredicate {

    @Override
    public boolean hidden(ServiceMeta serviceMeta) {
        return serviceMeta.isHierarchyAnnotated(Hidden.class);
    }

    @Override
    public OpenAPI resolve(ServiceMeta serviceMeta) {
        AnnotationMeta<OpenAPIDefinition> meta = serviceMeta.findAnnotation(OpenAPIDefinition.class);
        if (meta == null) {
            return null;
        }

        OpenAPI model = new OpenAPI();
        OpenAPIDefinition definition = meta.getAnnotation();

        Info info = new Info();
        model.setInfo(info);
        io.swagger.v3.oas.annotations.info.Info infoAnn = definition.info();
        info.setTitle(trim(infoAnn.title()))
                .setDescription(trim(infoAnn.description()))
                .setVersion(trim(infoAnn.version()))
                .setExtensions(toProperties(infoAnn.extensions()));

        Contact contact = new Contact();
        info.setContact(contact);
        io.swagger.v3.oas.annotations.info.Contact contactAnn = infoAnn.contact();
        contact.setName(trim(contactAnn.name()))
                .setEmail(trim(contactAnn.email()))
                .setUrl(trim(contactAnn.url()))
                .setExtensions(toProperties(contactAnn.extensions()));

        License license = new License();
        info.setLicense(license);
        io.swagger.v3.oas.annotations.info.License licenseAnn = infoAnn.license();
        license.setName(trim(licenseAnn.name()))
                .setUrl(trim(licenseAnn.url()))
                .setExtensions(toProperties(licenseAnn.extensions()));

        for (io.swagger.v3.oas.annotations.tags.Tag tagAnn : definition.tags()) {
            model.addTag(new Tag()
                    .setName(trim(tagAnn.name()))
                    .setDescription(trim(tagAnn.description()))
                    .setExternalDocs(toExternalDocs(tagAnn.externalDocs()))
                    .setExtensions(toProperties(tagAnn.extensions())));
        }

        model.setExternalDocs(toExternalDocs(definition.externalDocs()));

        model.setExtensions(toProperties(definition.extensions()));
        return model;
    }

    private static Map<String, String> toProperties(io.swagger.v3.oas.annotations.extensions.Extension[] extensions) {
        int len = extensions.length;
        if (len == 0) {
            return null;
        }
        Map<String, String> properties = CollectionUtils.newLinkedHashMap(extensions.length);
        for (io.swagger.v3.oas.annotations.extensions.Extension extension : extensions) {
            for (ExtensionProperty property : extension.properties()) {
                properties.put(property.name(), property.value());
            }
        }
        return properties;
    }

    private static ExternalDocs toExternalDocs(ExternalDocumentation ann) {
        return new ExternalDocs()
                .setDescription(trim(ann.description()))
                .setUrl(trim(ann.url()))
                .setExtensions(toProperties(ann.extensions()));
    }

    @Override
    public boolean hidden(MethodMeta methodMeta, OpenAPI openAPI, ResolveContext context) {
        return methodMeta.isHierarchyAnnotated(Hidden.class);
    }

    @Override
    public Operation resolve(MethodMeta methodMeta, OpenAPI openAPI, ResolveContext context) {
        AnnotationMeta<io.swagger.v3.oas.annotations.Operation> meta =
                methodMeta.findAnnotation(io.swagger.v3.oas.annotations.Operation.class);
        if (meta == null) {
            return null;
        }

        io.swagger.v3.oas.annotations.Operation operation = meta.getAnnotation();
        Operation model = new Operation();

        String method = trim(operation.method());
        if (method != null) {
            model.setHttpMethod(HttpMethods.of(method.toUpperCase()));
        }
        for (String tag : operation.tags()) {
            model.addTag(tag);
        }
        return model.setSummary(trim(operation.summary()))
                .setDescription(trim(operation.description()))
                .setExternalDocs(toExternalDocs(operation.externalDocs()))
                .setOperationId(trim(operation.operationId()))
                .setDeprecated(operation.deprecated() ? Boolean.TRUE : null)
                .setExtensions(toProperties(operation.extensions()));
    }

    @Override
    public Schema resolve(ParameterMeta parameter, Context context, Chain chain) {
        return chain.resolve(parameter, context);
    }

    @Override
    public Boolean acceptClass(Class<?> clazz, ParameterMeta parameter) {
        AnnotationMeta<io.swagger.v3.oas.annotations.media.Schema> schema =
                parameter.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
        return schema == null ? null : !schema.getAnnotation().hidden();
    }

    @Override
    public Boolean acceptProperty(BeanMeta bean, PropertyMeta property) {
        AnnotationMeta<io.swagger.v3.oas.annotations.media.Schema> schema =
                property.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
        return schema == null ? null : !schema.getAnnotation().hidden();
    }
}
