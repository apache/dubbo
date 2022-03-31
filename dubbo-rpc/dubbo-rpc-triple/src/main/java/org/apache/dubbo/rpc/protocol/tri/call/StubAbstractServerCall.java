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
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.StubMethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.stream.ServerStream;
import org.apache.dubbo.rpc.stub.StubSuppliers;

import java.io.IOException;
import java.util.concurrent.Executor;

public class StubAbstractServerCall extends AbstractServerCall {

    public StubAbstractServerCall(Invoker<?> invoker,
        ServerStream serverStream,
        FrameworkModel frameworkModel,
        String acceptEncoding,
        String serviceName,
        String methodName,
        Executor executor) {
        super(invoker, serverStream, frameworkModel,
            getServiceDescriptor(invoker.getUrl(), serviceName),
            acceptEncoding, serviceName, methodName, executor);
        this.methodDescriptor = serviceDescriptor.getMethods(methodName)
            .get(0);
        this.packableMethod = (StubMethodDescriptor) methodDescriptor;
    }

    private static ServiceDescriptor getServiceDescriptor(URL url, String serviceName) {
        ServiceDescriptor serviceDescriptor;
        if (url.getServiceModel() != null) {
            serviceDescriptor = url
                .getServiceModel()
                .getServiceModel();
        } else {
            serviceDescriptor = StubSuppliers.getServiceDescriptor(serviceName);
        }
        return serviceDescriptor;
    }

    @Override
    protected Object parseSingleMessage(byte[] data) throws IOException, ClassNotFoundException {
        return packableMethod.parseRequest(data);
    }

}
