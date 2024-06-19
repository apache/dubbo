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
package org.apache.dubbo.rpc.protocol.tri.test;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.Constants;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessage;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessageFrame;
import org.apache.dubbo.remoting.http12.message.HttpMessageDecoder;
import org.apache.dubbo.remoting.http12.message.HttpMessageEncoder;
import org.apache.dubbo.remoting.http12.message.codec.JsonCodec;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.rpc.protocol.tri.RpcInvocationBuildContext;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.rest.RestHttpMessageCodec;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RequestUtils;
import org.apache.dubbo.rpc.protocol.tri.test.TestRunnerBuilder.TProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TestRunnerImpl implements TestRunner {

    static final Http2InputMessage END = new Http2InputMessageFrame(new ByteArrayInputStream(new byte[0]), true);

    private final FrameworkModel frameworkModel;
    private final ApplicationModel applicationModel;

    TestRunnerImpl(List<TProvider<?>> providers) {
        frameworkModel = FrameworkModel.defaultModel();
        applicationModel = frameworkModel.newApplication();
        Protocol protocol = applicationModel.getExtensionLoader(Protocol.class).getExtension(TestProtocol.NAME);
        ProxyFactory proxy =
                applicationModel.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        for (TProvider<?> provider : providers) {
            registerProvider(provider, protocol, proxy);
        }
    }

    private <T> void registerProvider(TProvider<T> provider, Protocol protocol, ProxyFactory proxy) {
        Class<T> type = provider.type;
        String typeName = type.getName();
        Map<String, String> parameters = new LinkedHashMap<>(provider.parameters);
        parameters.put(CommonConstants.INTERFACE_KEY, typeName);
        String contextPath = parameters.get(Constants.CONTEXTPATH_KEY);
        String path = contextPath == null ? typeName : contextPath + '/' + typeName;
        URL providerUrl = new URL(TestProtocol.NAME, TestProtocol.HOST, TestProtocol.PORT, path, parameters);
        ModuleServiceRepository serviceRepository =
                applicationModel.getDefaultModule().getServiceRepository();
        ServiceDescriptor serviceDescriptor = serviceRepository.registerService(type);
        ProviderModel providerModel = new ProviderModel(
                providerUrl.getServiceKey(),
                provider.service,
                serviceDescriptor,
                new ServiceMetadata(),
                ClassUtils.getClassLoader(type));
        serviceRepository.registerProvider(providerModel);
        providerUrl = providerUrl.setServiceModel(providerModel);
        protocol.export(proxy.getInvoker(provider.service, type, providerUrl));
    }

    @Override
    @SuppressWarnings("unchecked")
    public TestResponse run(TestRequest request) {
        MockH2StreamChannel channel = new MockH2StreamChannel();
        URL url = new URL(TestProtocol.NAME, TestProtocol.HOST, TestProtocol.PORT, request.getProviderParams());
        TestServerTransportListener listener = new TestServerTransportListener(channel, url, frameworkModel);

        if (request.getMethod() == null) {
            request.setMethod(HttpMethods.GET.name());
        }

        String path = request.getPath();
        Assert.notNull(path, "path is required");

        if (!request.getParams().isEmpty()) {
            StringBuilder sb = new StringBuilder(path);
            boolean hasQuery = path.indexOf('?') != -1;
            for (Map.Entry<String, Object> entry : request.getParams().entrySet()) {
                String key = RequestUtils.encodeURL(entry.getKey());
                Object value = entry.getValue();
                if (value instanceof List) {
                    for (Object obj : (List<Object>) value) {
                        if (obj != null) {
                            if (hasQuery) {
                                sb.append('&');
                            } else {
                                hasQuery = true;
                                sb.append('?');
                            }
                            sb.append(key).append('=').append(RequestUtils.encodeURL(obj.toString()));
                        }
                    }
                } else if (value instanceof Object[]) {
                    for (Object obj : (Object[]) value) {
                        if (obj != null) {
                            if (hasQuery) {
                                sb.append('&');
                            } else {
                                hasQuery = true;
                                sb.append('?');
                            }
                            sb.append(key).append('=').append(RequestUtils.encodeURL(obj.toString()));
                        }
                    }
                } else {
                    if (hasQuery) {
                        sb.append('&');
                    } else {
                        hasQuery = true;
                        sb.append('?');
                    }
                    sb.append(key);
                    if (value != null) {
                        sb.append('=').append(RequestUtils.encodeURL(value.toString()));
                    }
                }
            }
            request.setPath(sb.toString());
        }

        if (!request.getCookies().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : request.getCookies().entrySet()) {
                sb.append(entry.getKey())
                        .append('=')
                        .append(RequestUtils.encodeURL(entry.getValue()))
                        .append(';');
            }
            request.setHeader("cookie", sb.toString());
        }

        listener.onMetadata(request.toMetadata());
        RpcInvocationBuildContext context = listener.getContext();
        HttpMessageDecoder decoder = JsonCodec.INSTANCE;
        if (context != null) {
            HttpMessageEncoder encoder = context.getHttpMessageEncoder();
            decoder = context.getHttpMessageDecoder();
            HttpRequest hRequest = (HttpRequest) context.getAttributes().get(TripleConstant.HTTP_REQUEST_KEY);
            if (CollectionUtils.isEmpty(request.getBodies())) {
                if (HttpMethods.supportBody(hRequest.method())) {
                    listener.onData(END);
                }
            } else {
                for (Object body : request.getBodies()) {
                    byte[] bytes;
                    if (body instanceof String) {
                        bytes = ((String) body).getBytes(StandardCharsets.UTF_8);
                    } else {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream(256);
                        encoder.encode(bos, body);
                        bytes = bos.toByteArray();
                    }
                    listener.onData(new Http2InputMessageFrame(new ByteArrayInputStream(bytes)));
                }
                listener.onData(END);
            }

            if (encoder instanceof RestHttpMessageCodec) {
                encoder = ((RestHttpMessageCodec) encoder).getMessageEncoder();
            }
            if (encoder instanceof HttpMessageDecoder) {
                decoder = (HttpMessageDecoder) encoder;
            }
        }
        return new TestResponse(channel.getHttpMetadata().headers(), channel.getBodies(), decoder);
    }

    @Override
    public <T> T run(TestRequest request, Class<T> type) {
        return run(request).getBody(type);
    }

    @Override
    public void destroy() {
        applicationModel.destroy();
    }
}