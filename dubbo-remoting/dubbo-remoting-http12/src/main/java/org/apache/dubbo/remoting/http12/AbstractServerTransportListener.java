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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.http12.exception.UnimplementedException;
import org.apache.dubbo.remoting.http12.exception.UnsupportedMediaTypeException;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.JsonMethodDescriptorDecoder;
import org.apache.dubbo.remoting.http12.util.RequestUtil;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.PathResolver;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.PackableMethod;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.service.ServiceDescriptorInternalCache;
import org.apache.dubbo.rpc.stub.StubSuppliers;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author icodening
 * @date 2023.05.31
 */
public abstract class AbstractServerTransportListener<HEADER extends RequestMetadata, MESSAGE extends HttpMessage> implements ServerTransportListener<HEADER, MESSAGE> {

    private final PathResolver pathResolver;

    private final Map<String, HttpMessageCodec> codecs;

    private final FrameworkModel frameworkModel;

    private HttpMessageCodec codec;

    protected ServerCall.Listener serverCallListener;

    private ServiceDescriptor serviceDescriptor;

    private MethodDescriptor methodDescriptor;

    private PackableMethod packableMethod;

    public AbstractServerTransportListener(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
        this.pathResolver = frameworkModel.getExtensionLoader(PathResolver.class).getDefaultExtension();
        this.codecs = frameworkModel.getExtensionLoader(HttpMessageCodec.class).getActivateExtensions().stream().collect(Collectors.toMap(httpMessageCodec -> httpMessageCodec.contentType().getName(), Function.identity()));
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
            throw new UnsupportedMediaTypeException(contentType);
        }
        this.codec = httpMessageCodec;
        String serviceName = parts[1];
        String originalMethodName = parts[2];
        boolean hasStub = pathResolver.hasNativeStub(path);
        Invoker<?> invoker = pathResolver.resolve(serviceName);
        Map<String, Object> attachment = RequestUtil.headerToAttachment(headers);
        //create ServerCallListener
        ServiceDescriptor serviceDescriptor;
        MethodDescriptor methodDescriptor;
        if (hasStub) {
            serviceDescriptor = getStubServiceDescriptor(invoker.getUrl(), serviceName);
            if (serviceDescriptor == null) {
                throw new UnimplementedException("service:" + serviceName);
            }
            methodDescriptor = serviceDescriptor.getMethods(originalMethodName).get(0);
        } else {
            serviceDescriptor = getReflectionServiceDescriptor(invoker.getUrl());
            if (serviceDescriptor == null) {
                throw new UnimplementedException("service:" + serviceName);
            }
            methodDescriptor = findReflectionMethodDescriptor(serviceDescriptor, originalMethodName, invoker);
        }
        if (methodDescriptor == null) {
            throw new UnimplementedException("method:" + originalMethodName);
        }
        this.serviceDescriptor = serviceDescriptor;
        this.methodDescriptor = methodDescriptor;
        RpcInvocation rpcInvocation = buildRpcInvocation(invoker, serviceDescriptor, methodDescriptor);
        this.serverCallListener = startListener(rpcInvocation, methodDescriptor, invoker);
    }

    @Override
    public void onData(MESSAGE message) {
        //decode message
        try {
            InputStream body = message.getBody();
            //TODO 根据content-type选择对应的
//            packableMethod.getRequestUnpack().unpack()
            JsonMethodDescriptorDecoder jsonMethodDescriptorDecoder = new JsonMethodDescriptorDecoder();
            Object[] decodeParameters = jsonMethodDescriptorDecoder.decode(body, methodDescriptor);
            this.serverCallListener.onMessage(decodeParameters);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected RpcInvocation buildRpcInvocation(Invoker<?> invoker,
                                               ServiceDescriptor serviceDescriptor,
                                               MethodDescriptor methodDescriptor) {
        final URL url = invoker.getUrl();
        RpcInvocation inv = new RpcInvocation(url.getServiceModel(),
            methodDescriptor.getMethodName(),
            serviceDescriptor.getInterfaceName(), url.getProtocolServiceKey(),
            methodDescriptor.getParameterClasses(),
            new Object[0]);
        inv.setTargetServiceUniqueName(url.getServiceKey());
        inv.setReturnTypes(methodDescriptor.getReturnTypes());
        //        headerFilters.forEach(f -> f.invoke(invoker, invocation));
        return inv;
    }

    private boolean isEcho(String methodName) {
        return CommonConstants.$ECHO.equals(methodName);
    }

    private boolean isGeneric(String methodName) {
        return CommonConstants.$INVOKE.equals(methodName) || CommonConstants.$INVOKE_ASYNC.equals(
            methodName);
    }

    protected abstract ServerCall.Listener startListener(RpcInvocation invocation,
                                                         MethodDescriptor methodDescriptor,
                                                         Invoker<?> invoker);

    protected PathResolver getPathResolver() {
        return pathResolver;
    }

    protected HttpMessageCodec getCodec() {
        return codec;
    }

    private static ServiceDescriptor getStubServiceDescriptor(URL url, String serviceName) {
        ServiceDescriptor serviceDescriptor;
        if (url.getServiceModel() != null) {
            serviceDescriptor = url
                .getServiceModel()
                .getServiceModel();
        } else {
            serviceDescriptor = StubSuppliers.getServiceDescriptor(serviceName);
        }
        return serviceDescriptor;
    }

    private static ServiceDescriptor getReflectionServiceDescriptor(URL url) {
        ProviderModel providerModel = (ProviderModel) url.getServiceModel();
        if (providerModel == null || providerModel.getServiceModel() == null) {
            return null;
        }
        return providerModel.getServiceModel();
    }

    private MethodDescriptor findReflectionMethodDescriptor(ServiceDescriptor serviceDescriptor, String methodName, Invoker<?> invoker) {
        MethodDescriptor methodDescriptor = null;
        if (isGeneric(methodName)) {
            // There should be one and only one
            methodDescriptor = ServiceDescriptorInternalCache.genericService()
                .getMethods(methodName).get(0);
        } else if (isEcho(methodName)) {
            // There should be one and only one
            return ServiceDescriptorInternalCache.echoService().getMethods(methodName)
                .get(0);
        } else {
            List<MethodDescriptor> methodDescriptors = serviceDescriptor.getMethods(methodName);
            // try lower-case method
            if (CollectionUtils.isEmpty(methodDescriptors)) {
                final String lowerMethod =
                    Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);
                methodDescriptors = serviceDescriptor.getMethods(lowerMethod);
            }
            if (CollectionUtils.isEmpty(methodDescriptors)) {
                return null;
            }
            // In most cases there is only one method
            if (methodDescriptors.size() == 1) {
                methodDescriptor = methodDescriptors.get(0);
            }
            // generated unary method ,use unary type
            // Response foo(Request)
            // void foo(Request,StreamObserver<Response>)
            if (methodDescriptors.size() == 2) {
                if (methodDescriptors.get(1).getRpcType() == MethodDescriptor.RpcType.SERVER_STREAM) {
                    methodDescriptor = methodDescriptors.get(0);
                } else if (methodDescriptors.get(0).getRpcType() == MethodDescriptor.RpcType.SERVER_STREAM) {
                    methodDescriptor = methodDescriptors.get(1);
                }
            }
        }
        return methodDescriptor;
    }
}
