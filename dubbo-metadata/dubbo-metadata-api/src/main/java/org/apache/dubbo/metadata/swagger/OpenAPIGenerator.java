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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.swagger.model.Components;
import org.apache.dubbo.metadata.swagger.model.OpenAPI;
import org.apache.dubbo.metadata.swagger.model.Operation;
import org.apache.dubbo.metadata.swagger.model.PathItem;
import org.apache.dubbo.metadata.swagger.model.PathItem.HttpMethod;
import org.apache.dubbo.metadata.swagger.model.Paths;
import org.apache.dubbo.metadata.swagger.model.media.StringSchema;
import org.apache.dubbo.metadata.swagger.model.parameters.Parameter;
import org.apache.dubbo.metadata.swagger.model.responses.ApiResponses;
import org.apache.dubbo.metadata.swagger.utils.PathUtils;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.DefaultRequestMappingRegistry;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMapping;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathExpression;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.HandlerMeta;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.dubbo.metadata.swagger.SwaggerConstants.DEFAULT_CONSUMES_MEDIA_TYPE;
import static org.apache.dubbo.metadata.swagger.SwaggerConstants.DEFAULT_PRODUCES_MEDIA_TYPE;
import static org.apache.dubbo.metadata.swagger.SwaggerConstants.PATH;
import static org.apache.dubbo.metadata.swagger.SwaggerConstants.QUERY;
import static org.apache.dubbo.metadata.swagger.model.PathItem.HttpMethod.DELETE;
import static org.apache.dubbo.metadata.swagger.model.PathItem.HttpMethod.GET;
import static org.apache.dubbo.metadata.swagger.model.PathItem.HttpMethod.PATCH;
import static org.apache.dubbo.metadata.swagger.model.PathItem.HttpMethod.POST;
import static org.apache.dubbo.metadata.swagger.model.PathItem.HttpMethod.PUT;

public class OpenAPIGenerator {
    private final Logger LOGGER = LoggerFactory.getLogger(OpenAPIGenerator.class);
    private final Lock reentrantLock = new ReentrantLock();
    private final DefaultRequestMappingRegistry defaultRequestMappingRegistry;
    private final OpenAPIService openAPIService;
    private final ResponseService responseBuilder;
    private final RequestService requestBuilder;

    private final Pattern pathPattern = Pattern.compile("\\{(.*?)}");

    public OpenAPIGenerator(
            DefaultRequestMappingRegistry defaultRequestMappingRegistry,
            RequestService requestService,
            OpenAPIService openAPIService,
            ResponseService responseBuilder) {
        super();
        this.openAPIService = openAPIService;
        this.responseBuilder = responseBuilder;
        this.requestBuilder = requestService;
        this.defaultRequestMappingRegistry = defaultRequestMappingRegistry;
    }

    public OpenAPI getOpenApi(Locale locale) {
        this.reentrantLock.lock();
        try {
            final OpenAPI openAPI;
            final Locale finalLocale = locale == null ? Locale.getDefault() : locale;
            if (openAPIService.getCachedOpenAPI(finalLocale) == null) {
                Instant start = Instant.now();

                openAPI = openAPIService.build();

                this.getPaths(finalLocale, openAPI);

                openAPIService.setServersPresent(!CollectionUtils.isEmpty(openAPI.getServers()));

                openAPIService.updateServers(openAPI);

                openAPIService.setCachedOpenAPI(openAPI, finalLocale);

                LOGGER.info(
                        "Init duration for springdoc-openapi is: {} ms",
                        Duration.between(start, Instant.now()).toMillis());
            } else {
                LOGGER.debug("Fetching openApi document from cache");
                openAPI = openAPIService.getCachedOpenAPI(finalLocale);
                openAPIService.updateServers(openAPI);
            }
            openAPIService.updateServers(openAPI);
            return openAPI;
        } finally {
            this.reentrantLock.unlock();
        }
    }

    protected void getPaths(Locale locale, OpenAPI openAPI) {
        Map<RequestMapping, HandlerMeta> map = defaultRequestMappingRegistry.getRegistrations();
        this.calculatePath(map, locale, openAPI);
    }

    private void calculatePath(Map<RequestMapping, HandlerMeta> map, Locale locale, OpenAPI openAPI) {
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
                            .map(mediaType -> mediaType.getType() + "/" + mediaType.getSubType())
                            .toArray(String[]::new);

                    String[] consumes = requestMappingInfo.getConsumesCondition().getMediaTypes().stream()
                            .map(mediaType -> mediaType.getType() + "/" + mediaType.getSubType())
                            .toArray(String[]::new);

                    String[] headers = requestMappingInfo.getHeadersCondition().getExpressions().stream()
                            .map(Object::toString)
                            .toArray(String[]::new);
                    String[] params = requestMappingInfo.getParamsCondition().getExpressions().stream()
                            .map(Object::toString)
                            .toArray(String[]::new);

                    Set<String> requestMethods =
                            entry.getKey().getMethodsCondition().getMethods();
                    if (requestMethods.isEmpty()) {
                        requestMethods = this.getDefaultAllowedHttpMethods();
                    }

                    calculatePath(
                            handlerMeta,
                            operationPath,
                            requestMethods,
                            consumes,
                            produces,
                            headers,
                            params,
                            locale,
                            openAPI);
                }
            }
        }
    }

    private void calculatePath(
            HandlerMeta handlerMeta,
            String operationPath,
            Set<String> requestMethods,
            String[] consumes,
            String[] produces,
            String[] headers,
            String[] params,
            Locale locale,
            OpenAPI openAPI) {
        this.calculatePath(
                handlerMeta,
                new RouterOperation(
                        operationPath,
                        requestMethods.toArray(new HttpMethod[requestMethods.size()]),
                        consumes,
                        produces,
                        headers,
                        params),
                locale,
                openAPI);
    }

    private void calculatePath(
            HandlerMeta handlerMeta, RouterOperation routerOperation, Locale locale, OpenAPI openAPI) {
        String operationPath = routerOperation.getPath();
        Set<HttpMethod> httpMethods = new TreeSet<>(Arrays.asList(routerOperation.getMethods()));
        String[] methodConsumes = routerOperation.getConsumes();
        String[] methodProduces = routerOperation.getProduces();
        String[] headers = routerOperation.getHeaders();
        Map<String, String> queryParams = routerOperation.getQueryParams();

        Components components = openAPI.getComponents();
        Paths paths = openAPI.getPaths();

        Map<HttpMethod, Operation> operationMap = null;
        if (paths.containsKey(operationPath)) {
            PathItem pathItem = paths.get(operationPath);
            operationMap = pathItem.readOperationsMap();
        }

        for (HttpMethod httpMethod : httpMethods) {
            Operation existingOperation = this.getExistingOperation(operationMap, httpMethod);
            Method method = handlerMeta.getMethod().getMethod();
            MethodAttributes methodAttributes = new MethodAttributes(
                    DEFAULT_CONSUMES_MEDIA_TYPE,
                    DEFAULT_PRODUCES_MEDIA_TYPE,
                    methodConsumes,
                    methodProduces,
                    headers,
                    locale);
            methodAttributes.setMethodOverloaded(existingOperation != null);

            methodAttributes.calculateConsumesProduces(method);

            Operation operation = (existingOperation != null) ? existingOperation : new Operation();

            this.fillParametersList(operation, queryParams, methodAttributes);

            // RequestBody in Operation
            requestBuilder
                    .getRequestBodyBuilder()
                    .buildRequestBodyFromDoc(methodAttributes, components, locale)
                    .ifPresent(operation::setRequestBody);

            // request
            operation = requestBuilder.build(handlerMeta, httpMethod, operation, methodAttributes, openAPI);

            // response
            ApiResponses apiResponses = responseBuilder.build(components, handlerMeta, operation, methodAttributes);
            operation.setResponses(apiResponses);

            PathItem pathItemObject = this.buildPathItem(httpMethod, operation, operationPath, paths);

            if (!StringUtils.contains(operationPath, "**")) {
                if (StringUtils.contains(operationPath, "*")) {
                    Matcher matcher = pathPattern.matcher(operationPath);
                    while (matcher.find()) {
                        String pathParam = matcher.group(1);
                        String newPathParam = pathParam.replace("*", "");
                        operationPath = operationPath.replace("{" + pathParam + "}", "{" + newPathParam + "}");
                    }
                }
                paths.addPathItem(operationPath, pathItemObject);
            }
        }
    }

    private void fillParametersList(
            Operation operation, Map<String, String> queryParams, MethodAttributes methodAttributes) {
        List<Parameter> parametersList = operation.getParameters();
        if (parametersList == null) {
            parametersList = new ArrayList<>();
        }
        Collection<Parameter> headersMap = RequestService.getHeaders(methodAttributes, new LinkedHashMap<>());
        headersMap.forEach(parameter -> {
            boolean exists = false;
            if (!CollectionUtils.isEmpty(operation.getParameters())) {
                for (Parameter p : operation.getParameters()) {
                    if (parameter.getName().equals(p.getName())) {
                        exists = true;
                        break;
                    }
                }
            }
            if (!exists) {
                operation.addParametersItem(parameter);
            }
        });
        if (!CollectionUtils.isEmptyMap(queryParams)) {
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                Parameter parameter = new Parameter();
                parameter.setName(entry.getKey());
                parameter.setSchema(new StringSchema()._default(entry.getValue()));
                parameter.setRequired(true);
                parameter.setIn(QUERY);
                ParameterService.mergeParameter(parametersList, parameter);
            }
            operation.setParameters(parametersList);
        }
    }

    private PathItem buildPathItem(HttpMethod httpMethod, Operation operation, String operationPath, Paths paths) {
        PathItem pathItemObject = null;
        if (operation != null && !CollectionUtils.isEmpty(operation.getParameters())) {
            Iterator<Parameter> paramIt = operation.getParameters().iterator();
            while (paramIt.hasNext()) {
                Parameter parameter = paramIt.next();
                if (PATH.equals(parameter.getIn())) {
                    // check it's present in the path
                    String name = parameter.getName();
                    if (!StringUtils.containsAny(operationPath, "{" + name + "}", "{*" + name + "}")) {
                        paramIt.remove();
                    }
                }
            }
        }
        if (paths.containsKey(operationPath)) {
            pathItemObject = paths.get(operationPath);
        } else {
            pathItemObject = new PathItem();
        }

        switch (httpMethod) {
            case POST:
                pathItemObject.post(operation);
                break;
            case GET:
                pathItemObject.get(operation);
                break;
            case DELETE:
                pathItemObject.delete(operation);
                break;
            case PUT:
                pathItemObject.put(operation);
                break;
            case PATCH:
                pathItemObject.patch(operation);
                break;
            case TRACE:
                pathItemObject.trace(operation);
                break;
            case HEAD:
                pathItemObject.head(operation);
                break;
            case OPTIONS:
                pathItemObject.options(operation);
                break;
            default:
                // Do nothing here
                break;
        }
        return pathItemObject;
    }

    private Operation getExistingOperation(Map<HttpMethod, Operation> operationMap, HttpMethod httpMethod) {
        Operation existingOperation = null;
        if (!CollectionUtils.isEmptyMap(operationMap)) {
            // Get existing operation definition
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

    protected Set<String> getDefaultAllowedHttpMethods() {
        String[] allowedRequestMethods = {
            GET.toString(),
            POST.toString(),
            PUT.toString(),
            PATCH.toString(),
            DELETE.toString(),
            HttpMethods.OPTIONS.toString(),
            HttpMethods.HEAD.toString()
        };
        return new HashSet<>(Arrays.asList(allowedRequestMethods));
    }
}
