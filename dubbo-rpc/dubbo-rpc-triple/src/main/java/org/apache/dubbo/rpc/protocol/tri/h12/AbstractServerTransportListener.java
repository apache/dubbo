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
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.JsonUtils;
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
import org.apache.dubbo.remoting.http12.message.HttpMessageCodecFactory;
import org.apache.dubbo.remoting.http12.message.MethodMetadata;
import org.apache.dubbo.rpc.HeaderFilter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.PathResolver;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.TripleProtocol;
import org.apache.dubbo.rpc.protocol.tri.stream.StreamUtils;
import org.apache.dubbo.rpc.service.ServiceDescriptorInternalCache;
import org.apache.dubbo.rpc.stub.StubSuppliers;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static org.apache.dubbo.common.constants.CommonConstants.HEADER_FILTER_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_PARSE;

public abstract class AbstractServerTransportListener<HEADER extends RequestMetadata, MESSAGE extends HttpInputMessage> implements HttpTransportListener<HEADER, MESSAGE> {

    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(AbstractServerTransportListener.class);

    private final PathResolver pathResolver;

    private final FrameworkModel frameworkModel;

    private final URL url;

    private final HttpChannel httpChannel;

    private final List<HeaderFilter> headerFilters;

    private HttpMessageCodec httpMessageCodec;

    private Invoker<?> invoker;

    private ServiceDescriptor serviceDescriptor;

    private MethodDescriptor methodDescriptor;

    private RpcInvocation rpcInvocation;

    private MethodMetadata methodMetadata;

    private HEADER httpMetadata;

    private Executor executor;

    private boolean hasStub;

    private HttpMessageListener httpMessageListener;

    public AbstractServerTransportListener(FrameworkModel frameworkModel, URL url, HttpChannel httpChannel) {
        this.frameworkModel = frameworkModel;
        this.url = url;
        this.httpChannel = httpChannel;
        this.pathResolver = frameworkModel.getExtensionLoader(PathResolver.class).getDefaultExtension();
        this.headerFilters = frameworkModel.getExtensionLoader(HeaderFilter.class).getActivateExtension(url, HEADER_FILTER_KEY);
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
//            LOGGER.error("initialize executor fail.", throwable);
            onError(throwable);
            return;
        }
        if (this.executor == null) {
//            LOGGER.error("executor must be not null.");
            onError(new NullPointerException("initializeExecutor return null"));
            return;
        }
        executor.execute(() -> {
            try {
                doOnMetadata(metadata);
            } catch (Throwable throwable) {
//                LOGGER.error("server internal error", throwable);
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

        //2. check service
        String[] parts = path.split("/");
        if (parts.length != 3) {
            throw new IllegalPathException(path);
        }
        String serviceName = parts[1];
        this.hasStub = pathResolver.hasNativeStub(path);
        this.invoker = getInvoker(metadata, serviceName);
        if (invoker == null) {
            throw new UnimplementedException(serviceName);
        }
        HttpMessageCodec httpMessageCodec = determineHttpMessageCodec(contentType);
        if (httpMessageCodec == null) {
            throw new UnsupportedMediaTypeException(contentType);
        }
        this.httpMessageCodec = httpMessageCodec;
        setServiceDescriptor(findServiceDescriptor(invoker, serviceName, hasStub));
        setHttpMessageListener(newHttpMessageListener());
        onMetadataCompletion(metadata);
    }

    protected abstract HttpMessageListener newHttpMessageListener();

    @Override
    public void onData(MESSAGE message) {
        this.executor.execute(() -> {
            try {
                doOnData(message);
            } catch (Throwable e) {
//                LOGGER.error("server internal error", e);
                onError(e);
            }
        });
    }

    protected void doOnData(MESSAGE message) {
        //decode message
        onPrepareData(message);
        InputStream body = message.getBody();
        httpMessageListener.onMessage(body);
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
        for (HttpMessageCodecFactory httpMessageCodecFactory : frameworkModel.getExtensionLoader(HttpMessageCodecFactory.class).getActivateExtensions()) {
            if (httpMessageCodecFactory.support(contentType)) {
                return httpMessageCodecFactory.createCodec(invoker.getUrl(), frameworkModel);
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

    protected static MethodDescriptor findMethodDescriptor(ServiceDescriptor serviceDescriptor, String originalMethodName, boolean hasStub) throws UnimplementedException {
        MethodDescriptor result;
        if (hasStub) {
            result = serviceDescriptor.getMethods(originalMethodName).get(0);
        } else {
            result = findReflectionMethodDescriptor(serviceDescriptor, originalMethodName);
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
        Map<String, String> headers = getHttpMetadata().headers().toSingleValueMap();
        Map<String, Object> requestMetadata = headersToMap(headers, () -> {
            return Optional.ofNullable(headers.get(TripleHeaderEnum.TRI_HEADER_CONVERT.getHeader()))
                .map(CharSequence::toString)
                .orElse(null);
        });
        inv.setObjectAttachments(StreamUtils.toAttachments(requestMetadata));

        inv.put("tri.remote.address", httpChannel.remoteAddress());
        //customizer RpcInvocation
        headerFilters.forEach(f -> f.invoke(invoker, inv));
        return inv;
    }

    protected static ServiceDescriptor getStubServiceDescriptor(URL url, String serviceName) {
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

    protected static ServiceDescriptor getReflectionServiceDescriptor(URL url) {
        ProviderModel providerModel = (ProviderModel) url.getServiceModel();
        if (providerModel == null || providerModel.getServiceModel() == null) {
            return null;
        }
        return providerModel.getServiceModel();
    }

    protected static boolean isEcho(String methodName) {
        return CommonConstants.$ECHO.equals(methodName);
    }

    protected static boolean isGeneric(String methodName) {
        return CommonConstants.$INVOKE.equals(methodName) || CommonConstants.$INVOKE_ASYNC.equals(
            methodName);
    }

    protected static MethodDescriptor findReflectionMethodDescriptor(ServiceDescriptor serviceDescriptor, String methodName) {
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

    public void setServiceDescriptor(ServiceDescriptor serviceDescriptor) {
        this.serviceDescriptor = serviceDescriptor;
    }

    public void setMethodDescriptor(MethodDescriptor methodDescriptor) {
        this.methodDescriptor = methodDescriptor;
    }

    public void setMethodMetadata(MethodMetadata methodMetadata) {
        this.methodMetadata = methodMetadata;
    }

    protected RpcInvocation getRpcInvocation() {
        return rpcInvocation;
    }

    public void setRpcInvocation(RpcInvocation rpcInvocation) {
        this.rpcInvocation = rpcInvocation;
    }

    protected MethodMetadata getMethodMetadata() {
        return methodMetadata;
    }

    protected HttpMessageCodec getHttpMessageCodec() {
        return httpMessageCodec;
    }

    protected void setHttpMessageListener(HttpMessageListener httpMessageListener) {
        this.httpMessageListener = httpMessageListener;
    }

    protected HttpMessageListener getHttpMessageListener() {
        return httpMessageListener;
    }

    protected PathResolver getPathResolver() {
        return pathResolver;
    }

    protected final URL getUrl() {
        return url;
    }

    public boolean isHasStub() {
        return hasStub;
    }

    protected Map<String, Object> headersToMap(Map<String, String> headers, Supplier<Object> convertUpperHeaderSupplier) {
        if (headers == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> attachments = new HashMap<>(headers.size());
        for (Map.Entry<String, String> header : headers.entrySet()) {
            String key = header.getKey();
            if (key.endsWith(TripleConstant.HEADER_BIN_SUFFIX)
                && key.length() > TripleConstant.HEADER_BIN_SUFFIX.length()) {
                try {
                    String realKey = key.substring(0,
                        key.length() - TripleConstant.HEADER_BIN_SUFFIX.length());
                    byte[] value = StreamUtils.decodeASCIIByte(header.getValue());
                    attachments.put(realKey, value);
                } catch (Exception e) {
                    LOGGER.error(PROTOCOL_FAILED_PARSE, "", "", "Failed to parse response attachment key=" + key, e);
                }
            } else {
                attachments.put(key, header.getValue());
            }
        }

        // try converting upper key
        Object obj = convertUpperHeaderSupplier.get();
        if (obj == null) {
            return attachments;
        }
        if (obj instanceof String) {
            String json = TriRpcStatus.decodeMessage((String) obj);
            Map<String, String> map = JsonUtils.toJavaObject(json, Map.class);
            for (Map.Entry<String, String> entry : map.entrySet()) {
                Object val = attachments.remove(entry.getKey());
                if (val != null) {
                    attachments.put(entry.getValue(), val);
                }
            }
        } else {
            // If convertUpperHeaderSupplier does not return String, just fail...
            // Internal invocation, use INTERNAL_ERROR instead.

            LOGGER.error(INTERNAL_ERROR, "wrong internal invocation", "", "Triple convertNoLowerCaseHeader error, obj is not String");
        }
        return attachments;
    }
}
