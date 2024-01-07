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
package org.apache.dubbo.rpc.protocol.tri.route;

import org.apache.dubbo.remoting.http12.message.HttpMessageDecoder;
import org.apache.dubbo.remoting.http12.message.HttpMessageEncoder;
import org.apache.dubbo.remoting.http12.message.MethodMetadata;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.RpcInvocationBuildContext;

import java.util.HashMap;
import java.util.Map;

public final class RequestHandler implements RpcInvocationBuildContext {

    private final Invoker<?> invoker;
    private boolean hasStub;
    private String methodName;
    private MethodDescriptor methodDescriptor;
    private MethodMetadata methodMetadata;
    private ServiceDescriptor serviceDescriptor;
    private HttpMessageDecoder httpMessageDecoder;
    private HttpMessageEncoder httpMessageEncoder;
    private Map<String, Object> attributes = new HashMap<>();

    public RequestHandler(Invoker<?> invoker) {
        this.invoker = invoker;
    }

    @Override
    public Invoker<?> getInvoker() {
        return invoker;
    }

    @Override
    public boolean isHasStub() {
        return hasStub;
    }

    public void setHasStub(boolean hasStub) {
        this.hasStub = hasStub;
    }

    @Override
    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public MethodDescriptor getMethodDescriptor() {
        return methodDescriptor;
    }

    @Override
    public MethodMetadata getMethodMetadata() {
        return methodMetadata;
    }

    @Override
    public void setMethodMetadata(MethodMetadata methodMetadata) {
        this.methodMetadata = methodMetadata;
    }

    @Override
    public void setMethodDescriptor(MethodDescriptor methodDescriptor) {
        this.methodDescriptor = methodDescriptor;
    }

    @Override
    public ServiceDescriptor getServiceDescriptor() {
        return serviceDescriptor;
    }

    public void setServiceDescriptor(ServiceDescriptor serviceDescriptor) {
        this.serviceDescriptor = serviceDescriptor;
    }

    @Override
    public HttpMessageDecoder getHttpMessageDecoder() {
        return httpMessageDecoder;
    }

    public void setHttpMessageDecoder(HttpMessageDecoder httpMessageDecoder) {
        this.httpMessageDecoder = httpMessageDecoder;
    }

    @Override
    public HttpMessageEncoder getHttpMessageEncoder() {
        return httpMessageEncoder;
    }

    public void setHttpMessageEncoder(HttpMessageEncoder httpMessageEncoder) {
        this.httpMessageEncoder = httpMessageEncoder;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
}
