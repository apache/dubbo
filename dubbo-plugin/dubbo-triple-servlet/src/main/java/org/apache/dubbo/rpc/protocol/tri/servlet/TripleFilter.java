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
package org.apache.dubbo.rpc.protocol.tri.servlet;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessageFrame;
import org.apache.dubbo.remoting.http12.h2.Http2ServerTransportListenerFactory;
import org.apache.dubbo.remoting.http12.h2.Http2TransportListener;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.PathResolver;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.ServletExchanger;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.TripleProtocol;
import org.apache.dubbo.rpc.protocol.tri.h12.grpc.GrpcHeaderNames;
import org.apache.dubbo.rpc.protocol.tri.h12.grpc.GrpcHttp2ServerTransportListener;
import org.apache.dubbo.rpc.protocol.tri.h12.grpc.GrpcUtils;
import org.apache.dubbo.rpc.protocol.tri.h12.http2.GenericHttp2ServerTransportListenerFactory;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.DefaultRequestMappingRegistry;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMappingRegistry;

import javax.servlet.AsyncContext;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_IO_EXCEPTION;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;

public class TripleFilter implements Filter {

    private static final ErrorTypeAwareLogger LOG = LoggerFactory.getErrorTypeAwareLogger(TripleFilter.class);

    private PathResolver pathResolver;
    private RequestMappingRegistry mappingRegistry;
    private int defaultTimeout;

    @Override
    public void init(FilterConfig config) {
        FrameworkModel frameworkModel = FrameworkModel.defaultModel();
        pathResolver = frameworkModel.getDefaultExtension(PathResolver.class);
        mappingRegistry = frameworkModel.getBeanFactory().getOrRegisterBean(DefaultRequestMappingRegistry.class);
        String timeoutString = config.getInitParameter("timeout");
        defaultTimeout = timeoutString == null ? 180_000 : Integer.parseInt(timeoutString);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpServletRequest hRequest = (HttpServletRequest) request;
        HttpServletResponse hResponse = (HttpServletResponse) response;

        if (!hasServiceMapping(hRequest) && !mappingRegistry.exists(hRequest.getRequestURI(), hRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        AsyncContext context = request.startAsync();
        try {
            H2StreamChannel streamChannel = new ServletStreamChannel(hRequest, hResponse, context);
            Http2TransportListener listener = determineHttp2ServerTransportListenerFactory(request.getContentType())
                    .newInstance(streamChannel, ServletExchanger.getUrl(), FrameworkModel.defaultModel());

            context.setTimeout(resolveTimeout(hRequest, listener instanceof GrpcHttp2ServerTransportListener));

            listener.onMetadata(new HttpMetadataAdapter(hRequest));

            ByteArrayOutputStream os;
            try {
                os = new ByteArrayOutputStream(1024);
                StreamUtils.copy(request.getInputStream(), os);
            } catch (Throwable t) {
                LOG.error(COMMON_IO_EXCEPTION, "", "", "Failed to read input", t);
                try {
                    hResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
                } finally {
                    context.complete();
                }
                return;
            }
            listener.onData(new Http2InputMessageFrame(new ByteArrayInputStream(os.toByteArray()), true));
        } catch (Throwable t) {
            LOG.error(INTERNAL_ERROR, "", "", "Failed to process request", t);
            try {
                hResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } finally {
                context.complete();
            }
        }
    }

    @Override
    public void destroy() {}

    private boolean hasServiceMapping(HttpServletRequest request) {
        String uri = request.getRequestURI();

        int index = uri.indexOf('/', 1);
        if (index == -1) {
            return false;
        }
        if (uri.indexOf('/', index + 1) != -1) {
            return false;
        }

        String serviceName = uri.substring(1, index);
        String version = request.getHeader(TripleHeaderEnum.SERVICE_VERSION.getHeader());
        String group = request.getHeader(TripleHeaderEnum.SERVICE_GROUP.getHeader());
        String key = URL.buildKey(serviceName, group, version);
        Invoker<?> invoker = pathResolver.resolve(key);
        if (invoker == null && TripleProtocol.RESOLVE_FALLBACK_TO_DEFAULT) {
            invoker = pathResolver.resolve(URL.buildKey(serviceName, group, TripleConstant.DEFAULT_VERSION));
            if (invoker == null) {
                return pathResolver.resolve(serviceName) != null;
            }
        }

        return true;
    }

    private Http2ServerTransportListenerFactory determineHttp2ServerTransportListenerFactory(String contentType) {
        Set<Http2ServerTransportListenerFactory> http2ServerTransportListenerFactories = FrameworkModel.defaultModel()
                .getExtensionLoader(Http2ServerTransportListenerFactory.class)
                .getSupportedExtensionInstances();
        for (Http2ServerTransportListenerFactory factory : http2ServerTransportListenerFactories) {
            if (factory.supportContentType(contentType)) {
                return factory;
            }
        }
        return GenericHttp2ServerTransportListenerFactory.INSTANCE;
    }

    private int resolveTimeout(HttpServletRequest request, boolean isGrpc) {
        try {
            if (isGrpc) {
                String timeoutString = request.getHeader(GrpcHeaderNames.GRPC_TIMEOUT.getName());
                if (timeoutString != null) {
                    Long timeout = GrpcUtils.parseTimeoutToMills(timeoutString);
                    if (timeout != null) {
                        return timeout.intValue() + 2000;
                    }
                }
            } else {
                String timeoutString = request.getHeader(TripleHeaderEnum.SERVICE_TIMEOUT.getHeader());
                if (timeoutString != null) {
                    return Integer.parseInt(timeoutString) + 2000;
                }
            }
        } catch (Throwable ignored) {
        }
        return defaultTimeout;
    }
}
