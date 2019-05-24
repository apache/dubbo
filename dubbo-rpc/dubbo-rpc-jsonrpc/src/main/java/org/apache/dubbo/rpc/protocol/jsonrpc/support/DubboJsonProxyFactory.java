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
package org.apache.dubbo.rpc.protocol.jsonrpc.support;

import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.jsonrpc.RpcContextFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ReflectionUtil;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class DubboJsonProxyFactory implements MethodInterceptor {

    private Object proxyObject = null;

    private JsonRpcHttpClient jsonRpcHttpClient = null;
    private Map<String, String> extraHttpHeaders = new HashMap<>();

    private final Class<?> serviceInterface;
    private final String serviceUrl;
    private ObjectMapper objectMapper = null;

    public DubboJsonProxyFactory(Class<?> serviceInterface, String serviceUrl) {
        this.serviceInterface = serviceInterface;
        this.serviceUrl = serviceUrl;
    }

    public void init() {
        proxyObject = ProxyFactory.getProxy(serviceInterface, this);

        if (jsonRpcHttpClient == null) {
            if (objectMapper == null) {
                objectMapper = new ObjectMapper();
            }

            try {
                jsonRpcHttpClient = new JsonRpcHttpClient(objectMapper, new URL(serviceUrl), extraHttpHeaders);
            } catch (MalformedURLException mue) {
                throw new RpcException(mue);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(MethodInvocation invocation)
            throws Throwable {
        Method method = invocation.getMethod();
        if (method.getDeclaringClass() == Object.class && method.getName().equals("toString")) {
            return proxyObject.getClass().getName() + "@" + System.identityHashCode(proxyObject);
        }

        Type retType = (invocation.getMethod().getGenericReturnType() != null) ? invocation.getMethod().getGenericReturnType() : invocation.getMethod().getReturnType();
        Object arguments = ReflectionUtil.parseArguments(invocation.getMethod(), invocation.getArguments(), false);

        HashMap<String, String> headers = new HashMap<>(extraHttpHeaders);
        headers.put(RpcContextFilter.DUBBO_ATTACHMENT_HEADER,
                RpcContextFilter.parse(RpcContext.getContext().getAttachments()));

        return jsonRpcHttpClient.invoke(invocation.getMethod().getName(), arguments, retType, headers);
    }


    public Object getObject() {
        return proxyObject;
    }

    public Class<?> getObjectType() {
        return serviceInterface;
    }

    /**
     * @param objectMapper the objectMapper to set
     */
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * @param extraHttpHeaders the extraHttpHeaders to set
     */
    public void setExtraHttpHeaders(Map<String, String> extraHttpHeaders) {
        this.extraHttpHeaders = extraHttpHeaders;
    }

    public void setJsonRpcHttpClient(JsonRpcHttpClient jsonRpcHttpClient) {
        this.jsonRpcHttpClient = jsonRpcHttpClient;
    }

}
