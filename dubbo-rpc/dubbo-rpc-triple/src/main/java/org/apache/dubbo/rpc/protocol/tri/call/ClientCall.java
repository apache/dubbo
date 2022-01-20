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

import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.StreamMethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.pack.Pack;
import org.apache.dubbo.rpc.protocol.tri.pack.Unpack;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStream;

import java.util.Map;

public class ClientCall {

    private ClientStream stream;
    private Pack requestPack;
    private Unpack responseUnpack;
    private Compressor compressor;
    private final Connection connection;

    public ClientCall(Connection connection) {
        this.connection = connection;
    }


    public void sendMessage(Object message) {

    }

    public void start(Listener responseListener, RpcInvocation invocation) {

    }

    public void cancel(GrpcStatus status, Map<String, Object> attachments) {

    }

    interface Listener {

        void onAttachment(Map<String, Object> attachments);

        void onMessage(Object message);

        void onClose(GrpcStatus status, Map<String, Object> trailers);
    }

    public static Listener runtimeCallListener(MethodDescriptor methodDescriptor,Object[] arguments){
        if(methodDescriptor instanceof StreamMethodDescriptor){
            final int index = ((StreamMethodDescriptor) methodDescriptor).responseObserverIndex();
            final Object responseObserver = arguments[index];

        }else{

        }
    }
}
