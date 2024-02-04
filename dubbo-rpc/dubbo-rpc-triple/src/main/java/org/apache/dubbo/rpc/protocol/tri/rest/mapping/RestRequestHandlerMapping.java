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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.remoting.http12.message.codec.CodecUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.RestHttpMessageCodec;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.ArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.CompositeArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.GeneralTypeConverter;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.TypeConverter;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.HandlerMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RequestUtils;
import org.apache.dubbo.rpc.protocol.tri.route.RequestHandler;
import org.apache.dubbo.rpc.protocol.tri.route.RequestHandlerMapping;

@Activate(order = -2000)
public final class RestRequestHandlerMapping implements RequestHandlerMapping {

    private final FrameworkModel frameworkModel;
    private final RequestMappingRegistry requestMappingRegistry;
    private final ArgumentResolver argumentResolver;
    private final TypeConverter typeConverter;
    private final ContentNegotiator contentNegotiator;
    private final CodecUtils codecUtils;

    public RestRequestHandlerMapping(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
        ScopeBeanFactory beanFactory = frameworkModel.getBeanFactory();
        requestMappingRegistry = beanFactory.getOrRegisterBean(DefaultRequestMappingRegistry.class);
        argumentResolver = beanFactory.getOrRegisterBean(CompositeArgumentResolver.class);
        typeConverter = beanFactory.getOrRegisterBean(GeneralTypeConverter.class);
        contentNegotiator = beanFactory.getOrRegisterBean(ContentNegotiator.class);
        codecUtils = beanFactory.getOrRegisterBean(CodecUtils.class);
    }

    @Override
    public RequestHandler getRequestHandler(URL url, HttpRequest request, HttpResponse response) {
        HandlerMeta meta = requestMappingRegistry.lookup(request);
        if (meta == null) {
            return null;
        }

        String requestMediaType = request.mediaType();
        String responseMediaType = contentNegotiator.negotiate(request);
        if (responseMediaType != null) {
            response.setContentType(responseMediaType);
        } else {
            if (requestMediaType != null && !RequestUtils.isFormOrMultiPart(request)) {
                responseMediaType = requestMediaType;
            } else {
                responseMediaType = MediaType.APPLICATION_JSON.getName();
            }
        }
        RestHttpMessageCodec codec = new RestHttpMessageCodec(
                request,
                response,
                meta.getParameters(),
                argumentResolver,
                typeConverter,
                codecUtils.determineHttpMessageEncoder(url, frameworkModel, responseMediaType));

        if (HttpMethods.supportBody(request.method()) && !RequestUtils.isFormOrMultiPart(request)) {
            if (StringUtils.isEmpty(requestMediaType)) {
                requestMediaType = responseMediaType;
            }
            request.setAttribute(
                    RestConstants.BODY_DECODER_ATTRIBUTE,
                    codecUtils.determineHttpMessageDecoder(url, frameworkModel, requestMediaType));
        }

        RequestHandler handler = new RequestHandler(meta.getInvoker());
        handler.setHasStub(false);
        handler.setMethodDescriptor(meta.getMethodDescriptor());
        handler.setMethodMetadata(meta.getMethodMetadata());
        handler.setServiceDescriptor(meta.getServiceDescriptor());
        handler.setHttpMessageDecoder(codec);
        handler.setHttpMessageEncoder(codec);
        return handler;
    }

    @Override
    public String getType() {
        return TripleConstant.TRIPLE_HANDLER_TYPE_REST;
    }
}
