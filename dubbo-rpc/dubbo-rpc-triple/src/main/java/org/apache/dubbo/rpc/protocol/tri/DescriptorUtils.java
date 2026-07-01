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
import org.apache.dubbo.common.stream.StreamObserver;
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
import java.lang.reflect.Type;
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
        MethodDescriptor methodDescriptor = findWellKnownMethodDescriptor(methodName);
        if (methodDescriptor != null) {
            return methodDescriptor;
        }

        List<MethodDescriptor> methodDescriptors = serviceDescriptor.getMethods(methodName);
        methodDescriptor = findSingleOrGeneratedUnaryMethodDescriptor(methodDescriptors);
        if (methodDescriptor == null && CollectionUtils.isNotEmpty(methodDescriptors)) {
            throw new UnimplementedException("method:" + methodName);
        }
        return methodDescriptor;
    }

    public static MethodDescriptor findTripleMethodDescriptor(
            ServiceDescriptor serviceDescriptor, String methodName, InputStream rawMessage) throws IOException {
        MethodDescriptor methodDescriptor = findWellKnownMethodDescriptor(methodName);
        if (methodDescriptor != null) {
            return methodDescriptor;
        }

        List<MethodDescriptor> methodDescriptors = serviceDescriptor.getMethods(methodName);
        if (CollectionUtils.isEmpty(methodDescriptors)) {
            throw new UnimplementedException("method:" + methodName);
        }
        if (methodDescriptors.size() == 1) {
            return methodDescriptors.get(0);
        }

        TripleRequestWrapper request = parseRequestWrapper(rawMessage);
        List<String> argTypes = request == null ? null : request.getArgTypes();
        if (argTypes != null) {
            MethodDescriptor matchedDescriptor =
                    findMethodDescriptorByParamTypes(methodDescriptors, argTypes.toArray(new String[0]));
            if (matchedDescriptor != null) {
                return matchedDescriptor;
            }
            if (CollectionUtils.isNotEmpty(argTypes)) {
                throw new UnimplementedException("method:" + methodName);
            }
        }

        MethodDescriptor generatedUnaryDescriptor = findGeneratedUnaryMethodDescriptor(methodDescriptors);
        if (generatedUnaryDescriptor != null) {
            return generatedUnaryDescriptor;
        }
        throw new UnimplementedException("method:" + methodName);
    }

    private static MethodDescriptor findSingleOrGeneratedUnaryMethodDescriptor(
            List<MethodDescriptor> methodDescriptors) {
        if (CollectionUtils.isEmpty(methodDescriptors)) {
            return null;
        }
        // In most cases there is only one method
        if (methodDescriptors.size() == 1) {
            return methodDescriptors.get(0);
        }
        return findGeneratedUnaryMethodDescriptor(methodDescriptors);
    }

    private static TripleRequestWrapper parseRequestWrapper(InputStream rawMessage) throws IOException {
        rawMessage.mark(Integer.MAX_VALUE);
        try {
            return TripleRequestWrapper.parseFrom(rawMessage);
        } finally {
            rawMessage.reset();
        }
    }

    private static MethodDescriptor findWellKnownMethodDescriptor(String methodName) {
        if (isGeneric(methodName)) {
            return ServiceDescriptorInternalCache.genericService()
                    .getMethods(methodName)
                    .get(0);
        }
        if (isEcho(methodName)) {
            return ServiceDescriptorInternalCache.echoService()
                    .getMethods(methodName)
                    .get(0);
        }
        return null;
    }

    private static MethodDescriptor findMethodDescriptorByParamTypes(
            List<MethodDescriptor> methodDescriptors, String[] paramTypes) {
        for (MethodDescriptor descriptor : methodDescriptors) {
            // wrapper mode the method can overload so maybe list
            if (Arrays.equals(descriptor.getCompatibleParamSignatures(), paramTypes)) {
                return descriptor;
            }
        }
        return null;
    }

    private static MethodDescriptor findGeneratedUnaryMethodDescriptor(List<MethodDescriptor> methodDescriptors) {
        // Generated unary methods may expose two Java methods for the same RPC:
        // Response foo(Request)
        // void foo(Request, StreamObserver<Response>)
        if (methodDescriptors.size() != 2) {
            return null;
        }

        MethodDescriptor unaryMethodDescriptor = null;
        MethodDescriptor serverStreamMethodDescriptor = null;
        for (MethodDescriptor descriptor : methodDescriptors) {
            if (descriptor.getRpcType() == MethodDescriptor.RpcType.UNARY) {
                unaryMethodDescriptor = descriptor;
            } else if (descriptor.getRpcType() == MethodDescriptor.RpcType.SERVER_STREAM) {
                serverStreamMethodDescriptor = descriptor;
            }
        }
        if (unaryMethodDescriptor == null || serverStreamMethodDescriptor == null) {
            return null;
        }
        if (!Arrays.equals(
                unaryMethodDescriptor.getParameterClasses(),
                getServerStreamRequestTypes(serverStreamMethodDescriptor))) {
            return null;
        }
        if (!isSameResponseType(unaryMethodDescriptor, serverStreamMethodDescriptor)) {
            return null;
        }
        return unaryMethodDescriptor;
    }

    private static Class<?>[] getServerStreamRequestTypes(MethodDescriptor serverStreamMethodDescriptor) {
        Class<?>[] parameterClasses = serverStreamMethodDescriptor.getParameterClasses();
        if (parameterClasses.length == 0
                || !StreamObserver.class.isAssignableFrom(parameterClasses[parameterClasses.length - 1])) {
            return null;
        }
        return Arrays.copyOf(parameterClasses, parameterClasses.length - 1);
    }

    private static boolean isSameResponseType(
            MethodDescriptor unaryMethodDescriptor, MethodDescriptor serverStreamMethodDescriptor) {
        Type[] returnTypes = unaryMethodDescriptor.getReturnTypes();
        if (returnTypes.length == 0 || !(returnTypes[0] instanceof Class)) {
            return false;
        }
        Class<?> actualResponseType = serverStreamMethodDescriptor.getActualResponseType();
        if (actualResponseType == null) {
            return false;
        }
        return returnTypes[0].equals(actualResponseType);
    }

    private static boolean isGeneric(String methodName) {
        return CommonConstants.$INVOKE.equals(methodName) || CommonConstants.$INVOKE_ASYNC.equals(methodName);
    }

    private static boolean isEcho(String methodName) {
        return CommonConstants.$ECHO.equals(methodName);
    }
}
