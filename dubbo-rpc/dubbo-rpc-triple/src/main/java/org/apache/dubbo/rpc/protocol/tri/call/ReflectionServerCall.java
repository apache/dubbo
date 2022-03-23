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

package org.apache.dubbo.rpc.protocol.tri.call;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.HeaderFilter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.MethodDescriptor.RpcType;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.ClassLoadUtil;
import org.apache.dubbo.rpc.protocol.tri.ReflectionPackableMethod;
import org.apache.dubbo.rpc.protocol.tri.stream.ServerStream;
import org.apache.dubbo.rpc.protocol.tri.stream.ServerStreamListener;
import org.apache.dubbo.rpc.service.ServiceDescriptorInternalCache;
import org.apache.dubbo.triple.TripleWrapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class ReflectionServerCall extends ServerCall {

    private final List<HeaderFilter> headerFilters;
    private MethodDescriptor methodDescriptor;
    private List<MethodDescriptor> methodDescriptors;
    private RpcInvocation invocation;

    public ReflectionServerCall(Invoker<?> invoker,
        ServerStream serverStream,
        FrameworkModel frameworkModel,
        String acceptEncoding,
        String serviceName,
        String methodName,
        List<HeaderFilter> headerFilters,
        Executor executor) {
        super(invoker, serverStream, frameworkModel,
            getServiceDescriptor(invoker.getUrl()),
            acceptEncoding, serviceName, methodName,
            executor);
        this.headerFilters = headerFilters;
    }

    private static ServiceDescriptor getServiceDescriptor(URL url) {
        ProviderModel providerModel = (ProviderModel) url.getServiceModel();
        if (providerModel == null || providerModel.getServiceModel() == null) {
            return null;
        }
        return providerModel.getServiceModel();
    }

    private boolean isEcho(String methodName) {
        return CommonConstants.$ECHO.equals(methodName);
    }

    private boolean isGeneric(String methodName) {
        return CommonConstants.$INVOKE.equals(methodName) || CommonConstants.$INVOKE_ASYNC.equals(
            methodName);
    }

    @Override
    public ServerStreamListener doStartCall(Map<String, Object> metadata) {

        if (isGeneric(methodName)) {
            // There should be one and only one
            methodDescriptor = ServiceDescriptorInternalCache.genericService()
                .getMethods(methodName).get(0);
        } else if (isEcho(methodName)) {
            // There should be one and only one
            methodDescriptor = ServiceDescriptorInternalCache.echoService().getMethods(methodName)
                .get(0);
        } else {
            methodDescriptors = serviceDescriptor.getMethods(methodName);
            // try lower-case method
            if (CollectionUtils.isEmpty(methodDescriptors)) {
                final String lowerMethod =
                    Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);
                methodDescriptors = serviceDescriptor.getMethods(lowerMethod);
            }
            if (CollectionUtils.isEmpty(methodDescriptors)) {
                responseErr(TriRpcStatus.UNIMPLEMENTED.withDescription(
                    "Method : " + methodName + " not found of service:" + serviceName));
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
                if (methodDescriptors.get(1).getRpcType() == RpcType.SERVER_STREAM) {
                    methodDescriptor = methodDescriptors.get(0);
                } else if (methodDescriptors.get(0).getRpcType() == RpcType.SERVER_STREAM) {
                    methodDescriptor = methodDescriptors.get(1);
                }
            }
        }
        if (methodDescriptor != null) {
            packableMethod = ReflectionPackableMethod.init(methodDescriptor, invoker.getUrl());
        }
        ServerStreamListenerImpl listener = new ServerStreamListenerImpl();
        listener.startCall(metadata);
        return listener;
    }

    class ServerStreamListenerImpl extends ServerCall.ServerStreamListenerBase {

        private Map<String, Object> metadata;

        void startCall(Map<String, Object> metadata) {
            this.metadata = metadata;
            trySetListener();
            if (listener == null) {
                // wrap request , need one message
                requestN(1);
            }
        }

        @Override
        public void complete() {
            if (listener != null) {
                listener.onComplete();
            }
        }

        @Override
        public void cancel(TriRpcStatus status) {
            listener.onCancel(status.description);
        }

        @Override
        protected void doOnMessage(byte[] message) throws IOException, ClassNotFoundException {
            trySetMethodDescriptor(message);
            trySetListener();
            if (closed) {
                return;
            }
            if (serviceDescriptor != null) {
                ClassLoadUtil.switchContextLoader(
                    serviceDescriptor.getServiceInterfaceClass().getClassLoader());
            }
            final Object obj = packableMethod.getRequestUnpack().unpack(message);
            listener.onMessage(obj);
        }

        private void trySetMethodDescriptor(byte[] data) throws IOException {
            if (methodDescriptor != null) {
                return;
            }
            final TripleWrapper.TripleRequestWrapper request;
            request = TripleWrapper.TripleRequestWrapper.parseFrom(data);

            final String[] paramTypes = request.getArgTypesList()
                .toArray(new String[request.getArgsCount()]);
            // wrapper mode the method can overload so maybe list
            for (MethodDescriptor descriptor : methodDescriptors) {
                // params type is array
                if (Arrays.equals(descriptor.getCompatibleParamSignatures(), paramTypes)) {
                    methodDescriptor = descriptor;
                    break;
                }
            }
            if (methodDescriptor == null) {
                close(TriRpcStatus.UNIMPLEMENTED.withDescription(
                    "Method :" + methodName + "[" + Arrays.toString(
                        paramTypes) + "] " + "not found of service:"
                        + serviceDescriptor.getInterfaceName()), null);
                return;
            }
            packableMethod = ReflectionPackableMethod.init(methodDescriptor, invoker.getUrl());
        }

        private void trySetListener() {
            if (listener != null) {
                return;
            }
            if (methodDescriptor == null) {
                return;
            }
            if (closed) {
                return;
            }
            invocation = buildInvocation(metadata, methodDescriptor);
            if (closed) {
                return;
            }
            headerFilters.forEach(f -> f.invoke(invoker, invocation));
            if (closed) {
                return;
            }
            listener = ReflectionServerCall.this.startInternalCall(invocation,
                methodDescriptor, invoker);
            if (listener == null) {
                closed = true;
            }
        }

    }
}
