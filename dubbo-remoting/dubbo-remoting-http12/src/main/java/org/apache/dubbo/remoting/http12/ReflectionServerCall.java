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
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.rpc.HeaderFilter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.service.ServiceDescriptorInternalCache;

import java.util.List;

public class ReflectionServerCall extends AbstractServerCall {

    private final List<HeaderFilter> headerFilters;

    private List<MethodDescriptor> methodDescriptors;

    public ReflectionServerCall(String serviceName,
                                String methodName,
                                HttpMessageCodec messageCodec,
                                Invoker<?> invoker,
                                FrameworkModel frameworkModel,
                                HttpChannelObserver httpChannelObserver,
                                List<HeaderFilter> headerFilters) {
        super(serviceName, methodName, messageCodec, invoker, frameworkModel, httpChannelObserver);
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
    public void startCall() {
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
//                responseErr(TriRpcStatus.UNIMPLEMENTED.withDescription(
//                    "Method : " + methodName + " not found of service:" + serviceName));
                return;
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
        trySetListener();
    }

    private void trySetListener() {
        if (serverCallListener != null) {
            return;
        }
        if (methodDescriptor == null) {
            return;
        }
        RpcInvocation invocation = buildInvocation(methodDescriptor);
//        headerFilters.forEach(f -> f.invoke(invoker, invocation));
        serverCallListener = ReflectionServerCall.this.startInternalCall(invocation,
            methodDescriptor, invoker);
    }
}
