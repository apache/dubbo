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

package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.rpc.HeaderFilter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.FrameworkServiceRepository;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.triple.TripleWrapper;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.Http2Headers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import static org.apache.dubbo.common.constants.CommonConstants.HEADER_FILTER_KEY;

public abstract class AbstractServerStream extends AbstractStream implements Stream {

    private final ProviderModel providerModel;
    private final List<HeaderFilter> headerFilters;
    private ServiceDescriptor serviceDescriptor;
    private List<MethodDescriptor> methodDescriptors;
    private Invoker<?> invoker;

    protected AbstractServerStream(URL url) {
        this(url, lookupProviderModel(url));
    }

    protected AbstractServerStream(URL url, ProviderModel providerModel) {
        this(url, lookupExecutor(providerModel), providerModel);
    }

    protected AbstractServerStream(URL url, Executor executor, ProviderModel providerModel) {
        super(url, executor);
        this.providerModel = providerModel;
        this.serialize(getUrl().getParameter(Constants.SERIALIZATION_KEY, Constants.DEFAULT_REMOTING_SERIALIZATION));
        this.headerFilters = url.getOrDefaultApplicationModel().getExtensionLoader(HeaderFilter.class).getActivateExtension(url, HEADER_FILTER_KEY);
    }

    private static Executor lookupExecutor(ProviderModel providerModel) {
        if (providerModel == null) {
            return null;
        }
        return (ExecutorService) providerModel.getServiceMetadata()
            .getAttribute(CommonConstants.THREADPOOL_KEY);
    }

    public static UnaryServerStream unary(URL url) {
        return new UnaryServerStream(url);
    }

    public static ServerStream stream(URL url) {
        return new ServerStream(url);
    }

    public static AbstractServerStream newServerStream(URL url, boolean unary) {
        return unary ? unary(url) : stream(url);
    }

    private static ProviderModel lookupProviderModel(URL url) {
        FrameworkServiceRepository repo = ScopeModelUtil.getFrameworkModel(url.getScopeModel()).getServiceRepository();
        final ProviderModel model = repo.lookupExportedService(url.getServiceKey());
        if (model != null) {
            ClassLoadUtil.switchContextLoader(model.getClassLoader());
        }
        return model;
    }

    public List<MethodDescriptor> getMethodDescriptors() {
        return methodDescriptors;
    }

    public AbstractServerStream methods(List<MethodDescriptor> methods) {
        this.methodDescriptors = methods;
        return this;
    }

    public ServiceDescriptor getServiceDescriptor() {
        return serviceDescriptor;
    }

    public void setServiceDescriptor(ServiceDescriptor serviceDescriptor) {
        this.serviceDescriptor = serviceDescriptor;
    }

    public Invoker<?> getInvoker() {
        return invoker;
    }

    public List<HeaderFilter> getHeaderFilters() {
        return headerFilters;
    }

    public ProviderModel getProviderModel() {
        return providerModel;
    }

    /**
     * Build the RpcInvocation with metadata and execute headerFilter
     *
     * @param metadata request header
     * @return RpcInvocation
     */
    protected RpcInvocation buildInvocation(Metadata metadata) {
        RpcInvocation inv = new RpcInvocation(getUrl().getServiceModel(),
            getMethodName(), getServiceDescriptor().getServiceName(),
            getUrl().getProtocolServiceKey(), getMethodDescriptor().getRealParameterClasses(), new Object[0]);
        inv.setTargetServiceUniqueName(getUrl().getServiceKey());
        inv.setReturnTypes(getMethodDescriptor().getReturnTypes());

        final Map<String, Object> attachments = parseMetadataToAttachmentMap(metadata);
        inv.setObjectAttachments(attachments);
        invokeHeaderFilter(inv);
        return inv;
    }

    /**
     * Intercept the header to do some validation
     * <p>
     * for example, check the token or a user-defined permission check operation
     *
     * @param inv RPC Invocation
     * @throws RpcException maybe throw rpcException
     */
    protected void invokeHeaderFilter(RpcInvocation inv) throws RpcException {
        for (HeaderFilter headerFilter : getHeaderFilters()) {
            headerFilter.invoke(getInvoker(), inv);
        }
    }

    /**
     * For the unary method, there may be overloaded methods,
     * so need to parse out the Wrapper from the data and continue buildRpcInvocation
     * <p>
     * Also, to prevent serialization attacks, headerFilter needs to be executed
     *
     * @param metadata request headers
     * @param data     request data
     * @return RPC Invocation
     */
    protected RpcInvocation buildUnaryInvocation(Metadata metadata, byte[] data) {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            if (getProviderModel() != null) {
                ClassLoadUtil.switchContextLoader(getProviderModel().getServiceInterfaceClass().getClassLoader());
            }
            // For the Wrapper method,the methodDescriptor needs to get from data, so parse the request first
            if (needDeserializeWrapper(getMethodDescriptor())) {
                // the wrapper structure is first resolved without actual deserialization
                TripleWrapper.TripleRequestWrapper wrapper = deserializeWrapperSetMdIfNeed(data);
                if (wrapper == null) {
                    return null;
                }
                RpcInvocation inv = buildInvocation(metadata);
                inv.setArguments(unwrapReq(getUrl(), wrapper, getMultipleSerialization()));
                return inv;
            } else {
                // Protobuf MethodDescriptor must not be null
                RpcInvocation inv = buildInvocation(metadata);
                inv.setArguments(new Object[]{unpack(data, getMethodDescriptor().getParameterClasses()[0])});
                return inv;
            }
        } catch (RpcException rpcException) {
            // for catch exceptions in headerFilter
            transportError(GrpcStatus.getStatus(rpcException, rpcException.getMessage()));
            return null;
        } catch (Throwable throwable) {
            LOGGER.warn("Decode request failed:", throwable);
            transportError(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                .withDescription("Decode request failed:" + throwable.getMessage()));
            return null;
        } finally {
            ClassLoadUtil.switchContextLoader(tccl);
        }
    }

    /**
     * Deserialize the stream request data
     *
     * @param data request data
     * @return Deserialized object
     */
    protected Object[] deserializeRequest(byte[] data) {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            if (getProviderModel() != null) {
                ClassLoadUtil.switchContextLoader(getProviderModel().getServiceInterfaceClass().getClassLoader());
            }
            if (needDeserializeWrapper(getMethodDescriptor())) {
                TripleWrapper.TripleRequestWrapper wrapper = deserializeWrapperSetMdIfNeed(data);
                if (wrapper == null) {
                    return null;
                }
                return unwrapReq(getUrl(), wrapper, getMultipleSerialization());
            } else {
                // Protobuf MethodDescriptor must not be null
                return new Object[]{unpack(data, getMethodDescriptor().getParameterClasses()[0])};
            }
        } catch (Throwable throwable) {
            LOGGER.warn("Decode request failed:", throwable);
            transportError(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                .withDescription("Decode request failed:" + throwable.getMessage()));
            return null;
        } finally {
            ClassLoadUtil.switchContextLoader(tccl);
        }
    }

    private boolean needDeserializeWrapper(MethodDescriptor md) {
        if (md == null) {
            return true;
        }
        return getMethodDescriptor().isNeedWrap();
    }

    private TripleWrapper.TripleRequestWrapper deserializeWrapperSetMdIfNeed(byte[] data) {
        final TripleWrapper.TripleRequestWrapper wrapper = unpack(data, TripleWrapper.TripleRequestWrapper.class);
        if (!getSerializeType().equals(convertHessianFromWrapper(wrapper.getSerializeType()))) {
            transportError(GrpcStatus.fromCode(GrpcStatus.Code.INVALID_ARGUMENT)
                .withDescription("Received inconsistent serialization type from client, " +
                    "reject to deserialize! Expected:" + getSerializeType() +
                    " Actual:" + convertHessianFromWrapper(wrapper.getSerializeType())));
            return null;
        }
        if (getMethodDescriptor() == null) {
            final String[] paramTypes = wrapper.getArgTypesList().toArray(new String[wrapper.getArgsCount()]);
            // wrapper mode the method can overload so maybe list
            for (MethodDescriptor descriptor : getMethodDescriptors()) {
                // params type is array
                if (Arrays.equals(descriptor.getCompatibleParamSignatures(), paramTypes)) {
                    method(descriptor);
                    break;
                }
            }
            if (getMethodDescriptor() == null) {
                transportError(GrpcStatus.fromCode(GrpcStatus.Code.UNIMPLEMENTED)
                    .withDescription("Method :" + getMethodName() + "[" + Arrays.toString(paramTypes) + "] " +
                        "not found of service:" + getServiceDescriptor().getServiceName()));
                return null;
            }
        }
        return wrapper;
    }

    private Object[] unwrapReq(URL url, TripleWrapper.TripleRequestWrapper wrap,
                               MultipleSerialization multipleSerialization) {
        String serializeType = convertHessianFromWrapper(wrap.getSerializeType());
        try {
            Object[] arguments = new Object[wrap.getArgsCount()];
            for (int i = 0; i < arguments.length; i++) {
                final ByteArrayInputStream bais = new ByteArrayInputStream(wrap.getArgs(i).toByteArray());
                Object obj = multipleSerialization.deserialize(url,
                    serializeType, wrap.getArgTypes(i), bais);
                arguments[i] = obj;
            }
            return arguments;
        } catch (Exception e) {
            throw new RuntimeException("Failed to unwrap req: " + e.getMessage(), e);
        }
    }

    /**
     * create basic meta data
     */
    protected Metadata createResponseMeta() {
        Metadata metadata = new DefaultMetadata();
        metadata.put(Http2Headers.PseudoHeaderName.STATUS.value(), HttpResponseStatus.OK.codeAsText());
        metadata.put(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO);
        metadata.putIfNotNull(TripleHeaderEnum.GRPC_ENCODING.getHeader(), super.getCompressor().getMessageEncoding())
            .putIfNotNull(TripleHeaderEnum.GRPC_ACCEPT_ENCODING.getHeader(), getAcceptEncoding());
        return metadata;
    }

    protected byte[] encodeResponse(Object value) {
        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            if (getProviderModel() != null) {
                ClassLoadUtil.switchContextLoader(getProviderModel().getServiceInterfaceClass().getClassLoader());
            }
            final Message message;
            if (getMethodDescriptor().isNeedWrap()) {
                message = wrapResp(getUrl(), getSerializeType(), value, getMethodDescriptor(),
                    getMultipleSerialization());
            } else {
                message = (Message) value;
            }
            byte[] out = pack(message);
            return super.compress(out);
        } catch (Throwable throwable) {
            LOGGER.error("Encode Response data error ", throwable);
            transportError(GrpcStatus.fromCode(GrpcStatus.Code.UNKNOWN)
                .withCause(throwable)
                .withDescription("Encode Response data error"));
            return null;
        } finally {
            ClassLoadUtil.switchContextLoader(tccl);
        }
    }

    @Override
    public void execute(Runnable runnable) {
        try {
            super.execute(() -> {
                try {
                    runnable.run();
                } catch (Throwable t) {
                    LOGGER.error("Exception processing triple message", t);
                    transportError(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                        .withDescription("Exception in invoker chain :" + t.getMessage())
                        .withCause(t));
                }
            });
        } catch (RejectedExecutionException e) {
            LOGGER.error("Provider's thread pool is full", e);
            transportError(GrpcStatus.fromCode(GrpcStatus.Code.RESOURCE_EXHAUSTED)
                .withDescription("Provider's thread pool is full"));
        } catch (Throwable t) {
            LOGGER.error("Provider submit request to thread pool error ", t);
            transportError(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                .withCause(t)
                .withDescription("Provider's error"));
        }
    }

    public AbstractServerStream service(ServiceDescriptor sd) {
        setServiceDescriptor(sd);
        return this;
    }

    public AbstractServerStream invoker(Invoker<?> invoker) {
        this.invoker = invoker;
        return this;
    }

    @Override
    protected void cancelByRemoteReset() {
        getCancellationContext().cancel(null);
    }


    @Override
    protected void cancelByLocal(Throwable throwable) {
        inboundTransportObserver()
            .onError(GrpcStatus.fromCode(GrpcStatus.Code.CANCELLED)
                .withCause(throwable));
    }

    public TripleWrapper.TripleResponseWrapper wrapResp(URL url, String serializeType, Object resp,
                                                        MethodDescriptor desc,
                                                        MultipleSerialization multipleSerialization) {
        try {
            final TripleWrapper.TripleResponseWrapper.Builder builder = TripleWrapper.TripleResponseWrapper.newBuilder()
                .setType(desc.getReturnClass().getName())
                .setSerializeType(convertHessianToWrapper(serializeType));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            multipleSerialization.serialize(url, serializeType, desc.getReturnClass().getName(), resp, bos);
            builder.setData(ByteString.copyFrom(bos.toByteArray()));
            bos.close();
            return builder.build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to pack wrapper req", e);
        }
    }

}
