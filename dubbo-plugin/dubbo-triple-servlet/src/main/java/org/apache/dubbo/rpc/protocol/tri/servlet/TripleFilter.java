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

import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.http12.HttpVersion;
import org.apache.dubbo.remoting.http12.h1.Http1InputMessage;
import org.apache.dubbo.remoting.http12.h1.Http1ServerTransportListener;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessageFrame;
import org.apache.dubbo.remoting.http12.h2.Http2ServerTransportListenerFactory;
import org.apache.dubbo.remoting.http12.h2.Http2TransportListener;
import org.apache.dubbo.rpc.PathResolver;
import org.apache.dubbo.rpc.TriRpcStatus.Code;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.RequestPath;
import org.apache.dubbo.rpc.protocol.tri.ServletExchanger;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.h12.grpc.GrpcHeaderNames;
import org.apache.dubbo.rpc.protocol.tri.h12.grpc.GrpcHttp2ServerTransportListener;
import org.apache.dubbo.rpc.protocol.tri.h12.grpc.GrpcUtils;
import org.apache.dubbo.rpc.protocol.tri.h12.http1.DefaultHttp11ServerTransportListenerFactory;
import org.apache.dubbo.rpc.protocol.tri.h12.http2.GenericHttp2ServerTransportListenerFactory;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.DefaultRequestMappingRegistry;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMappingRegistry;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import static org.apache.dubbo.rpc.protocol.tri.TripleConstants.UPGRADE_HEADER_KEY;

public class TripleFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TripleFilter.class);

    private PathResolver pathResolver;
    private RequestMappingRegistry mappingRegistry;

    @Override
    public void init(FilterConfig config) {
        FrameworkModel frameworkModel = FrameworkModel.defaultModel();
        pathResolver = frameworkModel.getDefaultExtension(PathResolver.class);
        mappingRegistry = frameworkModel.getOrRegisterBean(DefaultRequestMappingRegistry.class);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        boolean isHttp2 = HttpVersion.HTTP2.getProtocol().equals(request.getProtocol());
        if (isHttp2) {
            if (hasGrpcMapping(request) || mappingRegistry.exists(request.getRequestURI(), request.getMethod())) {
                handleHttp2(request, response);
                return;
            }
        } else {
            if (notUpgradeRequest(request) && mappingRegistry.exists(request.getRequestURI(), request.getMethod())) {
                handleHttp1(request, response);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private void handleHttp2(HttpServletRequest request, HttpServletResponse response) {
        AsyncContext context = request.startAsync(request, response);
        ServletStreamChannel channel = new ServletStreamChannel(request, response, context);
        try {
            Http2TransportListener listener = determineHttp2ServerTransportListenerFactory(request.getContentType())
                    .newInstance(channel, ServletExchanger.getUrl(), FrameworkModel.defaultModel());

            boolean isGrpc = listener instanceof GrpcHttp2ServerTransportListener;
            channel.setGrpc(isGrpc);
            context.setTimeout(resolveTimeout(request, isGrpc));
            context.addListener(new TripleAsyncListener(channel));
            ServletInputStream is = request.getInputStream();
            is.setReadListener(new TripleReadListener(listener, channel, is));
            response.getOutputStream().setWriteListener(new TripleWriteListener(channel));

            listener.onMetadata(new HttpMetadataAdapter(request));
        } catch (Throwable t) {
            LOGGER.info("Failed to process request", t);
            channel.writeError(Code.UNKNOWN.code, t);
        }
    }

    private void handleHttp1(HttpServletRequest request, HttpServletResponse response) {
        AsyncContext context = request.startAsync(request, response);
        ServletStreamChannel channel = new ServletStreamChannel(request, response, context);
        try {
            Http1ServerTransportListener listener = DefaultHttp11ServerTransportListenerFactory.INSTANCE.newInstance(
                    channel, ServletExchanger.getUrl(), FrameworkModel.defaultModel());
            channel.setGrpc(false);
            context.setTimeout(resolveTimeout(request, false));
            ServletInputStream is = request.getInputStream();
            response.getOutputStream().setWriteListener(new TripleWriteListener(channel));

            listener.onMetadata(new HttpMetadataAdapter(request));
            listener.onData(new Http1InputMessage(
                    is.available() == 0 ? StreamUtils.EMPTY : new ByteArrayInputStream(StreamUtils.readBytes(is))));
        } catch (Throwable t) {
            LOGGER.info("Failed to process request", t);
            channel.writeError(Code.UNKNOWN.code, t);
        }
    }

    @Override
    public void destroy() {}

    private boolean hasGrpcMapping(HttpServletRequest request) {
        if (!GrpcUtils.isGrpcRequest(request.getContentType())) {
            return false;
        }

        RequestPath path = RequestPath.parse(request.getRequestURI());
        if (path == null) {
            return false;
        }

        String group = request.getHeader(TripleHeaderEnum.SERVICE_GROUP.getName());
        String version = request.getHeader(TripleHeaderEnum.SERVICE_VERSION.getName());
        return pathResolver.resolve(path.getPath(), group, version) != null;
    }

    private boolean notUpgradeRequest(HttpServletRequest request) {
        return request.getHeader(UPGRADE_HEADER_KEY) == null;
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

    private static int resolveTimeout(HttpServletRequest request, boolean isGrpc) {
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
                String timeoutString = request.getHeader(TripleHeaderEnum.SERVICE_TIMEOUT.getName());
                if (timeoutString != null) {
                    return Integer.parseInt(timeoutString) + 2000;
                }
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static final class TripleAsyncListener implements AsyncListener {

        private final ServletStreamChannel streamChannel;

        TripleAsyncListener(ServletStreamChannel streamChannel) {
            this.streamChannel = streamChannel;
        }

        @Override
        public void onComplete(AsyncEvent event) {}

        @Override
        public void onTimeout(AsyncEvent event) {
            streamChannel.writeError(Code.DEADLINE_EXCEEDED.code, event.getThrowable());
        }

        @Override
        public void onError(AsyncEvent event) {
            streamChannel.writeError(Code.CANCELLED.code, event.getThrowable());
        }

        @Override
        public void onStartAsync(AsyncEvent event) {}
    }

    private static final class TripleReadListener implements ReadListener {

        private final Http2TransportListener listener;
        private final ServletStreamChannel channel;
        private final ServletInputStream input;
        private final byte[] buffer = new byte[4 * 1024];

        TripleReadListener(Http2TransportListener listener, ServletStreamChannel channel, ServletInputStream input) {
            this.listener = listener;
            this.channel = channel;
            this.input = input;
        }

        @Override
        public void onDataAvailable() throws IOException {
            while (input.isReady()) {
                int length = input.read(buffer);
                if (length == -1) {
                    return;
                }
                byte[] copy = Arrays.copyOf(buffer, length);
                listener.onData(new Http2InputMessageFrame(new ByteArrayInputStream(copy), false));
            }
        }

        @Override
        public void onAllDataRead() {
            listener.onData(new Http2InputMessageFrame(StreamUtils.EMPTY, true));
        }

        @Override
        public void onError(Throwable t) {
            channel.writeError(Code.CANCELLED.code, t);
        }
    }

    private static final class TripleWriteListener implements WriteListener {

        private final ServletStreamChannel channel;

        TripleWriteListener(ServletStreamChannel channel) {
            this.channel = channel;
        }

        @Override
        public void onWritePossible() {
            channel.onWritePossible();
        }

        @Override
        public void onError(Throwable t) {
            channel.writeError(Code.CANCELLED.code, t);
        }
    }
}
