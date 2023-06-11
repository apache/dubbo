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
package org.apache.dubbo.rpc.protocol.rest.extension.resteay;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.dubbo.rpc.protocol.rest.extension.resteay.filter.DubboPreMatchContainerRequestContext;
import org.apache.dubbo.rpc.protocol.rest.request.NettyRequestFacade;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;
import org.jboss.resteasy.plugins.server.netty.NettyHttpRequest;
import org.jboss.resteasy.plugins.server.netty.NettyUtil;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.spi.ResteasyUriInfo;

import javax.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;
import java.net.URI;

public interface ResteasyContext {
    String HTTP_PROTOCOL = "http://";
    String HTTP = "http";
    String HTTPS_PROTOCOL = "https://";


    default DubboPreMatchContainerRequestContext convertHttpRequestToContainerRequestContext(RequestFacade requestFacade, ContainerRequestFilter[] requestFilters) {


        NettyRequestFacade nettyRequestFacade = (NettyRequestFacade) requestFacade;
        HttpRequest request = (HttpRequest) requestFacade.getRequest();

        NettyHttpRequest nettyRequest = createNettyHttpRequest(nettyRequestFacade, request);

        if (request instanceof HttpContent) {

            try {
                byte[] inputStream = requestFacade.getInputStream();
                ByteBuf buffer = nettyRequestFacade.getNettyChannelContext().alloc().buffer();
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
        NettyHttpRequest nettyRequest = new NettyHttpRequest(nettyRequestFacade.getNettyChannelContext(), headers, uriInfo, request.getMethod().name(),
            null, null, HttpHeaders.is100ContinueExpected(request));

        return nettyRequest;
    }


    default NettyHttpRequest createNettyHttpRequest(RequestFacade requestFacade) {
        NettyRequestFacade nettyRequestFacade = (NettyRequestFacade) requestFacade;
        HttpRequest request = (HttpRequest) requestFacade.getRequest();

        ResteasyHttpHeaders headers = NettyUtil.extractHttpHeaders(request);
        ResteasyUriInfo uriInfo = extractUriInfo(request);
        NettyHttpRequest nettyRequest = new NettyHttpRequest(nettyRequestFacade.getNettyChannelContext(), headers, uriInfo, request.getMethod().name(),
            null, null, HttpHeaders.is100ContinueExpected(request));

        return nettyRequest;
    }
}
