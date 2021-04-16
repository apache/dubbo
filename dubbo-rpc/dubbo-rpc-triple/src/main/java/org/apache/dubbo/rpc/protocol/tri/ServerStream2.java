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
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceRepository;

import com.google.protobuf.Message;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public abstract class ServerStream2 extends AbstractStream2 implements Stream {
        protected static final ExecutorRepository EXECUTOR_REPOSITORY =
        ExtensionLoader.getExtensionLoader(ExecutorRepository.class).getDefaultExtension();

    private final ProviderModel providerModel;
    private Invoker<?> invoker;

    protected ServerStream2(URL url) {
        super(url);
        this.providerModel = lookupProviderModel(url);
    }

    public static ServerStream2 unary(URL url) {
        return new UnaryServerStream2(url);
    }

    public static ServerStream2 stream(URL url) {
        return new StreamServerStream2(url);
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


    protected byte[] encodeResponse(Object value) {
        final com.google.protobuf.Message message;
        if (getMethodDescriptor().isNeedWrap()) {
            message = TripleUtil.wrapResp(getUrl(), getSerializeType(), value, getMethodDescriptor(), getMultipleSerialization());
        } else {
            message = (Message) value;
        }
        return TripleUtil.pack(message);
    }


    protected void executorInvoke(){
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
            executor.execute(this::invoke);
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


    public ServerStream2 service(ServiceDescriptor sd) {
        setServiceDescriptor(sd);
        return this;
    }

    public ServerStream2 method(MethodDescriptor md) {
        setMethodDescriptor(md);
        return this;
    }

    public ServerStream2 invoker(Invoker<?> invoker) {
        this.invoker = invoker;
        return this;
    }

}
