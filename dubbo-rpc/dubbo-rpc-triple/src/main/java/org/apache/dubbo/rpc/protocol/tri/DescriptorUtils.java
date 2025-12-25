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
import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.http12.exception.UnimplementedException;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.TripleCustomerProtocolWrapper.TripleRequestWrapper;
import org.apache.dubbo.rpc.service.ServiceDescriptorInternalCache;
import org.apache.dubbo.rpc.stub.StubSuppliers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * The MetaUtils provides utility methods for working with service descriptors and method descriptors.
 */
public final class DescriptorUtils {

    private DescriptorUtils() {}

    public static ServiceDescriptor findServiceDescriptor(Invoker<?> invoker, String serviceName, boolean hasStub)
            throws UnimplementedException {
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

    public static ServiceDescriptor getStubServiceDescriptor(URL url, String serviceName) {
        ServiceDescriptor serviceDescriptor;
        if (url.getServiceModel() != null) {
            serviceDescriptor = url.getServiceModel().getServiceModel();
        } else {
            serviceDescriptor = StubSuppliers.getServiceDescriptor(serviceName);
        }
        return serviceDescriptor;
    }

    public static ServiceDescriptor getReflectionServiceDescriptor(URL url) {
        ProviderModel providerModel = (ProviderModel) url.getServiceModel();
        if (providerModel == null || providerModel.getServiceModel() == null) {
            return null;
        }
        return providerModel.getServiceModel();
    }

    public static MethodDescriptor findMethodDescriptor(
            ServiceDescriptor serviceDescriptor, String originalMethodName, boolean hasStub)
            throws UnimplementedException {
        MethodDescriptor result;
        if (hasStub) {
            result = serviceDescriptor.getMethods(originalMethodName).get(0);
        } else {
            result = findReflectionMethodDescriptor(serviceDescriptor, originalMethodName);
        }
        return result;
    }

    public static MethodDescriptor findReflectionMethodDescriptor(
            ServiceDescriptor serviceDescriptor, String methodName) {
        MethodDescriptor methodDescriptor = null;
        if (isGeneric(methodName)) {
            // There should be one and only one
            methodDescriptor = ServiceDescriptorInternalCache.genericService()
                    .getMethods(methodName)
                    .get(0);
        } else if (isEcho(methodName)) {
            // There should be one and only one
            return ServiceDescriptorInternalCache.echoService()
                    .getMethods(methodName)
                    .get(0);
        } else {
            List<MethodDescriptor> methodDescriptors = serviceDescriptor.getMethods(methodName);
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

    public static MethodDescriptor findTripleMethodDescriptor(
            ServiceDescriptor serviceDescriptor, String methodName, InputStream rawMessage) throws IOException {
        MethodDescriptor methodDescriptor = findReflectionMethodDescriptor(serviceDescriptor, methodName);
        if (methodDescriptor == null) {
            byte[] data = StreamUtils.readBytes(rawMessage);
            List<MethodDescriptor> methodDescriptors = serviceDescriptor.getMethods(methodName);
            TripleRequestWrapper request = TripleRequestWrapper.parseFrom(data);
            String[] paramTypes = request.getArgTypes().toArray(new String[0]);
            // wrapper mode the method can overload so maybe list
            for (MethodDescriptor descriptor : methodDescriptors) {
                // params type is array
                if (Arrays.equals(descriptor.getCompatibleParamSignatures(), paramTypes)) {
                    methodDescriptor = descriptor;
                    break;
                }
            }
            if (methodDescriptor == null) {
                throw new UnimplementedException("method:" + methodName);
            }
            rawMessage.reset();
        }
        return methodDescriptor;
    }

    private static boolean isGeneric(String methodName) {
        return CommonConstants.$INVOKE.equals(methodName) || CommonConstants.$INVOKE_ASYNC.equals(methodName);
    }

    private static boolean isEcho(String methodName) {
        return CommonConstants.$ECHO.equals(methodName);
    }
}
