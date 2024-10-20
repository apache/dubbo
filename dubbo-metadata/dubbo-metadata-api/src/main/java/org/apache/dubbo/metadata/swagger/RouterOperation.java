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

import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.metadata.swagger.model.Operation;
import org.apache.dubbo.metadata.swagger.model.PathItem.HttpMethod;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * The type Router operation.
 *
 * @author bnasslahsen
 */
public class RouterOperation implements Comparable<RouterOperation> {

    /**
     * The Path.
     */
    private String path;

    /**
     * The Methods.
     */
    private HttpMethod[] methods;

    /**
     * The Consumes.
     */
    private String[] consumes;

    /**
     * The Produces.
     */
    private String[] produces;

    /**
     * The Headers.
     */
    private String[] headers;

    /**
     * The Params.
     */
    private String[] params;

    /**
     * The Bean class.
     */
    private Class<?> beanClass;

    /**
     * The Bean method.
     */
    private String beanMethod;

    /**
     * The Parameter types.
     */
    private Class<?>[] parameterTypes;

    /**
     * The Query params.
     */
    private Map<String, String> queryParams;

    /**
     * The Operation model.
     */
    private Operation operationModel;

    /**
     * Instantiates a new Router operation.
     */
    public RouterOperation() {}

    /**
     * Instantiates a new Router operation.
     *
     * @param path     the path
     * @param methods  the methods
     * @param consumes the consumes
     * @param produces the produces
     * @param headers  the headers
     * @param params   the params
     */
    public RouterOperation(
            String path,
            HttpMethod[] methods,
            String[] consumes,
            String[] produces,
            String[] headers,
            String[] params) {
        this.path = path;
        this.methods = methods;
        this.consumes = consumes;
        this.produces = produces;
        this.headers = headers;
        this.params = params;
    }

    /**
     * Gets path.
     *
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets path.
     *
     * @param path the path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Get methods request method [ ].
     *
     * @return the request method [ ]
     */
    public HttpMethod[] getMethods() {
        return methods;
    }

    /**
     * Sets methods.
     *
     * @param methods the methods
     */
    public void setMethods(HttpMethod[] methods) {
        this.methods = methods;
    }

    /**
     * Get consumes string [ ].
     *
     * @return the string [ ]
     */
    public String[] getConsumes() {
        return consumes;
    }

    /**
     * Sets consumes.
     *
     * @param consumes the consumes
     */
    public void setConsumes(String[] consumes) {
        this.consumes = consumes;
    }

    /**
     * Get produces string [ ].
     *
     * @return the string [ ]
     */
    public String[] getProduces() {
        return produces;
    }

    /**
     * Sets produces.
     *
     * @param produces the produces
     */
    public void setProduces(String[] produces) {
        this.produces = produces;
    }

    /**
     * Gets bean class.
     *
     * @return the bean class
     */
    public Class<?> getBeanClass() {
        return beanClass;
    }

    /**
     * Sets bean class.
     *
     * @param beanClass the bean class
     */
    public void setBeanClass(Class<?> beanClass) {
        this.beanClass = beanClass;
    }

    /**
     * Gets bean method.
     *
     * @return the bean method
     */
    public String getBeanMethod() {
        return beanMethod;
    }

    /**
     * Sets bean method.
     *
     * @param beanMethod the bean method
     */
    public void setBeanMethod(String beanMethod) {
        this.beanMethod = beanMethod;
    }

    /**
     * Get parameter types class [ ].
     *
     * @return the class [ ]
     */
    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Sets parameter types.
     *
     * @param parameterTypes the parameter types
     */
    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    /**
     * Get headers string [ ].
     *
     * @return the string [ ]
     */
    public String[] getHeaders() {
        return headers;
    }

    /**
     * Sets headers.
     *
     * @param headers the headers
     */
    public void setHeaders(String[] headers) {
        this.headers = headers;
    }

    /**
     * Gets query params.
     *
     * @return the query params
     */
    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    /**
     * Sets query params.
     *
     * @param queryParams the query params
     */
    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    /**
     * Gets params.
     *
     * @return the params
     */
    public String[] getParams() {
        return this.params;
    }

    /**
     * Sets params.
     *
     * @param params the params
     */
    public void setParams(String[] params) {
        this.params = params;
    }

    @Override
    public int compareTo(RouterOperation routerOperation) {
        int result = path.compareTo(routerOperation.getPath());
        if (result == 0 && !ArrayUtils.isEmpty(methods))
            result = methods[0].compareTo(routerOperation.getMethods()[0]);
        if (result == 0 && operationModel != null && routerOperation.getOperationModel() != null)
            result = operationModel
                    .getOperationId()
                    .compareTo(routerOperation.getOperationModel().getOperationId());
        return result;
    }

    private Operation getOperationModel() {
        return operationModel;
    }

    private void setOperationModel(Operation operation) {
        this.operationModel = operation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouterOperation that = (RouterOperation) o;
        return Objects.equals(path, that.path)
                && Arrays.equals(methods, that.methods)
                && Arrays.equals(consumes, that.consumes)
                && Arrays.equals(produces, that.produces)
                && Arrays.equals(headers, that.headers)
                && Arrays.equals(params, that.params)
                && Objects.equals(beanClass, that.beanClass)
                && Objects.equals(beanMethod, that.beanMethod)
                && Arrays.equals(parameterTypes, that.parameterTypes)
                && Objects.equals(queryParams, that.queryParams)
                && Objects.equals(operationModel, that.operationModel);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(path, beanClass, beanMethod, queryParams, operationModel);
        result = 31 * result + Arrays.hashCode(methods);
        result = 31 * result + Arrays.hashCode(params);
        result = 31 * result + Arrays.hashCode(consumes);
        result = 31 * result + Arrays.hashCode(produces);
        result = 31 * result + Arrays.hashCode(headers);
        result = 31 * result + Arrays.hashCode(parameterTypes);
        return result;
    }
}
