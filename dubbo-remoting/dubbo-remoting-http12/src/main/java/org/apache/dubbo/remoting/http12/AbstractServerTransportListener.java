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
package org.apache.dubbo.remoting.http12;

import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.PathResolver;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author icodening
 * @date 2023.05.31
 */
public abstract class AbstractServerTransportListener<HEADER extends RequestMetadata, MESSAGE extends HttpMessage> implements ServerTransportListener<HEADER, MESSAGE> {

    private final HttpChannel httpChannel;

    private final PathResolver pathResolver;

    private final Map<String, HttpMessageCodec> codecs;

    private final FrameworkModel frameworkModel;

    private HttpMessageCodec codec;

    private ServerCall serverCall;

    public AbstractServerTransportListener(HttpChannel httpChannel, FrameworkModel frameworkModel) {
        this.httpChannel = httpChannel;
        this.frameworkModel = frameworkModel;
        this.pathResolver = frameworkModel.getExtensionLoader(PathResolver.class).getDefaultExtension();
        this.codecs = frameworkModel.getExtensionLoader(HttpMessageCodec.class).getActivateExtensions().stream().collect(Collectors.toMap(httpMessageCodec -> httpMessageCodec.contentType().getName(), Function.identity()));
    }

    protected HttpChannel getHttpChannel() {
        return httpChannel;
    }

    @Override
    public void onMetadata(HEADER metadata) {
        String method = metadata.method();
        String path = metadata.path();
        HttpHeaders headers = metadata.headers();
        String contentType = headers.getFirst(HttpHeaderNames.CONTENT_TYPE.getName());
        String[] parts = path.split("/");
        if (parts.length != 3) {
            return;
        }
        HttpMessageCodec httpMessageCodec = codecs.get(contentType);
        if (httpMessageCodec == null) {
            throw new UnsupportedOperationException("Unsupported content-type: " + contentType);
        }
        this.codec = httpMessageCodec;
        String serviceName = parts[1];
        String originalMethodName = parts[2];
        boolean hasStub = pathResolver.hasNativeStub(path);
        Invoker<?> invoker = pathResolver.resolve(serviceName);
        //create ServerCallListener
        if (hasStub) {
//            listener = new StubAbstractServerCall(invoker, TripleServerStream.this,
//                frameworkModel,
//                acceptEncoding, serviceName, originalMethodName, executor);
        } else {
            serverCall = new ReflectionServerCall(serviceName, originalMethodName, codec, invoker, frameworkModel, createHttpChannelObserver(), Collections.emptyList());
        }
    }

    @Override
    public void onData(MESSAGE message) {
        //decode message
        InputStream body = message.getBody();
        serverCall.onMessageAvailable(body);
    }

    protected abstract HttpChannelObserver createHttpChannelObserver();

    protected PathResolver getPathResolver() {
        return pathResolver;
    }

    protected HttpMessageCodec getCodec() {
        return codec;
    }
}
