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
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceRepository;
import org.apache.dubbo.triple.TripleWrapper;

import com.google.protobuf.Message;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public abstract class AbstractServerStream extends AbstractStream implements Stream {

    protected static final ExecutorRepository EXECUTOR_REPOSITORY =
            ExtensionLoader.getExtensionLoader(ExecutorRepository.class).getDefaultExtension();
    private final ProviderModel providerModel;
    private List<MethodDescriptor> methodDescriptors;
    private Invoker<?> invoker;

    protected AbstractServerStream(URL url) {
        this(url, lookupProviderModel(url));
    }

    protected AbstractServerStream(URL url, ProviderModel providerModel) {
        this(url, lookupExecutor(url, providerModel), providerModel);
    }

    protected AbstractServerStream(URL url, Executor executor, ProviderModel providerModel) {
        super(url, executor);
        this.providerModel = providerModel;
        this.serialize(getUrl().getParameter(Constants.SERIALIZATION_KEY, Constants.DEFAULT_REMOTING_SERIALIZATION));
    }

    private static Executor lookupExecutor(URL url, ProviderModel providerModel) {
        ExecutorService executor = null;
        if (providerModel != null) {
            executor = (ExecutorService) providerModel.getServiceMetadata()
                    .getAttribute(CommonConstants.THREADPOOL_KEY);
        }
        if (executor == null) {
            executor = EXECUTOR_REPOSITORY.getExecutor(url);
        }
        if (executor == null) {
            executor = EXECUTOR_REPOSITORY.createExecutorIfAbsent(url);
        }
        return executor;
    }

    public static AbstractServerStream unary(URL url) {
        return new UnaryServerStream(url);
    }

    public static AbstractServerStream stream(URL url) {
        return new ServerStream(url);
    }

    private static ProviderModel lookupProviderModel(URL url) {
        ServiceRepository repo = ApplicationModel.getServiceRepository();
        final ProviderModel model = repo.lookupExportedService(url.getServiceKey());
        if (model != null) {
            ClassLoadUtil.switchContextLoader(model.getServiceInterfaceClass().getClassLoader());
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

    public ProviderModel getProviderModel() {
        return providerModel;
    }

    protected RpcInvocation buildInvocation(Metadata metadata) {
        RpcInvocation inv = new RpcInvocation();
        inv.setServiceName(getServiceDescriptor().getServiceName());
        inv.setTargetServiceUniqueName(getUrl().getServiceKey());
        inv.setMethodName(getMethodDescriptor().getMethodName());
        inv.setParameterTypes(getMethodDescriptor().getParameterClasses());
        inv.setReturnTypes(getMethodDescriptor().getReturnTypes());

        final Map<String, Object> attachments = parseMetadataToMap(metadata);
        attachments.remove("interface");
        attachments.remove("serialization");
        attachments.remove("te");
        attachments.remove("path");
        attachments.remove(TripleConstant.CONTENT_TYPE_KEY);
        attachments.remove(TripleConstant.SERVICE_GROUP);
        attachments.remove(TripleConstant.SERVICE_VERSION);
        attachments.remove(TripleConstant.MESSAGE_KEY);
        attachments.remove(TripleConstant.STATUS_KEY);
        attachments.remove(TripleConstant.TIMEOUT);
        inv.setObjectAttachments(attachments);

        return inv;
    }

    protected Object[] deserializeRequest(byte[] data) {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            if (getProviderModel() != null) {
                ClassLoadUtil.switchContextLoader(getProviderModel().getServiceInterfaceClass().getClassLoader());
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

                    for (MethodDescriptor descriptor : getMethodDescriptors()) {
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

    protected byte[] encodeResponse(Object value) {
        final com.google.protobuf.Message message;
        if (getMethodDescriptor().isNeedWrap()) {
            message = TripleUtil.wrapResp(getUrl(), getSerializeType(), value, getMethodDescriptor(),
                    getMultipleSerialization());
        } else {
            message = (Message) value;
        }
        return TripleUtil.pack(message);
    }

    @Override
    public void execute(Runnable runnable) {
        try {
            super.execute(runnable);
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

}
