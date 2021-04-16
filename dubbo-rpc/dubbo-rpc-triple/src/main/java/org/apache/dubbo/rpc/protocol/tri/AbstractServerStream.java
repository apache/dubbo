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
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceRepository;
import org.apache.dubbo.triple.TripleWrapper;

import com.google.protobuf.Message;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public abstract class AbstractServerStream extends AbstractStream implements Stream {
        protected static final ExecutorRepository EXECUTOR_REPOSITORY =
        ExtensionLoader.getExtensionLoader(ExecutorRepository.class).getDefaultExtension();

    private final ProviderModel providerModel;
    private Invoker<?> invoker;

    protected AbstractServerStream(URL url) {
        super(url);
        this.providerModel = lookupProviderModel(url);
    }

    public static AbstractServerStream unary(URL url) {
        return new UnaryServerStream(url);
    }

    public static AbstractServerStream stream(URL url) {
        return new ServerStream(url);
    }

    public Invoker<?> getInvoker() {
        return invoker;
    }

    public ProviderModel getProviderModel() {
        return providerModel;
    }

    private ProviderModel lookupProviderModel(URL url) {
        ServiceRepository repo = ApplicationModel.getServiceRepository();
        final ProviderModel model = repo.lookupExportedService(url.getServiceKey());
        if (model != null) {
            ClassLoadUtil.switchContextLoader(model.getServiceInterfaceClass().getClassLoader());
        }
        return model;
    }

    protected RpcInvocation buildInvocation(Metadata metadata) {
        RpcInvocation inv = new RpcInvocation();
        inv.setMethodName(getMethodDescriptor().getMethodName());
        inv.setServiceName(getServiceDescriptor().getServiceName());
        inv.setTargetServiceUniqueName(getUrl().getServiceKey());
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

    protected Object[] deserializeRequest(byte[] data){
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            if (getProviderModel() != null) {
                ClassLoadUtil.switchContextLoader(getProviderModel().getServiceInterfaceClass().getClassLoader());
            }
            if (getMethodDescriptor().isNeedWrap()) {
                final TripleWrapper.TripleRequestWrapper wrapper = TripleUtil.unpack(data, TripleWrapper.TripleRequestWrapper.class);
                serialize(wrapper.getSerializeType());
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
            message = TripleUtil.wrapResp(getUrl(), getSerializeType(), value, getMethodDescriptor(), getMultipleSerialization());
        } else {
            message = (Message) value;
        }
        return TripleUtil.pack(message);
    }


    protected void executorInvoke(Runnable runnable){
        ExecutorService executor = null;
        if (getProviderModel() != null) {
            executor = (ExecutorService) getProviderModel().getServiceMetadata().getAttribute(
                    CommonConstants.THREADPOOL_KEY);
        }
        if (executor == null) {
            executor = EXECUTOR_REPOSITORY.getExecutor(getUrl());
        }
        if (executor == null) {
            executor = EXECUTOR_REPOSITORY.createExecutorIfAbsent(getUrl());
        }

        try {
            executor.execute(runnable);
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
