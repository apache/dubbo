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
package org.apache.dubbo.metadata.swagger;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.metadata.swagger.model.Components;
import org.apache.dubbo.metadata.swagger.model.OpenAPI;
import org.apache.dubbo.metadata.swagger.model.Operation;
import org.apache.dubbo.metadata.swagger.model.PathItem;
import org.apache.dubbo.metadata.swagger.model.PathItem.HttpMethod;
import org.apache.dubbo.metadata.swagger.model.Paths;
import org.apache.dubbo.metadata.swagger.model.RouterOperation;
import org.apache.dubbo.metadata.swagger.model.annotations.ParameterIn;
import org.apache.dubbo.metadata.swagger.model.parameters.Parameter;
import org.apache.dubbo.metadata.swagger.model.responses.ApiResponses;
import org.apache.dubbo.metadata.swagger.utils.PathUtils;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.DefaultRequestMappingRegistry;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMapping;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathExpression;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.HandlerMeta;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;

import static org.apache.dubbo.metadata.swagger.model.PathItem.HttpMethod.DELETE;
import static org.apache.dubbo.metadata.swagger.model.PathItem.HttpMethod.GET;
import static org.apache.dubbo.metadata.swagger.model.PathItem.HttpMethod.PATCH;
import static org.apache.dubbo.metadata.swagger.model.PathItem.HttpMethod.POST;
import static org.apache.dubbo.metadata.swagger.model.PathItem.HttpMethod.PUT;

public class OpenAPIGenerator {

    /**
     * The Reentrant lock.
     */
    private final Lock reentrantLock = new ReentrantLock();

    /**
     * The Open api builder.
     */
    protected final OpenAPIService openAPIService;

    /**
     * The Request builder.
     */
    private final RequestService requestBuilder;

    /**
     * The Response builder
     */
    private final ResponseService responseBuilder;

    protected OpenAPIGenerator(
            OpenAPIService openAPIService, RequestService requestBuilder, ResponseService responseBuilder) {
        super();
        this.openAPIService = openAPIService;
        this.requestBuilder = requestBuilder;
        this.responseBuilder = responseBuilder;
    }

    public OpenAPIGenerator() {
        super();
        this.openAPIService = new OpenAPIService();
        this.requestBuilder = new RequestService();
        this.responseBuilder = new ResponseService();
    }

    private DefaultRequestMappingRegistry defaultRequestMappingRegistry;

    private static final String DEFAULT_CONSUMES_MEDIA_TYPE = "application/json";
    private static final String DEFAULT_PRODUCES_MEDIA_TYPE = "*/*";

    public OpenAPI getOpenApi() {
        this.reentrantLock.lock();
        try {
            final OpenAPI openAPI;
            final Locale finalLocale = Locale.getDefault();
            if (openAPIService.getCachedOpenAPI(finalLocale) == null) {
                // initialize openAPI
                openAPI = openAPIService.build(finalLocale);

                getPaths(openAPI);

                openAPIService.updateServers(openAPI);

                openAPIService.setCachedOpenAPI(openAPI, finalLocale);
            } else {
                openAPI = openAPIService.getCachedOpenAPI(finalLocale);
                openAPIService.updateServers(openAPI);
            }
            openAPIService.updateServers(openAPI);
            return openAPI;
        } finally {
            this.reentrantLock.unlock();
        }
    }

    private void getPaths(OpenAPI openAPI) {
        Map<RequestMapping, HandlerMeta> map = defaultRequestMappingRegistry.getRegistrations();
        calculatePath(map, openAPI);
    }

    private void calculatePath(Map<RequestMapping, HandlerMeta> map, OpenAPI openAPI) {
        for (Entry<RequestMapping, HandlerMeta> entry : map.entrySet()) {
            RequestMapping requestMappingInfo = entry.getKey();
            HandlerMeta handlerMeta = entry.getValue();
            List<PathExpression> patterns =
                    requestMappingInfo.getPathCondition().getExpressions();
            if (!CollectionUtils.isEmpty(patterns)) {
                Map<String, String> regexMap = new LinkedHashMap<>();
                for (PathExpression pattern : patterns) {

                    String operationPath = PathUtils.parsePath(pattern.toString(), regexMap);

                    String[] produces = requestMappingInfo.getProducesCondition().getMediaTypes().stream()
                            .map(MediaType::toString)
                            .toArray(String[]::new);

                    Set<String> requestMethods =
                            entry.getKey().getMethodsCondition().getMethods();
                    if (requestMethods.isEmpty()) {
                        requestMethods = this.getDefaultAllowedHttpMethods();
                    }
                    calculatePath(
                            handlerMeta,
                            new RouterOperation(
                                    operationPath,
                                    requestMethods.toArray(new HttpMethod[requestMethods.size()]),
                                    produces),
                            openAPI);
                }
            }
        }
    }

    private void calculatePath(HandlerMeta handlerMethod, RouterOperation routerOperation, OpenAPI openAPI) {
        String operationPath = routerOperation.getPath();
        Set<HttpMethod> httpMethods = new TreeSet<>(Arrays.asList(routerOperation.getMethods()));
        String[] methodProduces = routerOperation.getProduces();

        Components components = openAPI.getComponents();
        Paths paths = openAPI.getPaths();

        Map<HttpMethod, Operation> operationMap = null;
        if (paths.containsKey(operationMap)) {
            PathItem pathItem = paths.get(operationPath);
            operationMap = pathItem.readOperationsMap();
        }

        for (HttpMethod httpMethod : httpMethods) {
            Operation existingOperation = getExistingOperation(operationMap, httpMethod);

            MethodAttributes methodAttributes =
                    new MethodAttributes(DEFAULT_CONSUMES_MEDIA_TYPE, DEFAULT_PRODUCES_MEDIA_TYPE, methodProduces);

            methodAttributes.fillMethod();

            Operation operation = (existingOperation != null) ? existingOperation : new Operation();

            // request
            operation = requestBuilder.build(handlerMethod, httpMethod, operation, methodAttributes, openAPI);

            // response
            ApiResponses apiResponses = responseBuilder.build(components, handlerMethod, operation, methodAttributes);
            operation.setResponses(apiResponses);

            PathItem pathItemObject = buildPathItem(httpMethod, operation, operationPath, paths);
            paths.addPathItem(operationPath, pathItemObject);
        }
    }

    private PathItem buildPathItem(HttpMethod httpMethod, Operation operation, String operationPath, Paths paths) {
        PathItem pathItem;
        if (operation != null && !CollectionUtils.isEmpty(operation.getParameters())) {
            Iterator<Parameter> parameterIterator = operation.getParameters().iterator();
            while (parameterIterator.hasNext()) {
                Parameter parameter = parameterIterator.next();
                if (ParameterIn.PATH.toString().equals(parameter.getIn())) {
                    String name = parameter.getName();
                    // check it's present in the path
                    if (!StringUtils.containsAny(operationPath, "{" + name + "}", "{*" + name + "}"))
                        parameterIterator.remove();
                }
            }
        }

        if (paths.containsKey(operationPath)) pathItem = paths.get(operationPath);
        else pathItem = new PathItem();
        switch (httpMethod) {
            case POST:
                pathItem.post(operation);
                break;
            case GET:
                pathItem.get(operation);
                break;
            case DELETE:
                pathItem.delete(operation);
                break;
            case PUT:
                pathItem.put(operation);
                break;
            case PATCH:
                pathItem.patch(operation);
                break;
            case TRACE:
                pathItem.trace(operation);
                break;
            case HEAD:
                pathItem.head(operation);
                break;
            case OPTIONS:
                pathItem.options(operation);
                break;
            default:
                // Do nothing here
                break;
        }
        return pathItem;
    }

    private Operation getExistingOperation(Map<HttpMethod, Operation> operationMap, HttpMethod httpMethod) {
        Operation existingOperation = null;
        if (!CollectionUtils.isEmptyMap(operationMap)) {
            switch (httpMethod) {
                case GET:
                    existingOperation = operationMap.get(HttpMethod.GET);
                    break;
                case POST:
                    existingOperation = operationMap.get(HttpMethod.POST);
                    break;
                case PUT:
                    existingOperation = operationMap.get(HttpMethod.PUT);
                    break;
                case DELETE:
                    existingOperation = operationMap.get(HttpMethod.DELETE);
                    break;
                case PATCH:
                    existingOperation = operationMap.get(HttpMethod.PATCH);
                    break;
                case HEAD:
                    existingOperation = operationMap.get(HttpMethod.HEAD);
                    break;
                case OPTIONS:
                    existingOperation = operationMap.get(HttpMethod.OPTIONS);
                    break;
                default:
                    // Do nothing here
                    break;
            }
        }
        return existingOperation;
    }

    /**
     * Gets default allowed http methods.
     *
     * @return the default allowed http methods
     */
    protected Set<String> getDefaultAllowedHttpMethods() {
        String[] allowedRequestMethods = {
            GET.toString(),
            POST.toString(),
            PUT.toString(),
            PATCH.toString(),
            DELETE.toString(),
            HttpMethod.OPTIONS.toString(),
            HttpMethod.HEAD.toString()
        };
        return new HashSet<>(Arrays.asList(allowedRequestMethods));
    }
}
