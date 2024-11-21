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
import org.apache.dubbo.remoting.http12.rest.ParamType;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.Registration;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMapping;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathCondition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathExpression;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.BeanMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.BeanMeta.PropertyMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.NamedValueMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.ApiResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.OpenAPI;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Operation;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Parameter;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Parameter.In;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.PathItem;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.RequestBody;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

final class DefinitionResolver {

    private static final FluentLogger LOG = FluentLogger.of(DefaultOpenAPIService.class);

    private final ExtensionFactory extensionFactory;
    private final ConfigFactory configFactory;
    private final SchemaFactory schemaFactory;
    private final OpenAPIDefinitionResolver[] resolvers;

    DefinitionResolver(FrameworkModel frameworkModel) {
        extensionFactory = frameworkModel.getOrRegisterBean(ExtensionFactory.class);
        configFactory = frameworkModel.getOrRegisterBean(ConfigFactory.class);
        schemaFactory = frameworkModel.getOrRegisterBean(SchemaFactory.class);
        resolvers = extensionFactory.getExtensions(OpenAPIDefinitionResolver.class);
    }

    public OpenAPI resolve(ServiceMeta serviceMeta, Collection<List<Registration>> registrationsByMethod) {
        OpenAPI openAPI = null;

        for (OpenAPIDefinitionResolver resolver : resolvers) {
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
        openAPI.setMeta(serviceMeta);

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
                    String path = expression.toString();
                    PathItem pathItem = openAPI.getOrAddPath(path);
                    String ref = pathItem.getRef();
                    if (ref != null) {
                        path = ref;
                        pathItem = openAPI.getOrAddPath(path);
                    }
                    if (mainPath != null && expression.isDirect()) {
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

        for (OpenAPIDefinitionResolver resolver : resolvers) {
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
            Operation existingOperation = pathItem.getOperation(httpMethod);
            if (existingOperation == null) {
                if (operation == null) {
                    operation = new Operation();
                }
                pathItem.addOperation(httpMethod, operation);
            } else {
                if (existingOperation.getMeta() != null) {
                    LOG.internalWarn("Operation already exists, path='{}', httpMethod='{}', method={}", path, hm, meta);
                }
                continue;
            }
            operation.setMeta(meta);
            resolveOperation(path, httpMethod, operation, openAPI, meta, mapping);
        }

        return true;
    }

    private void resolveOperation(
            String path,
            HttpMethods httpMethod,
            Operation operation,
            OpenAPI openAPI,
            MethodMeta meta,
            RequestMapping mapping) {
        if (operation.getOperationId() == null) {
            operation.setOperationId(meta.getMethod().getName());
        }
        if (operation.getDeprecated() == null && meta.isHierarchyAnnotated(Deprecated.class)) {
            operation.setDeprecated(true);
        }

        for (int i = 0, len = path.length(), start = 0; i < len; i++) {
            char c = path.charAt(i);
            if (c == '{') {
                start = i + 1;
            } else if (start > 0 && c == '}') {
                String name = path.substring(start, i);
                Parameter parameter = operation.getParameter(name, In.PATH);
                if (parameter == null) {
                    parameter = new Parameter(name, In.PATH);
                    operation.addParameter(parameter);
                }
                parameter.setRequired(true);
                if (parameter.getSchema() == null) {
                    parameter.setSchema(PrimitiveSchema.STRING.newSchema());
                }
                start = 0;
            }
        }

        for (ParameterMeta paramMeta : meta.getParameters()) {
            resolveParameter(operation, paramMeta, true);
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

    private void resolveParameter(Operation operation, ParameterMeta paramMeta, boolean traverse) {
        String name = paramMeta.getName();
        if (name == null) {
            return;
        }

        NamedValueMeta valueMeta = paramMeta.getNamedValueMeta();
        In in = Helper.toIn(valueMeta.paramType());
        if (in == null) {
            return;
        }

        boolean simple = paramMeta.isSimple();
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
                parameter.setSchema(schema = schemaFactory.getSchema(paramMeta));
            }
            if (schema.getDefaultValue() == null) {
                schema.setDefaultValue(valueMeta.defaultValue());
            }
            parameter.setMeta(paramMeta);
            return;
        }
        if (!traverse) {
            return;
        }

        BeanMeta beanMeta = paramMeta.getBeanMeta();
        try {
            for (ParameterMeta ctorParam : beanMeta.getConstructor().getParameters()) {
                resolveParameter(operation, ctorParam, false);
            }
        } catch (Throwable ignored) {
        }
        for (PropertyMeta property : beanMeta.getProperties()) {
            if ((property.getVisibility() & 0b001) == 0) {
                continue;
            }
            resolveParameter(operation, property, false);
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
                        content.setSchema(schemaFactory.getSchema(paramMeta));
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
                    content.setSchema(schemaFactory.getSchema(paramMetas.get(0)));
                } else {
                    content.setSchema(schemaFactory.getSchema(paramMetas));
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
                    content.setSchema(schemaFactory.getSchema(ErrorResponse.class));
                } else {
                    content.setSchema(schemaFactory.getSchema(meta.getReturnParameter()));
                }
            }
        }
    }

    static final class ResolveContextImpl extends AbstractContext implements ResolveContext {

        ResolveContextImpl(OpenAPI openAPI, SchemaFactory schemaFactory, ExtensionFactory extensionFactory) {
            super(openAPI, schemaFactory, extensionFactory);
        }
    }
}
