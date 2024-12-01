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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.FluentLogger;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.nested.OpenAPIConfig;
import org.apache.dubbo.remoting.http12.ErrorResponse;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.HttpUtils;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.remoting.http12.rest.ParamType;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.Registration;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMapping;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.MethodsCondition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathCondition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathExpression;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.BeanMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.BeanMeta.PropertyMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.NamedValueMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.OpenAPIDefinitionResolver.OpenAPIChain;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.OpenAPIDefinitionResolver.OperationChain;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.OpenAPIDefinitionResolver.OperationContext;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.ApiResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.OpenAPI;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Operation;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Parameter;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Parameter.In;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.PathItem;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.RequestBody;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Schema;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Server;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

final class DefinitionResolver {

    private static final FluentLogger LOG = FluentLogger.of(DefinitionResolver.class);

    private final ExtensionFactory extensionFactory;
    private final ConfigFactory configFactory;
    private final SchemaResolver schemaResolver;
    private final OpenAPIDefinitionResolver[] resolvers;

    DefinitionResolver(FrameworkModel frameworkModel) {
        extensionFactory = frameworkModel.getOrRegisterBean(ExtensionFactory.class);
        configFactory = frameworkModel.getOrRegisterBean(ConfigFactory.class);
        schemaResolver = frameworkModel.getOrRegisterBean(SchemaResolver.class);
        resolvers = extensionFactory.getExtensions(OpenAPIDefinitionResolver.class);
    }

    public OpenAPI resolve(ServiceMeta serviceMeta, Collection<List<Registration>> registrationsByMethod) {
        OpenAPI definition = new OpenAPIChainImpl(resolvers, openAPI -> {
                    if (StringUtils.isEmpty(openAPI.getGroup())) {
                        openAPI.setGroup(Constants.DEFAULT_GROUP);
                    }
                    openAPI.setConfig(configFactory.getConfig(openAPI.getGroup()));
                    String service = serviceMeta.getServiceInterface();
                    int index = service.lastIndexOf('.');
                    String tagName = index == -1 ? service : service.substring(index + 1);
                    openAPI.addTag(new Tag().setName(tagName).setDescription(service));
                    return openAPI;
                })
                .resolve(
                        new OpenAPI().setMeta(serviceMeta).setGlobalConfig(configFactory.getGlobalConfig()),
                        serviceMeta);
        if (definition == null) {
            return null;
        }
        if (definition.getConfig() == null) {
            definition.setConfig(configFactory.getConfig(definition.getGroup()));
        }

        if (CollectionUtils.isEmpty(definition.getServers())) {
            URL url = serviceMeta.getUrl();
            definition.addServer(new Server()
                    .setUrl("http://" + url.getHost() + ':' + url.getPort())
                    .setDescription(Constants.DUBBO_DEFAULT_SERVER));
        }

        OperationContext context = new OperationContextImpl(definition, schemaResolver, extensionFactory);
        for (List<Registration> registrations : registrationsByMethod) {
            String mainPath = null;
            for (Registration registration : registrations) {
                RequestMapping mapping = registration.getMapping();
                PathCondition pathCondition = mapping.getPathCondition();
                if (pathCondition == null) {
                    continue;
                }
                for (PathExpression expression : pathCondition.getExpressions()) {
                    String path = expression.toString();
                    PathItem pathItem = definition.getOrAddPath(path);
                    String ref = pathItem.getRef();
                    if (ref != null) {
                        path = ref;
                        pathItem = definition.getOrAddPath(path);
                    }
                    if (mainPath != null && expression.isDirect()) {
                        pathItem.setRef(mainPath);
                        continue;
                    }
                    MethodMeta methodMeta = registration.getMeta().getMethod();
                    if (resolvePath(path, pathItem, definition, methodMeta, mapping, context)) {
                        mainPath = path;
                    }
                }
            }
        }

        return definition;
    }

    private boolean resolvePath(
            String path,
            PathItem pathItem,
            OpenAPI openAPI,
            MethodMeta methodMeta,
            RequestMapping mapping,
            OperationContext context) {
        Collection<HttpMethods> httpMethods = null;
        for (OpenAPIDefinitionResolver resolver : resolvers) {
            httpMethods = resolver.resolve(pathItem, methodMeta, context);
            if (httpMethods != null) {
                break;
            }
        }
        if (httpMethods == null) {
            httpMethods = new LinkedList<>();
            for (String method : determineHttpMethods(openAPI, methodMeta, mapping)) {
                httpMethods.add(HttpMethods.of(method.toUpperCase()));
            }
        }

        boolean added = false;
        for (HttpMethods httpMethod : httpMethods) {
            Operation operation = new Operation().setMeta(methodMeta);
            Operation existingOperation = pathItem.getOperation(httpMethod);
            if (existingOperation != null && existingOperation.getMeta() != null) {
                LOG.internalWarn(
                        "Operation already exists, path='{}', httpMethod='{}', method={}",
                        path,
                        httpMethod,
                        methodMeta);
                continue;
            }
            operation = new OperationChainImpl(
                            resolvers, op -> resolveOperation(path, httpMethod, op, openAPI, methodMeta, mapping))
                    .resolve(operation, methodMeta, context);
            if (operation != null) {
                pathItem.addOperation(httpMethod, operation);
                added = true;
            }
        }
        return added;
    }

    private Collection<String> determineHttpMethods(OpenAPI openAPI, MethodMeta meta, RequestMapping mapping) {
        Collection<String> httpMethods = null;
        MethodsCondition condition = mapping.getMethodsCondition();
        if (condition != null) {
            httpMethods = condition.getMethods();
        }
        if (httpMethods == null) {
            String[] defaultHttpMethods = openAPI.getConfigValue(OpenAPIConfig::getDefaultHttpMethods);
            if (defaultHttpMethods == null) {
                httpMethods = Helper.guessHttpMethod(meta);
            } else {
                httpMethods = Arrays.asList(defaultHttpMethods);
            }
        }
        return httpMethods;
    }

    private Operation resolveOperation(
            String path,
            HttpMethods httpMethod,
            Operation operation,
            OpenAPI openAPI,
            MethodMeta meta,
            RequestMapping mapping) {
        if (operation.getGroup() == null) {
            operation.setGroup(openAPI.getGroup());
        }
        for (Tag tag : openAPI.getTags()) {
            operation.addTag(tag.getName());
        }
        if (operation.getDeprecated() == null && meta.isHierarchyAnnotated(Deprecated.class)) {
            operation.setDeprecated(true);
        }

        ServiceMeta serviceMeta = meta.getServiceMeta();
        if (serviceMeta.getServiceVersion() != null) {
            operation.addParameter(new Parameter(TripleHeaderEnum.SERVICE_GROUP.getName(), In.HEADER)
                    .setSchema(PrimitiveSchema.STRING.newSchema()));
        }
        if (serviceMeta.getServiceGroup() != null) {
            operation.addParameter(new Parameter(TripleHeaderEnum.SERVICE_VERSION.getName(), In.HEADER)
                    .setSchema(PrimitiveSchema.STRING.newSchema()));
        }

        List<String> variables = Helper.extractVariables(path);
        if (variables != null) {
            for (String variable : variables) {
                Parameter parameter = operation.getParameter(variable, In.PATH);
                if (parameter == null) {
                    parameter = new Parameter(variable, In.PATH);
                    operation.addParameter(parameter);
                }
                parameter.setRequired(true);
                if (parameter.getSchema() == null) {
                    parameter.setSchema(PrimitiveSchema.STRING.newSchema());
                }
            }
        }

        for (ParameterMeta paramMeta : meta.getParameters()) {
            resolveParameter(httpMethod, operation, paramMeta, true);
        }

        if (httpMethod.supportBody()) {
            RequestBody body = operation.getRequestBody();
            if (body == null) {
                body = new RequestBody();
                operation.setRequestBody(body);
            }
            if (CollectionUtils.isEmptyMap(body.getContents())) {
                resolveRequestBody(body, openAPI, meta, mapping);
            }
        }

        if (CollectionUtils.isEmptyMap(operation.getResponses())) {
            String[] httpStatusCodes = openAPI.getConfigValue(OpenAPIConfig::getDefaultHttpStatusCodes);
            if (httpStatusCodes == null) {
                httpStatusCodes = new String[] {"200", "400", "500"};
            }
            for (String httpStatusCode : httpStatusCodes) {
                ApiResponse response = operation.getOrAddResponse(httpStatusCode);
                resolveResponse(httpStatusCode, response, openAPI, meta, mapping);
            }
        }
        return operation;
    }

    private void resolveParameter(HttpMethods httpMethod, Operation operation, ParameterMeta meta, boolean traverse) {
        String name = meta.getName();
        if (name == null) {
            return;
        }

        NamedValueMeta valueMeta = meta.getNamedValueMeta();
        ParamType paramType = valueMeta.paramType();
        if (paramType == null) {
            if (httpMethod.supportBody()) {
                return;
            }
            paramType = ParamType.Param;
        }
        In in = Helper.toIn(paramType);
        if (in == null) {
            return;
        }

        boolean simple = meta.isSimple();
        if (in != In.QUERY && !simple) {
            return;
        }
        if (simple) {
            Parameter parameter = operation.getParameter(name, in);
            if (parameter == null) {
                parameter = new Parameter(name, in);
                operation.addParameter(parameter);
            }
            if (parameter.getRequired() == null) {
                parameter.setRequired(valueMeta.required());
            }
            Schema schema = parameter.getSchema();
            if (schema == null) {
                parameter.setSchema(schema = schemaResolver.resolve(meta));
            }
            if (schema.getDefaultValue() == null) {
                schema.setDefaultValue(valueMeta.defaultValue());
            }
            parameter.setMeta(meta);
            return;
        }
        if (!traverse) {
            return;
        }

        BeanMeta beanMeta = meta.getBeanMeta();
        try {
            for (ParameterMeta ctorParam : beanMeta.getConstructor().getParameters()) {
                resolveParameter(httpMethod, operation, ctorParam, false);
            }
        } catch (Throwable ignored) {
        }
        for (PropertyMeta property : beanMeta.getProperties()) {
            if ((property.getVisibility() & 0b001) == 0) {
                continue;
            }
            resolveParameter(httpMethod, operation, property, false);
        }
    }

    private void resolveRequestBody(RequestBody body, OpenAPI openAPI, MethodMeta meta, RequestMapping mapping) {
        Collection<MediaType> mediaTypes = null;
        if (mapping.getConsumesCondition() != null) {
            mediaTypes = mapping.getConsumesCondition().getMediaTypes();
        }
        if (mediaTypes == null) {
            String[] defaultMediaTypes = openAPI.getConfigValue(OpenAPIConfig::getDefaultConsumesMediaTypes);
            if (defaultMediaTypes == null) {
                mediaTypes = Collections.singletonList(MediaType.APPLICATION_JSON);
            } else {
                mediaTypes = Arrays.stream(defaultMediaTypes).map(MediaType::of).collect(Collectors.toList());
            }
        }
        out:
        for (MediaType mediaType : mediaTypes) {
            org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.MediaType content =
                    body.getOrAddContent(mediaType.getName());
            if (content.getSchema() == null) {
                for (ParameterMeta paramMeta : meta.getParameters()) {
                    ParamType paramType = paramMeta.getNamedValueMeta().paramType();
                    if (paramType == ParamType.Body) {
                        content.setSchema(schemaResolver.resolve(paramMeta));
                        continue out;
                    }
                }

                List<ParameterMeta> paramMetas = new ArrayList<>();
                for (ParameterMeta paramMeta : meta.getParameters()) {
                    if (paramMeta.getNamedValueMeta().paramType() == null) {
                        paramMetas.add(paramMeta);
                    }
                }
                int size = paramMetas.size();
                if (size == 0) {
                    continue;
                }
                if (size == 1) {
                    content.setSchema(schemaResolver.resolve(paramMetas.get(0)));
                } else {
                    content.setSchema(schemaResolver.resolve(paramMetas));
                }
            }
        }
    }

    private void resolveResponse(
            String httpStatusCode, ApiResponse response, OpenAPI openAPI, MethodMeta meta, RequestMapping mapping) {
        int httpStatus = Integer.parseInt(httpStatusCode);
        if (response.getDescription() == null) {
            response.setDescription(HttpUtils.getStatusMessage(httpStatus));
        }
        if (httpStatus > 201 && httpStatus < 400) {
            return;
        }
        if (meta.getActualReturnType() == void.class) {
            return;
        }

        Collection<MediaType> mediaTypes = null;
        if (mapping.getProducesCondition() != null) {
            mediaTypes = mapping.getProducesCondition().getMediaTypes();
        }
        if (mediaTypes == null) {
            String[] defaultMediaTypes = openAPI.getConfigValue(OpenAPIConfig::getDefaultProducesMediaTypes);
            if (defaultMediaTypes == null) {
                mediaTypes = Collections.singletonList(MediaType.APPLICATION_JSON);
            } else {
                mediaTypes = Arrays.stream(defaultMediaTypes).map(MediaType::of).collect(Collectors.toList());
            }
        }
        for (MediaType mediaType : mediaTypes) {
            org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.MediaType content =
                    response.getOrAddContent(mediaType.getName());
            if (content.getSchema() == null) {
                if (httpStatus >= 400) {
                    content.setSchema(schemaResolver.resolve(ErrorResponse.class));
                } else {
                    content.setSchema(schemaResolver.resolve(meta.getReturnParameter()));
                }
            }
        }
    }

    private static final class OperationContextImpl extends AbstractContext implements OperationContext {

        OperationContextImpl(OpenAPI openAPI, SchemaResolver schemaResolver, ExtensionFactory extensionFactory) {
            super(openAPI, schemaResolver, extensionFactory);
        }
    }

    private static final class OpenAPIChainImpl implements OpenAPIChain {

        private final OpenAPIDefinitionResolver[] resolvers;
        private final Function<OpenAPI, OpenAPI> fallback;
        private int cursor;

        OpenAPIChainImpl(OpenAPIDefinitionResolver[] resolvers, Function<OpenAPI, OpenAPI> fallback) {
            this.resolvers = resolvers;
            this.fallback = fallback;
        }

        @Override
        public OpenAPI resolve(OpenAPI openAPI, ServiceMeta serviceMeta) {
            if (cursor < resolvers.length) {
                return resolvers[cursor++].resolve(openAPI, serviceMeta, this);
            }
            return fallback.apply(openAPI);
        }
    }

    private static final class OperationChainImpl implements OperationChain {

        private final OpenAPIDefinitionResolver[] resolvers;
        private final Function<Operation, Operation> fallback;
        private int cursor;

        OperationChainImpl(OpenAPIDefinitionResolver[] resolvers, Function<Operation, Operation> fallback) {
            this.resolvers = resolvers;
            this.fallback = fallback;
        }

        @Override
        public Operation resolve(Operation operation, MethodMeta methodMeta, OperationContext chain) {
            if (cursor < resolvers.length) {
                return resolvers[cursor++].resolve(operation, methodMeta, chain, this);
            }
            return fallback.apply(operation);
        }
    }
}
