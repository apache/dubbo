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
package org.apache.dubbo.rpc.protocol.rest.extension.resteasy.intercept;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.rest.RestHeaderEnum;
import org.apache.dubbo.rpc.protocol.rest.deploy.ServiceDeployer;
import org.apache.dubbo.rpc.protocol.rest.extension.resteasy.ResteasyContext;
import org.apache.dubbo.rpc.protocol.rest.filter.RestResponseInterceptor;
import org.apache.dubbo.rpc.protocol.rest.filter.context.RestInterceptContext;
import org.apache.dubbo.rpc.protocol.rest.netty.NettyHttpResponse;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.WriterInterceptor;

import java.io.ByteArrayOutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.core.interception.AbstractWriterInterceptorContext;
import org.jboss.resteasy.plugins.server.netty.NettyHttpRequest;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

@Activate(
        value = "resteasy",
        onClass = {
            "javax.ws.rs.ext.WriterInterceptorContext",
            "org.jboss.resteasy.plugins.server.netty.NettyHttpRequest",
            "org.jboss.resteasy.plugins.server.netty.NettyHttpResponse"
        })
public class ResteasyWriterInterceptorAdapter implements RestResponseInterceptor, ResteasyContext {

    private ResteasyProviderFactory resteasyProviderFactory = getResteasyProviderFactory();

    @Override
    public void intercept(RestInterceptContext restResponseInterceptor) throws Exception {

        RpcInvocation rpcInvocation = restResponseInterceptor.getRpcInvocation();
        ServiceDeployer serviceDeployer = restResponseInterceptor.getServiceDeployer();
        RequestFacade request = restResponseInterceptor.getRequestFacade();
        NettyHttpResponse response = restResponseInterceptor.getResponse();
        Object result = restResponseInterceptor.getResult();

        Class<?> type = rpcInvocation.getReturnType();

        List<WriterInterceptor> extension = serviceDeployer.getExtensions(WriterInterceptor.class);

        if (extension.isEmpty()) {
            return;
        }

        NettyHttpRequest nettyHttpRequest = (NettyHttpRequest) restResponseInterceptor.getOriginRequest();

        HttpRequest restRequest = nettyHttpRequest == null ? createNettyHttpRequest(request) : nettyHttpRequest;

        MultivaluedMap<String, Object> headers = new MultivaluedMapImpl();
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try {

            // get content-type
            String value = getAcceptMediaType(request, type).value;

            MediaType mediaType = MediaType.valueOf(value);

            AbstractWriterInterceptorContext writerContext = getAbstractWriterInterceptorContext(
                    restRequest, extension, result, type, type, mediaType, os, headers);

            writerContext.proceed();
            ByteArrayOutputStream outputStream = (ByteArrayOutputStream) writerContext.getOutputStream();

            addResponseHeaders(response, writerContext.getHeaders());

            if (outputStream.size() <= 0) {
                return;
            }

            // intercept response  first
            restOutputStream(response);

            byte[] bytes = outputStream.toByteArray();
            response.getOutputStream().write(bytes);
            response.addOutputHeaders(RestHeaderEnum.CONTENT_TYPE.getHeader(), value);

            restResponseInterceptor.setComplete(true);
        } finally {
            IOUtils.close(os);
        }
    }

    private AbstractWriterInterceptorContext getAbstractWriterInterceptorContext(
            HttpRequest request,
            List<WriterInterceptor> extension,
            Object entity,
            Class type,
            Type genericType,
            MediaType mediaType,
            ByteArrayOutputStream os,
            MultivaluedMap<String, Object> headers) {
        AbstractWriterInterceptorContext writerContext = new DubboServerWriterInterceptorContext(
                extension.toArray(new WriterInterceptor[0]),
                resteasyProviderFactory,
                entity,
                type,
                genericType,
                new Annotation[0],
                mediaType,
                headers,
                os,
                request);
        return writerContext;
    }

    protected ResteasyProviderFactory getResteasyProviderFactory() {
        return new ResteasyProviderFactory();
    }
}
