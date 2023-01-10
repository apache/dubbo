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
package org.apache.dubbo.rpc.protocol.rest.httpinvoke;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.*;
import org.apache.dubbo.rpc.protocol.rest.request.convert.RequestConvert;
import org.apache.dubbo.rpc.protocol.rest.request.convert.factory.RequestConvertFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HttpInvokeInvocationHandler<CLIENT> implements InvocationHandler {
    private static final RequestConvertFactory requestConvertAdaptive = ApplicationModel.defaultModel().getExtensionLoader(RequestConvertFactory.class).getAdaptiveExtension();

    private static Set<HttpConnectionPreBuildIntercept> httpConnectionPreBuildIntercepts =
        ApplicationModel.defaultModel().getExtensionLoader(HttpConnectionPreBuildIntercept.class).getSupportedExtensionInstances();

    private final Map<Method, RestMethodMetadata> methodRestMethodMetadataMap;
    private final String address;
    private final CLIENT restClient;
    private URL url;

    public HttpInvokeInvocationHandler(Map<Method, RestMethodMetadata> metadataMap, URL url, CLIENT restClient) {
        this.methodRestMethodMetadataMap = metadataMap;
        this.url = url;
        this.address = url.getAddress();
        this.restClient = restClient;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RestMethodMetadata restMethodMetadata = methodRestMethodMetadataMap.get(method);

        RequestTemplate requestTemplate = new RequestTemplate(restMethodMetadata.getRequest().getMethod(), address);

        HttpConnectionConfig connectionConfig = new HttpConnectionConfig();

        HttpConnectionCreateContext httpConnectionCreateContext = createBuildContext(requestTemplate,
            connectionConfig,
            restMethodMetadata, Arrays.asList(args));

        for (HttpConnectionPreBuildIntercept intercept : httpConnectionPreBuildIntercepts) {

            intercept.intercept(httpConnectionCreateContext);
        }

        RequestConvert requestConvert = requestConvertAdaptive.createRequestConvert(url, restClient, restMethodMetadata);


        return requestConvert.request(requestTemplate);


    }

    private static HttpConnectionCreateContext createBuildContext(RequestTemplate requestTemplate,
                                                                  HttpConnectionConfig connectionConfig,
                                                                  RestMethodMetadata restMethodMetadata, List<Object> rags) {
        HttpConnectionCreateContext httpConnectionCreateContext = new HttpConnectionCreateContext();
        httpConnectionCreateContext.setConnectionConfig(connectionConfig);
        httpConnectionCreateContext.setRequestTemplate(requestTemplate);
        httpConnectionCreateContext.setRestMethodMetadata(restMethodMetadata);
        httpConnectionCreateContext.setMethodRealArgs(rags);
        return httpConnectionCreateContext;
    }
}
