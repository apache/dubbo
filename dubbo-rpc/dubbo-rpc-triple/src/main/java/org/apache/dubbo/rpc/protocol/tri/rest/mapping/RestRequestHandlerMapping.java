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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.HttpResult;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.exception.HttpResultPayloadException;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.remoting.http12.message.codec.CodecUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.TripleConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.RestHttpMessageCodec;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.ArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.CompositeArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.GeneralTypeConverter;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.TypeConverter;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.MethodsCondition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.HandlerMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RequestUtils;
import org.apache.dubbo.rpc.protocol.tri.route.RequestHandler;
import org.apache.dubbo.rpc.protocol.tri.route.RequestHandlerMapping;

import java.util.Set;

@Activate(order = -2000)
public final class RestRequestHandlerMapping implements RequestHandlerMapping {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestRequestHandlerMapping.class);

    private final RequestMappingRegistry requestMappingRegistry;
    private final ArgumentResolver argumentResolver;
    private final TypeConverter typeConverter;
    private final ContentNegotiator contentNegotiator;
    private final CodecUtils codecUtils;

    public RestRequestHandlerMapping(FrameworkModel frameworkModel) {
        ScopeBeanFactory beanFactory = frameworkModel.getBeanFactory();
        requestMappingRegistry = beanFactory.getOrRegisterBean(DefaultRequestMappingRegistry.class);
        argumentResolver = beanFactory.getOrRegisterBean(CompositeArgumentResolver.class);
        typeConverter = beanFactory.getOrRegisterBean(GeneralTypeConverter.class);
        contentNegotiator = beanFactory.getOrRegisterBean(ContentNegotiator.class);
        codecUtils = beanFactory.getOrRegisterBean(CodecUtils.class);
    }

    @Override
    public RequestHandler getRequestHandler(URL url, HttpRequest request, HttpResponse response) {
        LOGGER.debug("Received http request: {}", request);

        HandlerMeta meta = requestMappingRegistry.lookup(request);
        if (meta == null) {
            String path = request.attribute(RestConstants.PATH_ATTRIBUTE);
            if (RestConstants.SLASH.equals(path) && HttpMethods.OPTIONS.name().equals(request.method())) {
                handleOptionsRequest(request);
            }

            LOGGER.debug("No handler found for http request: {}", request);
            return null;
        }

        String method = request.method();
        if (HttpMethods.OPTIONS.name().equals(method)) {
            handleOptionsRequest(request);
        }

        String requestMediaType = request.mediaType();
        String responseMediaType = contentNegotiator.negotiate(request, meta);
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
                codecUtils.determineHttpMessageEncoder(url, responseMediaType));

        if (HttpMethods.supportBody(method) && !RequestUtils.isFormOrMultiPart(request)) {
            if (StringUtils.isEmpty(requestMediaType)) {
                requestMediaType = responseMediaType;
            }
            request.setAttribute(
                    RestConstants.BODY_DECODER_ATTRIBUTE,
                    codecUtils.determineHttpMessageDecoder(url, requestMediaType));
        }

        LOGGER.debug("Content-type negotiate result: request='{}', response='{}'", requestMediaType, responseMediaType);

        RequestHandler handler = new RequestHandler(meta.getInvoker());
        handler.setHasStub(false);
        handler.setMethodDescriptor(meta.getMethodDescriptor());
        handler.setMethodMetadata(meta.getMethodMetadata());
        handler.setServiceDescriptor(meta.getServiceDescriptor());
        handler.setHttpMessageDecoder(codec);
        handler.setHttpMessageEncoder(codec);
        return handler;
    }

    private static void handleOptionsRequest(HttpRequest request) {
        RequestMapping mapping = request.attribute(RestConstants.MAPPING_ATTRIBUTE);
        MethodsCondition condition = mapping == null ? null : mapping.getMethodsCondition();
        if (condition == null) {
            throw new HttpResultPayloadException(HttpResult.builder()
                    .status(HttpStatus.NO_CONTENT)
                    .header("allow", "GET,HEAD,POST,PUT,PATCH,DELETE,OPTIONS")
                    .build());
        }
        Set<String> methods = condition.getMethods();
        if (methods.size() == 1 && methods.contains(HttpMethods.OPTIONS.name())) {
            return;
        }
        throw new HttpResultPayloadException(HttpResult.builder()
                .status(HttpStatus.NO_CONTENT)
                .header("allow", StringUtils.join(methods, ","))
                .build());
    }

    @Override
    public String getType() {
        return TripleConstants.TRIPLE_HANDLER_TYPE_REST;
    }
}
