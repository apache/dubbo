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
package org.apache.dubbo.rpc.proxy;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Constants;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * InvokerHandler
 */
public class InvokerInvocationHandler implements InvocationHandler {
    private static final Logger logger = LoggerFactory.getLogger(InvokerInvocationHandler.class);
    private final Invoker<?> invoker;
    private ConsumerModel consumerModel;
    private ServiceDescriptor serviceDescriptor;
    private String serviceKey;
    private String serviceName;

    public InvokerInvocationHandler(Invoker<?> handler) {
        this.invoker = handler;
        this.serviceKey = invoker.getUrl().getServiceKey();
        this.serviceName = invoker.getUrl().getServiceInterface();
        if (serviceKey != null) {
            this.consumerModel = ApplicationModel.getConsumerModel(serviceKey);
            this.serviceDescriptor = ApplicationModel.getServiceRepository()
                    .lookupService(serviceName);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(invoker, args);
        }
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0) {
            if ("toString".equals(methodName)) {
                return invoker.toString();
            } else if ("$destroy".equals(methodName)) {
                invoker.destroy();
                return null;
            } else if ("hashCode".equals(methodName)) {
                return invoker.hashCode();
            }
        } else if (parameterTypes.length == 1 && "equals".equals(methodName)) {
            return invoker.equals(args[0]);
        }

        Map<Object, Object> attributes = new HashMap<>(8);
        if (consumerModel != null) {
            attributes.put(Constants.CONSUMER_MODEL, consumerModel);
            attributes.put(Constants.METHOD_MODEL, consumerModel.getMethodModel(method));
        }
        if (serviceDescriptor != null) {
            attributes.put(Constants.SERVICE_DESCRIPTOR, serviceDescriptor);
            attributes.put(Constants.METHOD_DESCRIPTOR, serviceDescriptor.getMethod(method));
        }

        RpcInvocation rpcInvocation = new RpcInvocation(method, serviceName, args, null, attributes);
        rpcInvocation.setTargetServiceUniqueName(serviceKey);

        return invoker.invoke(rpcInvocation).recreate();
    }
}
