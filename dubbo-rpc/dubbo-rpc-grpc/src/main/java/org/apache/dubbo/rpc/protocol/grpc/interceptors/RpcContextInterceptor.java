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
package org.apache.dubbo.rpc.protocol.grpc.interceptors;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.RpcContext;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;

import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;

/**
 * Hand over context information from Dubbo to gRPC.
 */
@Activate(group = {PROVIDER, CONSUMER})
public class RpcContextInterceptor implements ClientInterceptor, ServerInterceptor {

    private static final String DUBBO = "D-";

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        RpcContext rpcContext = RpcContext.getContext();
        Map<String, Object> attachments = rpcContext.getAttachments();
        if (attachments != null) {
            for (Map.Entry<String, Object> entry : attachments.entrySet()) {
                callOptions = callOptions.withOption(CallOptions.Key.create(DUBBO + entry.getKey()), entry.getValue());
            }
        }
        return next.newCall(method, callOptions);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        RpcContext rpcContext = RpcContext.getContext();
        Set<String> keys = headers.keys();
        if (keys != null) {
            keys.stream().filter(k -> k.startsWith(DUBBO)).forEach(k -> {
                rpcContext.setAttachment(k.substring(DUBBO.length()),
                        headers.get(Metadata.Key.of(k, Metadata.ASCII_STRING_MARSHALLER)));
            });
        }
        return next.startCall(call, headers);
    }

}
