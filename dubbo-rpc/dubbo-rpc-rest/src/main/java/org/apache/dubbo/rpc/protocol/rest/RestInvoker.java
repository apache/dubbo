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
package org.apache.dubbo.rpc.protocol.rest;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.ParameterTypesComparator;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;
import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.remoting.http.RequestTemplate;
import org.apache.dubbo.remoting.http.RestClient;
import org.apache.dubbo.remoting.http.RestResult;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.AbstractInvoker;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionCreateContext;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionPreBuildIntercept;
import org.apache.dubbo.rpc.protocol.rest.exception.ParamParseException;
import org.apache.dubbo.rpc.protocol.rest.exception.PathNoFoundException;
import org.apache.dubbo.rpc.protocol.rest.exception.RemoteServerInternalException;
import org.apache.dubbo.rpc.protocol.rest.message.HttpMessageCodecManager;
import org.apache.dubbo.rpc.protocol.rest.util.HttpHeaderUtil;
import org.apache.dubbo.rpc.protocol.rest.util.MediaTypeUtil;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class RestInvoker<T> extends AbstractInvoker<T> {
    private final ServiceRestMetadata serviceRestMetadata;
    private final ReferenceCountedClient<? extends RestClient> referenceCountedClient;
    private final Set<HttpConnectionPreBuildIntercept> httpConnectionPreBuildIntercepts;


    public RestInvoker(Class type, URL url,
                       ReferenceCountedClient<? extends RestClient> referenceCountedClient,
                       Set<HttpConnectionPreBuildIntercept> httpConnectionPreBuildIntercepts,
                       ServiceRestMetadata serviceRestMetadata) {
        super(type, url);
        this.serviceRestMetadata = serviceRestMetadata;
        this.referenceCountedClient = referenceCountedClient;
        this.httpConnectionPreBuildIntercepts = httpConnectionPreBuildIntercepts;
    }

    @Override
    protected Result doInvoke(Invocation invocation) {
        try {

            Map<String, Map<ParameterTypesComparator, RestMethodMetadata>> metadataMap = serviceRestMetadata.getMethodToServiceMap();
            //  get metadata
            RestMethodMetadata restMethodMetadata = metadataMap.get(invocation.getMethodName()).get(ParameterTypesComparator.getInstance(invocation.getParameterTypes()));

            // create requestTemplate
            RequestTemplate requestTemplate = new RequestTemplate(invocation, restMethodMetadata.getRequest().getMethod(), getUrl().getAddress());

            HttpConnectionCreateContext httpConnectionCreateContext =
                creatHttpConnectionCreateContext(invocation, serviceRestMetadata, restMethodMetadata, requestTemplate);

            // fill real  data
            for (HttpConnectionPreBuildIntercept intercept : httpConnectionPreBuildIntercepts) {
                intercept.intercept(httpConnectionCreateContext);
            }

            // TODO check rest client cannot be reused
            CompletableFuture<RestResult> future = referenceCountedClient.getClient().send(requestTemplate);
            CompletableFuture<AppResponse> responseFuture = new CompletableFuture<>();
            AsyncRpcResult asyncRpcResult = new AsyncRpcResult(responseFuture, invocation);
            future.whenComplete((r, t) -> {
                if (t != null) {
                    responseFuture.completeExceptionally(t);
                } else {
                    AppResponse appResponse = new AppResponse();
                    try {
                        int responseCode = r.getResponseCode();
                        MediaType mediaType = MediaType.TEXT_PLAIN;

                        if (responseCode == 404) {
                            responseFuture.completeExceptionally(new PathNoFoundException(r.getMessage()));
                        } else if (400 <= responseCode && responseCode < 500) {
                            responseFuture.completeExceptionally(new ParamParseException(r.getMessage()));
                            // TODO add Exception Mapper
                        } else if (responseCode >= 500) {
                            responseFuture.completeExceptionally(new RemoteServerInternalException(r.getMessage()));
                        } else if (responseCode < 400) {
                            mediaType = MediaTypeUtil.convertMediaType(restMethodMetadata.getReflectMethod().getReturnType(), r.getContentType());
                            Object value = HttpMessageCodecManager.httpMessageDecode(r.getBody(),
                                restMethodMetadata.getReflectMethod().getReturnType(), mediaType);
                            appResponse.setValue(value);
                            // resolve response attribute & attachment
                            HttpHeaderUtil.parseResponseHeader(appResponse, r);
                            responseFuture.complete(appResponse);
                        }
                    } catch (Exception e) {
                        responseFuture.completeExceptionally(e);
                    }
                }
            });
            return asyncRpcResult;
        } catch (RpcException e) {
            throw e;
        }
    }

    /**
     * create intercept context
     *
     * @param invocation
     * @param serviceRestMetadata
     * @param restMethodMetadata
     * @param requestTemplate
     * @return
     */
    private HttpConnectionCreateContext creatHttpConnectionCreateContext(Invocation invocation, ServiceRestMetadata serviceRestMetadata, RestMethodMetadata restMethodMetadata, RequestTemplate requestTemplate) {
        HttpConnectionCreateContext httpConnectionCreateContext = new HttpConnectionCreateContext();
        httpConnectionCreateContext.setRequestTemplate(requestTemplate);
        httpConnectionCreateContext.setRestMethodMetadata(restMethodMetadata);
        httpConnectionCreateContext.setServiceRestMetadata(serviceRestMetadata);
        httpConnectionCreateContext.setInvocation(invocation);
        httpConnectionCreateContext.setUrl(getUrl());
        return httpConnectionCreateContext;
    }
}
