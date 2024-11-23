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
    public OpenAPI resolve(OpenAPI openAPI, ServiceMeta serviceMeta, OpenAPIChain chain) {
        AnnotationMeta<OpenAPIDefinition> annoMeta = serviceMeta.findAnnotation(OpenAPIDefinition.class);
        if (annoMeta == null) {
            return chain.resolve(openAPI, serviceMeta);
        }
        if (serviceMeta.isHierarchyAnnotated(Hidden.class)) {
            return null;
        }

        OpenAPIDefinition anno = annoMeta.getAnnotation();

        Info info = openAPI.getInfo();
        if (info == null) {
            openAPI.setInfo(info = new Info());
        }

        io.swagger.v3.oas.annotations.info.Info infoAnn = anno.info();
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

        for (io.swagger.v3.oas.annotations.tags.Tag tagAnn : anno.tags()) {
            openAPI.addTag(new Tag()
                    .setName(trim(tagAnn.name()))
                    .setDescription(trim(tagAnn.description()))
                    .setExternalDocs(toExternalDocs(tagAnn.externalDocs()))
                    .setExtensions(toProperties(tagAnn.extensions())));
        }

        openAPI.setExternalDocs(toExternalDocs(anno.externalDocs()));

        openAPI.setExtensions(toProperties(anno.extensions()));
        return openAPI;
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

    private static ExternalDocs toExternalDocs(ExternalDocumentation anno) {
        return new ExternalDocs()
                .setDescription(trim(anno.description()))
                .setUrl(trim(anno.url()))
                .setExtensions(toProperties(anno.extensions()));
    }

    @Override
    public Operation resolve(Operation operation, MethodMeta methodMeta, OperationContext ctx, OperationChain chain) {
        AnnotationMeta<io.swagger.v3.oas.annotations.Operation> annoMeta =
                methodMeta.findAnnotation(io.swagger.v3.oas.annotations.Operation.class);
        if (annoMeta == null) {
            return chain.resolve(operation, methodMeta, ctx);
        }
        io.swagger.v3.oas.annotations.Operation anno = annoMeta.getAnnotation();
        if (anno.hidden() || methodMeta.isHierarchyAnnotated(Hidden.class)) {
            return null;
        }

        String method = trim(anno.method());
        if (method != null) {
            operation.setHttpMethod(HttpMethods.of(method.toUpperCase()));
        }
        for (String tag : anno.tags()) {
            operation.addTag(tag);
        }
        return operation
                .setSummary(trim(anno.summary()))
                .setDescription(trim(anno.description()))
                .setExternalDocs(toExternalDocs(anno.externalDocs()))
                .setOperationId(trim(anno.operationId()))
                .setDeprecated(anno.deprecated() ? Boolean.TRUE : null)
                .setExtensions(toProperties(anno.extensions()));
    }

    @Override
    public Schema resolve(ParameterMeta parameter, SchemaContext context, SchemaChain chain) {
        AnnotationMeta<io.swagger.v3.oas.annotations.media.Schema> meta =
                parameter.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
        if (meta == null) {
            return chain.resolve(parameter, context);
        }
        io.swagger.v3.oas.annotations.media.Schema schema = meta.getAnnotation();
        if (schema.hidden() || parameter.isHierarchyAnnotated(Hidden.class)) {
            return null;
        }
        return chain.resolve(parameter, context);
    }

    @Override
    public Boolean acceptProperty(BeanMeta bean, PropertyMeta property) {
        AnnotationMeta<io.swagger.v3.oas.annotations.media.Schema> meta =
                bean.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
        if (meta == null) {
            return null;
        }
        io.swagger.v3.oas.annotations.media.Schema schema = meta.getAnnotation();
        return schema.hidden() || bean.isHierarchyAnnotated(Hidden.class) ? false : null;
    }
}
