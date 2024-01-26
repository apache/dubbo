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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta;

import org.apache.dubbo.remoting.http12.message.MethodMetadata;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;

public final class HandlerMeta {

    private final Invoker<?> invoker;
    private final MethodMeta method;
    private final MethodMetadata methodMetadata;
    private final MethodDescriptor methodDescriptor;
    private final ServiceDescriptor serviceDescriptor;

    public HandlerMeta(
            Invoker<?> invoker,
            MethodMeta method,
            MethodMetadata methodMetadata,
            MethodDescriptor methodDescriptor,
            ServiceDescriptor serviceDescriptor) {
        this.invoker = invoker;
        this.method = method;
        this.methodMetadata = methodMetadata;
        this.methodDescriptor = methodDescriptor;
        this.serviceDescriptor = serviceDescriptor;
    }

    public Invoker<?> getInvoker() {
        return invoker;
    }

    public MethodMeta getMethod() {
        return method;
    }

    public ServiceMeta getService() {
        return method.getServiceMeta();
    }

    public ParameterMeta[] getParameters() {
        return method.getParameters();
    }

    public MethodMetadata getMethodMetadata() {
        return methodMetadata;
    }

    public MethodDescriptor getMethodDescriptor() {
        return methodDescriptor;
    }

    public ServiceDescriptor getServiceDescriptor() {
        return serviceDescriptor;
    }
}
