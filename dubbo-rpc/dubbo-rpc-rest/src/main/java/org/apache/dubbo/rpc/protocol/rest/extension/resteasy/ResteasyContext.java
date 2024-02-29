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
package org.apache.dubbo.rpc.protocol.rest.extension.resteasy;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.rpc.protocol.rest.deploy.ServiceDeployer;
import org.apache.dubbo.rpc.protocol.rest.extension.resteasy.filter.DubboContainerResponseContextImpl;
import org.apache.dubbo.rpc.protocol.rest.extension.resteasy.filter.DubboPreMatchContainerRequestContext;
import org.apache.dubbo.rpc.protocol.rest.filter.ServiceInvokeRestFilter;
import org.apache.dubbo.rpc.protocol.rest.netty.ChunkOutputStream;
import org.apache.dubbo.rpc.protocol.rest.netty.NettyHttpResponse;
import org.apache.dubbo.rpc.protocol.rest.request.NettyRequestFacade;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;
import org.apache.dubbo.rpc.protocol.rest.util.MediaTypeUtil;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import org.jboss.resteasy.core.interception.ResponseContainerRequestContext;
import org.jboss.resteasy.plugins.server.netty.NettyHttpRequest;
import org.jboss.resteasy.plugins.server.netty.NettyUtil;
import org.jboss.resteasy.specimpl.BuiltResponse;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyUriInfo;

public interface ResteasyContext {
    String HTTP_PROTOCOL = "http://";
    String HTTP = "http";
    String HTTPS_PROTOCOL = "https://";

    /**
     * return extensions that are  filtered by  extension type
     *
     * @param extension
     * @param <T>
     * @return
     */
    default <T> List<T> getExtension(ServiceDeployer serviceDeployer, Class<T> extension) {

        return serviceDeployer.getExtensions(extension);
    }

    default DubboPreMatchContainerRequestContext convertHttpRequestToContainerRequestContext(
            RequestFacade requestFacade, ContainerRequestFilter[] requestFilters) {

        NettyRequestFacade nettyRequestFacade = (NettyRequestFacade) requestFacade;
        HttpRequest request = (HttpRequest) requestFacade.getRequest();

        NettyHttpRequest nettyRequest = createNettyHttpRequest(nettyRequestFacade, request);

        if (request instanceof HttpContent) {

            try {
                byte[] inputStream = requestFacade.getInputStream();
                ByteBuf buffer =
                        nettyRequestFacade.getNettyChannelContext().alloc().buffer();
                buffer.writeBytes(inputStream);
                nettyRequest.setContentBuffer(buffer);
            } catch (IOException e) {
            }
        }

        return new DubboPreMatchContainerRequestContext(nettyRequest, requestFilters, null);
    }

    default ResteasyUriInfo extractUriInfo(HttpRequest request) {
        String host = HttpHeaders.getHost(request, "unknown");
        if ("".equals(host)) {
            host = "unknown";
        }
        String uri = request.getUri();

        String uriString;

        // If we appear to have an absolute URL, don't try to recreate it from the host and request line.
        if (uri.startsWith(HTTP_PROTOCOL) || uri.startsWith(HTTPS_PROTOCOL)) {
            uriString = uri;
        } else {
            uriString = HTTP + "://" + host + uri;
        }

        URI absoluteURI = URI.create(uriString);
        return new ResteasyUriInfo(uriString, absoluteURI.getRawQuery(), "");
    }

    default NettyHttpRequest createNettyHttpRequest(NettyRequestFacade nettyRequestFacade, HttpRequest request) {
        ResteasyHttpHeaders headers = NettyUtil.extractHttpHeaders(request);
        ResteasyUriInfo uriInfo = extractUriInfo(request);
        NettyHttpRequest nettyRequest = new NettyHttpRequest(
                nettyRequestFacade.getNettyChannelContext(),
                headers,
                uriInfo,
                request.getMethod().name(),
                null,
                null,
                HttpHeaders.is100ContinueExpected(request));

        return nettyRequest;
    }

    default NettyHttpRequest createNettyHttpRequest(RequestFacade requestFacade) {
        NettyRequestFacade nettyRequestFacade = (NettyRequestFacade) requestFacade;
        HttpRequest request = (HttpRequest) requestFacade.getRequest();

        ResteasyHttpHeaders headers = NettyUtil.extractHttpHeaders(request);
        ResteasyUriInfo uriInfo = extractUriInfo(request);
        NettyHttpRequest nettyRequest = new NettyHttpRequest(
                nettyRequestFacade.getNettyChannelContext(),
                headers,
                uriInfo,
                request.getMethod().name(),
                null,
                null,
                HttpHeaders.is100ContinueExpected(request));

        return nettyRequest;
    }

    default void writeResteasyResponse(
            URL url, RequestFacade requestFacade, NettyHttpResponse response, BuiltResponse restResponse)
            throws Exception {
        if (restResponse.getMediaType() != null) {
            MediaType mediaType = MediaTypeUtil.convertMediaType(
                    restResponse.getEntityClass(), restResponse.getMediaType().toString());
            ServiceInvokeRestFilter.writeResult(
                    response, url, restResponse.getEntity(), restResponse.getEntityClass(), mediaType);
        } else {
            ServiceInvokeRestFilter.writeResult(
                    response, requestFacade, url, restResponse.getEntity(), restResponse.getEntityClass());
        }
    }

    default MediaType getAcceptMediaType(RequestFacade request, Class<?> returnType) {

        return ServiceInvokeRestFilter.getAcceptMediaType(request, returnType);
    }

    default void addResponseHeaders(NettyHttpResponse response, MultivaluedMap<String, Object> headers) {
        if (headers == null || headers.isEmpty()) {

            return;
        }
        for (Map.Entry<String, List<Object>> entry : headers.entrySet()) {

            String key = entry.getKey();
            List<Object> value = entry.getValue();
            if (value == null || value.isEmpty()) {
                continue;
            }
            for (Object tmp : value) {
                response.addOutputHeaders(key, tmp.toString());
            }
        }
    }

    default DubboContainerResponseContextImpl createContainerResponseContext(
            Object originRequest,
            RequestFacade request,
            HttpResponse httpResponse,
            BuiltResponse jaxrsResponse,
            ContainerResponseFilter[] responseFilters) {

        NettyHttpRequest nettyHttpRequest =
                originRequest == null ? createNettyHttpRequest(request) : (NettyHttpRequest) originRequest;

        ResponseContainerRequestContext requestContext = new ResponseContainerRequestContext(nettyHttpRequest);
        DubboContainerResponseContextImpl responseContext = new DubboContainerResponseContextImpl(
                nettyHttpRequest, httpResponse, jaxrsResponse, requestContext, responseFilters, null, null);

        return responseContext;
    }

    default void restOutputStream(NettyHttpResponse response) throws IOException {
        ChunkOutputStream outputStream = (ChunkOutputStream) response.getOutputStream();
        outputStream.reset();
    }
}
