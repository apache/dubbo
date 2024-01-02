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
package org.apache.dubbo.rpc.protocol.tri.h12;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.message.codec.CodecUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.PathResolver;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.DescriptorUtils;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.TripleProtocol;
import org.apache.dubbo.rpc.protocol.tri.route.RequestHandler;
import org.apache.dubbo.rpc.protocol.tri.route.RequestHandlerMapping;

@Activate(order = -2000)
public class HttpRequestHandlerMapping implements RequestHandlerMapping {

    private final FrameworkModel frameworkModel;
    private final PathResolver pathResolver;
    private final CodecUtils codecUtils;

    public HttpRequestHandlerMapping(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
        pathResolver = frameworkModel.getDefaultExtension(PathResolver.class);
        codecUtils = frameworkModel.getBeanFactory().getOrRegisterBean(CodecUtils.class);
    }

    @Override
    public RequestHandler getRequestHandler(URL url, HttpRequest request, HttpResponse response) {
        if (!supportContentType(request.contentType())) {
            return null;
        }

        String uri = request.uri();
        int index = uri.indexOf('/', 1);
        if (index == -1) {
            return null;
        }
        if (uri.indexOf('/', index + 1) != -1) {
            return null;
        }

        String serviceName = uri.substring(1, index);
        String version = request.header(TripleHeaderEnum.SERVICE_VERSION.getHeader());
        String group = request.header(TripleHeaderEnum.SERVICE_GROUP.getHeader());
        String key = URL.buildKey(serviceName, group, version);
        Invoker<?> invoker = pathResolver.resolve(key);
        if (invoker == null) {
            if (TripleProtocol.RESOLVE_FALLBACK_TO_DEFAULT) {
                invoker = pathResolver.resolve(URL.buildKey(serviceName, group, TripleConstant.DEFAULT_VERSION));
                if (invoker == null) {
                    invoker = pathResolver.resolve(serviceName);
                    if (invoker == null) {
                        return null;
                    }
                }
            }
        }

        RequestHandler handler = new RequestHandler(invoker);
        handler.setHasStub(pathResolver.hasNativeStub(uri));
        handler.setMethodName(uri.substring(index + 1));
        handler.setServiceDescriptor(DescriptorUtils.findServiceDescriptor(invoker, serviceName, handler.isHasStub()));
        determineHttpMessageCodec(handler, url, request);
        return handler;
    }

    protected boolean supportContentType(String contentType) {
        return true;
    }

    protected void determineHttpMessageCodec(RequestHandler handler, URL url, HttpRequest request) {
        String mediaType = request.mediaType();
        handler.setHttpMessageDecoder(codecUtils.determineHttpMessageDecoder(url, frameworkModel, mediaType));
        handler.setHttpMessageEncoder(codecUtils.determineHttpMessageEncoder(url, frameworkModel, mediaType));
    }

    protected final FrameworkModel getFrameworkModel() {
        return frameworkModel;
    }

    @Override
    public String getType() {
        return TripleConstant.TRIPLE_HANDLER_TYPE_HTTP;
    }
}
