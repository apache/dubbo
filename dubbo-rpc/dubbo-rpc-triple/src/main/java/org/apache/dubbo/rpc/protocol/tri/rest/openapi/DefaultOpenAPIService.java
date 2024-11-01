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

import org.apache.dubbo.common.resource.Disposable;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.exception.UnsupportedMediaTypeException;
import org.apache.dubbo.remoting.http12.message.HttpMessageEncoder;
import org.apache.dubbo.remoting.http12.message.codec.JsonCodec;
import org.apache.dubbo.remoting.http12.message.codec.YamlCodec;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.Registration;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMapping;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMappingRegistry;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathExpression;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.HandlerMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.*;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;

public class DefaultOpenAPIService implements OpenAPIService, Disposable {

    private final List<AnnotationResolver> annotationResolvers;
    private final List<OpenAPIExtension> extensions;
    private final SchemaFactory schemaFactory = new SchemaFactory();
    private final Map<Class<?>, List<OpenAPIExtension>> extensionsCache = CollectionUtils.newConcurrentHashMap();

    private Map<Class<?>, OpenAPI> openAPIMap;
    private RequestMappingRegistry requestMappingRegistry;

    public DefaultOpenAPIService(FrameworkModel frameworkModel) {
        annotationResolvers = frameworkModel.getActivateExtensions(AnnotationResolver.class);
        extensions = frameworkModel.getActivateExtensions(OpenAPIExtension.class);
    }

    public void setRequestMappingRegistry(RequestMappingRegistry requestMappingRegistry) {
        this.requestMappingRegistry = requestMappingRegistry;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <S extends OpenAPIExtension> List<S> getExtensions(Class<S> clazz) {
        return (List) extensionsCache.computeIfAbsent(clazz, k -> {
            List<OpenAPIExtension> list = new ArrayList<>();
            for (OpenAPIExtension extension : extensions) {
                if (clazz.isInstance(extension)) {
                    list.add(extension);
                }
            }
            return list;
        });
    }

    @Override
    public OpenAPI getOpenAPI(OpenAPIRequest request) {
        Collection<Registration> registrations = requestMappingRegistry.getRegistrations();
        Map<Class<?>, ServiceMeta> serviceMetaMap = new HashMap<>();
        for (Registration registration : registrations) {
            ServiceMeta serviceMeta = registration.getMeta().getService();
            serviceMetaMap.putIfAbsent(serviceMeta.getType(), serviceMeta);
        }

        Map<Class<?>, OpenAPI> openAPIMap = new IdentityHashMap<>();
        out:
        for (Entry<Class<?>, ServiceMeta> entry : serviceMetaMap.entrySet()) {
            for (int i = 0, size = annotationResolvers.size(); i < size; i++) {
                OpenAPI openAPI = annotationResolvers.get(i).resolve(entry.getValue());
                if (openAPI == null) {
                    continue;
                }
                openAPIMap.put(entry.getKey(), openAPI);
                continue out;
            }
        }

        Map<Method, List<Registration>> registrationsGroupMap = new IdentityHashMap<>(registrations.size());
        for (Registration registration : registrations) {
            registrationsGroupMap
                    .computeIfAbsent(registration.getMeta().getMethod().getMethod(), k -> new ArrayList<>(1))
                    .add(registration);
        }

        ResolveContext context = new ResolveContext(schemaFactory);
        out:
        for (List<Registration> registrationsGroup : registrationsGroupMap.values()) {
            String mainPath = null;
            for (int i = 0, size = registrationsGroup.size(); i < size; i++) {
                Registration registration = registrationsGroup.get(i);
                HandlerMeta meta = registration.getMeta();
                OpenAPI openAPI = openAPIMap.get(meta.getService().getType());
                if (openAPI == null) {
                    continue out;
                }
                RequestMapping mapping = registration.getMapping();
                List<PathExpression> expressions = mapping.getPathCondition().getExpressions();
                for (int j = 0, len = expressions.size(); j < len; j++) {
                    PathExpression expression = expressions.get(j);
                    String path = Helper.resolvePath(expression);
                    PathItem pathItem = openAPI.getOrAddPath(path);
                    if (pathItem.getRef() != null) {
                        path = pathItem.getRef();
                        pathItem = openAPI.getOrAddPath(path);
                    }
                    if (mainPath == null) {
                        mainPath = path;
                        Set<String> methods = mapping.getMethodsCondition().getMethods();
                        for (String method : methods) {
                            HttpMethods httpMethod = HttpMethods.of(method);
                            Operation operation = pathItem.getOrAddOperation(httpMethod);
                            operation.setMeta(meta.getMethod());
                            resolveOperation(openAPI, httpMethod, operation, expression, mapping, meta, context);
                        }
                    } else {
                        pathItem.setRef(Helper.pathToRef(mainPath));
                    }
                }
            }
        }
        return null;
    }

    private void resolveOperation(
            OpenAPI openAPI,
            HttpMethods method,
            Operation operation,
            PathExpression expression,
            RequestMapping mapping,
            HandlerMeta meta,
            ResolveContext context) {
        operation.setOperationId(meta.getMethodDescriptor().getMethodName());
        for (ParameterMeta paramMeta : meta.getMethod().getParameters()) {
            Parameter parameter = resolveParameter(paramMeta);
            if (parameter != null) {
                operation.addParameter(parameter);
            }
        }
        resolveRequestBody(operation, mapping, meta);
        for (String httpStatus : new String[] {"200", "500"}) {
            resolveResponse(operation, httpStatus, mapping, meta);
        }
    }

    private Parameter resolveParameter(ParameterMeta paramMeta) {
        if (paramMeta == null) {
            return null;
        }
        Parameter parameter = new Parameter();
        parameter.setMeta(paramMeta);
        return parameter;
    }

    private void resolveRequestBody(Operation operation, RequestMapping mapping, HandlerMeta meta) {
        RequestBody body = new RequestBody();
        List<org.apache.dubbo.remoting.http12.message.MediaType> mediaTypes =
                mapping.getConsumesCondition().getMediaTypes();
        for (org.apache.dubbo.remoting.http12.message.MediaType mediaType : mediaTypes) {
            MediaType mediaTypeModel = new MediaType();
            mediaTypeModel.setSchema(resolveSchema(meta.getMethod().getMethod().getParameterTypes()[0]));
            body.addContent(mediaType.getName(), mediaTypeModel);
        }
        operation.setRequestBody(body);
    }

    private void resolveResponse(Operation operation, String httpStatus, RequestMapping mapping, HandlerMeta meta) {
        ApiResponse response = new ApiResponse();
        List<org.apache.dubbo.remoting.http12.message.MediaType> mediaTypes =
                mapping.getProducesCondition().getMediaTypes();
        for (org.apache.dubbo.remoting.http12.message.MediaType mediaType : mediaTypes) {
            MediaType mediaTypeModel = new MediaType();
            mediaTypeModel.setSchema(resolveSchema(meta.getMethod().getReturnType()));
            response.addContent(mediaType.getName(), mediaTypeModel);
        }
        operation.addResponse(httpStatus, response);
    }

    private Schema resolveSchema(Class<?> returnType) {
        return null;
    }

    @Override
    public String getDocument(OpenAPIRequest request) {
        Map<String, Object> document = new LinkedHashMap<>();
        OpenAPI openAPI = getOpenAPI(request);
        openAPI.writeTo(document, new WriteContext() {});

        HttpMessageEncoder encoder = getEncoder(request);
        ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
        encoder.encode(os, document, StandardCharsets.UTF_8);
        return new String(os.toByteArray(), StandardCharsets.UTF_8);
    }

    private static HttpMessageEncoder getEncoder(OpenAPIRequest request) {
        String format = request.getFormat();
        format = format == null ? "json" : format.toLowerCase();
        HttpMessageEncoder encoder;
        switch (format) {
            case "json":
                encoder = JsonCodec.INSTANCE;
                break;
            case "yml":
            case "yaml":
                encoder = YamlCodec.INSTANCE;
                break;
            case "proto":
                encoder = ProtoEncoder.INSTANCE;
                break;
            default:
                throw new UnsupportedMediaTypeException("application/" + format);
        }
        return encoder;
    }

    @Override
    public void refresh() {
        this.openAPIMap = null;
    }

    @Override
    public void export() {}

    @Override
    public void destroy() {
    }
}
