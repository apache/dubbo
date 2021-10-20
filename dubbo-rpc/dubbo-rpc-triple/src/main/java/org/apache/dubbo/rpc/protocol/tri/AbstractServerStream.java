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
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.rpc.HeaderFilter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.FrameworkServiceRepository;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.triple.TripleWrapper;

import com.google.protobuf.Message;
import io.netty.handler.codec.http2.Http2Error;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import static org.apache.dubbo.common.constants.CommonConstants.HEADER_FILTER_KEY;

public abstract class AbstractServerStream extends AbstractStream implements Stream {

    private final ProviderModel providerModel;
    private List<MethodDescriptor> methodDescriptors;
    private Invoker<?> invoker;
    private final List<HeaderFilter> headerFilters;

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

    public Invoker<?> getInvoker() {
        return invoker;
    }

    public List<HeaderFilter> getHeaderFilters() {
        return headerFilters;
    }

    public ProviderModel getProviderModel() {
        return providerModel;
    }

    protected RpcInvocation buildInvocation(Metadata metadata) {
        RpcInvocation inv = new RpcInvocation(getUrl().getServiceModel(),
            getMethodName(), getServiceDescriptor().getServiceName(),
            getUrl().getProtocolServiceKey(), getMethodDescriptor().getParameterClasses(), new Object[0]);
        inv.setTargetServiceUniqueName(getUrl().getServiceKey());
        inv.setReturnTypes(getMethodDescriptor().getReturnTypes());

        final Map<String, Object> attachments = parseMetadataToAttachmentMap(metadata);
        inv.setObjectAttachments(attachments);

        for (HeaderFilter headerFilter : getHeaderFilters()) {
            inv = headerFilter.invoke(getInvoker(), inv);
        }
        return inv;
    }

    protected Object[] deserializeRequest(byte[] data) {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            if (getProviderModel() != null) {
                ClassLoadUtil.switchContextLoader(getProviderModel().getClassLoader());
            }
            if (getMethodDescriptor() == null || getMethodDescriptor().isNeedWrap()) {
                final TripleWrapper.TripleRequestWrapper wrapper = TripleUtil.unpack(data,
                    TripleWrapper.TripleRequestWrapper.class);
                if (!getSerializeType().equals(TripleUtil.convertHessianFromWrapper(wrapper.getSerializeType()))) {
                    transportError(GrpcStatus.fromCode(GrpcStatus.Code.INVALID_ARGUMENT)
                        .withDescription("Received inconsistent serialization type from client, " +
                            "reject to deserialize! Expected:" + getSerializeType() +
                            " Actual:" + TripleUtil.convertHessianFromWrapper(wrapper.getSerializeType())));
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

                return TripleUtil.unwrapReq(getUrl(), wrapper, getMultipleSerialization());
            } else {
                return new Object[]{TripleUtil.unpack(data, getMethodDescriptor().getParameterClasses()[0])};
            }

        } finally {
            ClassLoadUtil.switchContextLoader(tccl);
        }
    }

    /**
     * create basic meta data
     */
    protected Metadata createRequestMeta() {
        Metadata metadata = new DefaultMetadata();
        metadata.putIfNotNull(TripleHeaderEnum.GRPC_ENCODING.getHeader(), super.getCompressor().getMessageEncoding())
            .putIfNotNull(TripleHeaderEnum.GRPC_ACCEPT_ENCODING.getHeader(), Compressor.getAcceptEncoding(getUrl().getOrDefaultFrameworkModel()));
        return metadata;
    }

    protected byte[] encodeResponse(Object value) {
        final com.google.protobuf.Message message;
        if (getMethodDescriptor().isNeedWrap()) {
            message = TripleUtil.wrapResp(getUrl(), getSerializeType(), value, getMethodDescriptor(),
                getMultipleSerialization());
        } else {
            message = (Message) value;
        }
        byte[] out = TripleUtil.pack(message);
        return super.compress(out);
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
    protected void cancelByRemoteReset(Http2Error http2Error) {
        getCancellationContext().cancel(null);
    }


    @Override
    protected void cancelByLocal(Throwable throwable) {
        asTransportObserver().onReset(Http2Error.CANCEL);
    }
}
