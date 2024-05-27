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

import org.apache.dubbo.remoting.http12.message.HttpMessageDecoder;
import org.apache.dubbo.remoting.http12.message.HttpMessageEncoder;
import org.apache.dubbo.remoting.http12.message.MethodMetadata;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;

import java.util.Map;

public interface RpcInvocationBuildContext {

    Invoker<?> getInvoker();

    boolean isHasStub();

    String getMethodName();

    MethodMetadata getMethodMetadata();

    void setMethodMetadata(MethodMetadata methodMetadata);

    MethodDescriptor getMethodDescriptor();

    void setMethodDescriptor(MethodDescriptor methodDescriptor);

    ServiceDescriptor getServiceDescriptor();

    HttpMessageDecoder getHttpMessageDecoder();

    HttpMessageEncoder getHttpMessageEncoder();

    Map<String, Object> getAttributes();
}
