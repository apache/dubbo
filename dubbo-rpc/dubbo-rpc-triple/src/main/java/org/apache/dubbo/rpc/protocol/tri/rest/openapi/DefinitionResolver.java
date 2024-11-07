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

import org.apache.dubbo.common.logger.FluentLogger;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.nested.OpenAPIConfig;
import org.apache.dubbo.remoting.http12.ErrorResponse;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.HttpUtils;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.Registration;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMapping;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathCondition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathExpression;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.ApiResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.OpenAPI;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Operation;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Parameter;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Parameter.In;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.PathItem;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.RequestBody;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

final class DefinitionResolver {

    private static final FluentLogger LOG = FluentLogger.of(DefaultOpenAPIService.class);

    private final List<AnnotationResolver> annotationResolvers;
    private final ExtensionFactory extensionFactory;
    private final ConfigFactory configFactory;
    private final SchemaFactory schemaFactory;

    DefinitionResolver(FrameworkModel frameworkModel) {
        annotationResolvers = frameworkModel.getActivateExtensions(AnnotationResolver.class);
        extensionFactory = frameworkModel.getOrRegisterBean(ExtensionFactory.class);
        configFactory = frameworkModel.getOrRegisterBean(ConfigFactory.class);
        schemaFactory = frameworkModel.getOrRegisterBean(SchemaFactory.class);
    }

    public OpenAPI resolve(ServiceMeta serviceMeta, Collection<List<Registration>> registrationsByMethod) {
        OpenAPI openAPI = null;
        List<AnnotationResolver> resolvers = annotationResolvers;
        for (int i = 0, size = resolvers.size(); i < size; i++) {
            AnnotationResolver resolver = resolvers.get(i);
            if (resolver.hidden(serviceMeta)) {
                return null;
            }
            openAPI = resolver.resolve(serviceMeta);
            if (openAPI != null) {
                break;
            }
        }
        if (openAPI == null) {
            openAPI = new OpenAPI();
        }
        if (StringUtils.isEmpty(openAPI.getGroup())) {
            openAPI.setGroup(Constants.DEFAULT_GROUP);
        }
        openAPI.setGlobalConfig(configFactory.getGlobalConfig());
        openAPI.setConfig(configFactory.getConfig(openAPI.getGroup()));
        openAPI.setService(serviceMeta);

        ResolveContext context = new ResolveContextImpl(openAPI, schemaFactory, extensionFactory);
        for (List<Registration> registrations : registrationsByMethod) {
            String mainPath = null;
            for (Registration registration : registrations) {
                RequestMapping mapping = registration.getMapping();
                PathCondition pathCondition = mapping.getPathCondition();
                if (pathCondition == null) {
                    continue;
                }
                for (PathExpression expression : pathCondition.getExpressions()) {
                    String path = Helper.toPathValue(expression);
                    PathItem pathItem = openAPI.getOrAddPath(path);
                    String ref = pathItem.getRef();
                    if (ref != null) {
                        path = ref;
                        pathItem = openAPI.getOrAddPath(path);
                    }
                    if (mainPath != null) {
                        pathItem.setRef(mainPath);
                        continue;
                    }
                    MethodMeta methodMeta = registration.getMeta().getMethod();
                    if (resolvePath(path, pathItem, openAPI, methodMeta, mapping, context)) {
                        mainPath = path;
                    }
                }
            }
        }

        return openAPI;
    }

    private boolean resolvePath(
            String path,
            PathItem pathItem,
            OpenAPI openAPI,
            MethodMeta meta,
            RequestMapping mapping,
            ResolveContext context) {
        Operation operation = null;
        Collection<String> httpMethods = null;
        List<AnnotationResolver> resolvers = annotationResolvers;
        for (int i = 0, size = resolvers.size(); i < size; i++) {
            AnnotationResolver resolver = resolvers.get(i);
            if (resolver.hidden(meta, openAPI, context)) {
                return false;
            }
            operation = resolver.resolve(meta, openAPI, context);
            if (operation == null) {
                continue;
            }
            if (operation.getHttpMethod() != null) {
                httpMethods =
                        Collections.singletonList(operation.getHttpMethod().name());
            }
        }

        if (httpMethods == null) {
            if (mapping.getMethodsCondition() != null) {
                httpMethods = mapping.getMethodsCondition().getMethods();
            }
            if (httpMethods == null) {
                String[] defaultHttpMethods = openAPI.getConfigValue(OpenAPIConfig::getDefaultHttpMethods);
                if (defaultHttpMethods == null) {
                    httpMethods = Helper.guessHttpMethod(meta);
                } else {
                    httpMethods = Arrays.asList(defaultHttpMethods);
                }
            }
        }

        for (String hm : httpMethods) {
            HttpMethods httpMethod = HttpMethods.of(hm.toUpperCase());
            Operation existsOperation = pathItem.getOperation(httpMethod);
            if (existsOperation == null) {
                if (operation == null) {
                    operation = new Operation();
                }
                pathItem.addOperation(httpMethod, operation);
            } else {
                if (existsOperation.getMethod() != null) {
                    LOG.internalWarn("Operation already exists, path='{}', httpMethod='{}', method={}", path, hm, meta);
                }
                continue;
            }
            operation.setMethod(meta);
            resolveOperation(httpMethod, operation, openAPI, meta, mapping);
        }
        return true;
    }

    private void resolveOperation(
            HttpMethods httpMethod, Operation operation, OpenAPI openAPI, MethodMeta meta, RequestMapping mapping) {
        if (operation.getOperationId() == null) {
            String operationId = generateOperationId(meta, openAPI);
            operation.setOperationId(operationId == null ? meta.getMethod().getName() : operationId);
        }
        if (operation.getDeprecated() == null && meta.isHierarchyAnnotated(Deprecated.class)) {
            operation.setDeprecated(true);
        }
        if (operation.getGroup() == null) {
            operation.setGroup(openAPI.getGroup());
        }

        if (CollectionUtils.isEmpty(operation.getParameters())) {
            for (ParameterMeta paramMeta : meta.getParameters()) {
                String name = paramMeta.getName();
                if (name == null) {
                    continue;
                }
                In in = Helper.toIn(paramMeta.getParamType());
                if (in == null) {
                    continue;
                }
                Parameter parameter = operation.getParameter(name, in);
                if (parameter == null) {
                    parameter = new Parameter(name, in);
                    operation.addParameter(parameter);
                }
                resolveParameter(parameter, paramMeta);
            }
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
    }

    private String generateOperationId(MethodMeta meta, OpenAPI openAPI) {
        String name = openAPI.getConfigValue(OpenAPIConfig::getOperationIdStrategy);
        if (name == null) {
            return null;
        }
        NamingStrategy strategy = extensionFactory.getExtension(NamingStrategy.class, "naming-strategy-" + name);
        if (strategy == null) {
            return null;
        }
        return strategy.generateOperationId(meta, openAPI);
    }

    private void resolveParameter(Parameter parameter, ParameterMeta meta) {
        if (parameter.getSchema() == null) {
            parameter.setSchema(schemaFactory.getSchema(meta.getActualGenericType()));
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
        for (MediaType mediaType : mediaTypes) {
            org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.MediaType content =
                    body.getOrAddContent(mediaType.getName());
            if (content.getSchema() == null) {
                content.setSchema(schemaFactory.getSchema(meta.getParameters()));
            }
        }
    }

    private void resolveResponse(
            String httpStatusCode, ApiResponse response, OpenAPI openAPI, MethodMeta meta, RequestMapping mapping) {
        int httpStatus = Integer.parseInt(httpStatusCode);
        if (httpStatus > 201 && httpStatus < 400) {
            response.setDescription(HttpUtils.getStatusMessage(httpStatus));
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
                    content.setSchema(schemaFactory.getSchema(ErrorResponse.class));
                } else {
                    content.setSchema(schemaFactory.getSchema(meta.getParameters()));
                }
            }
        }
    }
}
