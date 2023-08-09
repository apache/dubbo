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
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.http12.HttpChannel;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpInputMessage;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.HttpTransportListener;
import org.apache.dubbo.remoting.http12.RequestMetadata;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.remoting.http12.exception.IllegalPathException;
import org.apache.dubbo.remoting.http12.exception.UnimplementedException;
import org.apache.dubbo.remoting.http12.exception.UnsupportedMediaTypeException;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.ListeningDecoder;
import org.apache.dubbo.remoting.http12.message.MethodMetadata;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.PathResolver;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.TripleProtocol;
import org.apache.dubbo.rpc.service.ServiceDescriptorInternalCache;
import org.apache.dubbo.rpc.stub.StubSuppliers;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.Executor;

public abstract class AbstractServerTransportListener<HEADER extends RequestMetadata, MESSAGE extends HttpInputMessage> implements HttpTransportListener<HEADER, MESSAGE> {

    private final PathResolver pathResolver;

    private final FrameworkModel frameworkModel;

    private final HttpChannel httpChannel;

    private HttpMessageCodec httpMessageCodec;

    private Invoker<?> invoker;

    private ServiceDescriptor serviceDescriptor;

    private MethodDescriptor methodDescriptor;

    private RpcInvocation rpcInvocation;

    private MethodMetadata methodMetadata;

    private ListeningDecoder listeningDecoder;

    private HEADER httpMetadata;

    private Executor executor;

    public AbstractServerTransportListener(FrameworkModel frameworkModel, HttpChannel httpChannel) {
        this.frameworkModel = frameworkModel;
        this.httpChannel = httpChannel;
        this.pathResolver = frameworkModel.getExtensionLoader(PathResolver.class).getDefaultExtension();
    }

    protected Executor initializeExecutor(HEADER metadata) {
        //default direct executor
        return Runnable::run;
    }

    @Override
    public void onMetadata(HEADER metadata) {
        try {
            this.executor = initializeExecutor(metadata);
        } catch (Throwable throwable) {
            onError(throwable);
            return;
        }
        if (this.executor == null) {
            onError(new NullPointerException("initializeExecutor return null"));
            return;
        }
        executor.execute(() -> {
            try {
                doOnMetadata(metadata);
            } catch (Throwable throwable) {
                onError(throwable);
            }
        });
    }

    protected void doOnMetadata(HEADER metadata) {
        onPrepareMetadata(metadata);
        this.httpMetadata = metadata;
        String path = metadata.path();
        HttpHeaders headers = metadata.headers();
        //1.check necessary header
        String contentType = headers.getFirst(HttpHeaderNames.CONTENT_TYPE.getName());
        if (contentType == null) {
            throw new UnsupportedMediaTypeException("'" + HttpHeaderNames.CONTENT_TYPE.getName() + "' must be not null.");
        }
        HttpMessageCodec httpMessageCodec = determineHttpMessageCodec(contentType);
        if (httpMessageCodec == null) {
            throw new UnsupportedMediaTypeException(contentType);
        }
        this.httpMessageCodec = httpMessageCodec;

        //2. check service
        String[] parts = path.split("/");
        if (parts.length != 3) {
            throw new IllegalPathException(path);
        }
        String serviceName = parts[1];
        String originalMethodName = parts[2];
        boolean hasStub = pathResolver.hasNativeStub(path);
        this.invoker = getInvoker(metadata, serviceName);
        if (invoker == null) {
            throw new UnimplementedException(serviceName);
        }
        this.serviceDescriptor = findServiceDescriptor(invoker, serviceName, hasStub);
        this.methodDescriptor = findMethodDescriptor(serviceDescriptor, originalMethodName, hasStub);
        this.methodMetadata = MethodMetadata.fromMethodDescriptor(methodDescriptor);
        this.rpcInvocation = buildRpcInvocation(invoker, serviceDescriptor, methodDescriptor);
        this.listeningDecoder = newListeningDecoder(this.httpMessageCodec, this.methodMetadata.getActualRequestTypes());
        onMetadataCompletion(metadata);
    }

    @Override
    public void onData(MESSAGE message) {
        this.executor.execute(() -> {
            try {
                doOnData(message);
            } catch (Throwable e) {
                onError(e);
            }
        });
    }

    protected void doOnData(MESSAGE message) {
        //decode message
        onPrepareData(message);
        InputStream body = message.getBody();
        this.listeningDecoder.decode(body);
        onDataCompletion(message);
    }

    protected void onPrepareMetadata(HEADER header) {
        //default no op
    }

    protected void onMetadataCompletion(HEADER metadata) {
        //default no op
    }

    protected void onPrepareData(MESSAGE message) {
        //default no op
    }

    protected void onDataCompletion(MESSAGE message) {
        //default no op
    }

    protected void onError(Throwable throwable) {
        //default rethrow
        if (throwable instanceof RuntimeException) {
            throw ((RuntimeException) throwable);
        }
        if (throwable instanceof InvocationTargetException) {
            Throwable targetException = ((InvocationTargetException) throwable).getTargetException();
            if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            } else if (targetException instanceof Error) {
                throw (Error) targetException;
            }
        }
        throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR.getCode(), throwable);
    }

    protected abstract ListeningDecoder newListeningDecoder(HttpMessageCodec codec, Class<?>[] actualRequestTypes);


    private Invoker<?> getInvoker(HEADER metadata, String serviceName) {
        HttpHeaders headers = metadata.headers();
        final String version =
            headers.containsKey(TripleHeaderEnum.SERVICE_VERSION.getHeader()) ? headers.get(
                TripleHeaderEnum.SERVICE_VERSION.getHeader()).toString() : null;
        final String group =
            headers.containsKey(TripleHeaderEnum.SERVICE_GROUP.getHeader()) ? headers.get(
                TripleHeaderEnum.SERVICE_GROUP.getHeader()).toString() : null;
        final String key = URL.buildKey(serviceName, group, version);
        Invoker<?> invoker = pathResolver.resolve(key);
        if (invoker == null && TripleProtocol.RESOLVE_FALLBACK_TO_DEFAULT) {
            invoker = pathResolver.resolve(URL.buildKey(serviceName, group, "1.0.0"));
        }
        if (invoker == null && TripleProtocol.RESOLVE_FALLBACK_TO_DEFAULT) {
            invoker = pathResolver.resolve(serviceName);
        }
        return invoker;
    }


    protected HttpMessageCodec determineHttpMessageCodec(String contentType) {
        for (HttpMessageCodec httpMessageCodec : frameworkModel.getExtensionLoader(HttpMessageCodec.class).getActivateExtensions()) {
            if (httpMessageCodec.support(contentType)) {
                return httpMessageCodec;
            }
        }
        return null;
    }

    private static ServiceDescriptor findServiceDescriptor(Invoker<?> invoker, String serviceName, boolean hasStub) throws UnimplementedException {
        ServiceDescriptor result;
        if (hasStub) {
            result = getStubServiceDescriptor(invoker.getUrl(), serviceName);
        } else {
            result = getReflectionServiceDescriptor(invoker.getUrl());
        }
        if (result == null) {
            throw new UnimplementedException("service:" + serviceName);
        }
        return result;
    }

    private static MethodDescriptor findMethodDescriptor(ServiceDescriptor serviceDescriptor, String originalMethodName, boolean hasStub) throws UnimplementedException {
        MethodDescriptor result;
        if (hasStub) {
            result = serviceDescriptor.getMethods(originalMethodName).get(0);
        } else {
            result = findReflectionMethodDescriptor(serviceDescriptor, originalMethodName);
        }
        if (result == null) {
            throw new UnimplementedException("method:" + originalMethodName);
        }
        return result;
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
        //TODO header to attachment
//        Map<String, String> headers = getHttpMetadata().headers().toSingleValueMap();
//        Map<String, Object> requestMetadata = headersToMap(headers, () -> {
//            return Optional.ofNullable(headers.get(TripleHeaderEnum.TRI_HEADER_CONVERT.getHeader()))
//                .map(CharSequence::toString)
//                .orElse(null);
//        });
//        inv.setObjectAttachments(StreamUtils.toAttachments(requestMetadata));

        inv.put("tri.remote.address", httpChannel.remoteAddress());
        //customizer RpcInvocation
        //        headerFilters.forEach(f -> f.invoke(invoker, invocation));
        return inv;
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

    private static boolean isEcho(String methodName) {
        return CommonConstants.$ECHO.equals(methodName);
    }

    private static boolean isGeneric(String methodName) {
        return CommonConstants.$INVOKE.equals(methodName) || CommonConstants.$INVOKE_ASYNC.equals(
            methodName);
    }

    private static MethodDescriptor findReflectionMethodDescriptor(ServiceDescriptor serviceDescriptor, String methodName) {
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

    protected FrameworkModel getFrameworkModel() {
        return frameworkModel;
    }

    protected HEADER getHttpMetadata() {
        return httpMetadata;
    }

    protected Invoker<?> getInvoker() {
        return invoker;
    }

    protected ServiceDescriptor getServiceDescriptor() {
        return serviceDescriptor;
    }

    protected MethodDescriptor getMethodDescriptor() {
        return methodDescriptor;
    }

    protected RpcInvocation getRpcInvocation() {
        return rpcInvocation;
    }

    protected MethodMetadata getMethodMetadata() {
        return methodMetadata;
    }

    protected HttpMessageCodec getHttpMessageCodec() {
        return httpMessageCodec;
    }

    protected ListeningDecoder getListeningDecoder() {
        return this.listeningDecoder;
    }

}
