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
package org.apache.dubbo.rpc.protocol.rest.extension.resteay.intercept;

import org.apache.commons.io.IOUtils;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.rest.RestHeaderEnum;
import org.apache.dubbo.rpc.protocol.rest.deploy.ServiceDeployer;
import org.apache.dubbo.rpc.protocol.rest.extension.ServiceDeployerContext;
import org.apache.dubbo.rpc.protocol.rest.filter.RestResponseInterceptor;
import org.apache.dubbo.rpc.protocol.rest.extension.resteay.ResteasyContext;
import org.apache.dubbo.rpc.protocol.rest.netty.NettyHttpResponse;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;
import org.jboss.resteasy.core.interception.AbstractWriterInterceptorContext;
import org.jboss.resteasy.plugins.server.netty.NettyHttpRequest;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.WriterInterceptor;
import java.io.ByteArrayOutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.RESTEASY_NETTY_HTTP_REQUEST_ATTRIBUTE_KEY;

@Activate(value = "resteasy", onClass = {"javax.ws.rs.ext.WriterInterceptorContext", "org.jboss.resteasy.plugins.server.netty.NettyHttpRequest"})
public class ResteasyWriterInterceptorAdapter implements RestResponseInterceptor, ServiceDeployerContext, ResteasyContext {

    private ResteasyProviderFactory resteasyProviderFactory = new ResteasyProviderFactory();


    @Override
    public void intercept(URL url, RequestFacade request, NettyHttpResponse response, Object result, RpcInvocation rpcInvocation, Iterator<RestResponseInterceptor> interceptorIterator, ServiceDeployer serviceDeployer) throws Exception {

        Class<?> type = rpcInvocation.getReturnType();


        List<WriterInterceptor> extension = serviceDeployer.getExtensions(WriterInterceptor.class);

        if (extension.isEmpty()) {
            iteratorIntercept(url, request, response, result, rpcInvocation, interceptorIterator, serviceDeployer);
            return;
        }

        NettyHttpRequest nettyHttpRequest = (NettyHttpRequest) RpcContext.getServiceContext().getObjectAttachment(RESTEASY_NETTY_HTTP_REQUEST_ATTRIBUTE_KEY);

        HttpRequest restRequest = nettyHttpRequest == null ? createNettyHttpRequest(request) : nettyHttpRequest;

        MultivaluedMap<String, Object> headers = new MultivaluedMapImpl();
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try {


            //   TODO add headers to response
            //

            MediaType mediaType = MediaType.valueOf(getAcceptMediaType(request, type).value);

            AbstractWriterInterceptorContext writerContext = getAbstractWriterInterceptorContext(restRequest, extension, result, type, type, mediaType, os, headers);

            writerContext.proceed();
            ByteArrayOutputStream outputStream = (ByteArrayOutputStream) writerContext.getOutputStream();

            addResponseHeaders(response, writerContext.getHeaders());

            // TODO add headers
            if (outputStream.size() <= 0) {
                iteratorIntercept(url, request, response, result, rpcInvocation, interceptorIterator, serviceDeployer);
                return;
            }


            // TODO write to response
            byte[] bytes = outputStream.toByteArray();
            response.getOutputStream().write(bytes);
            response.addOutputHeaders(RestHeaderEnum.CONTENT_TYPE.getHeader(), mediaType.getType());

        } finally {
            IOUtils.close(os);
        }

    }


    private AbstractWriterInterceptorContext getAbstractWriterInterceptorContext(HttpRequest request,
                                                                                 List<WriterInterceptor> extension,
                                                                                 Object entity,
                                                                                 Class type,
                                                                                 Type genericType,
                                                                                 MediaType mediaType,
                                                                                 ByteArrayOutputStream os,
                                                                                 MultivaluedMap<String, Object> headers) {
        AbstractWriterInterceptorContext writerContext = new DubboServerWriterInterceptorContext(extension.toArray(new WriterInterceptor[0]),
            resteasyProviderFactory, entity, type, genericType, new Annotation[0], mediaType,
            headers, os, request);
        return writerContext;
    }


}
