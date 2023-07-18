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
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;

import java.util.Objects;

public abstract class AbstractServerCall implements ServerCall {

    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(AbstractServerCall.class);

    protected final Invoker<?> invoker;

    protected final FrameworkModel frameworkModel;

    protected final String methodName;

    protected final String serviceName;

    protected final HttpMessageCodec messageCodec;

    protected ServerCall.Listener serverCallListener;

    protected MethodDescriptor methodDescriptor;

    public final ServiceDescriptor serviceDescriptor;

    CancellationContext cancellationContext;

    protected AbstractServerCall(String serviceName,
                                 String methodName,
                                 HttpMessageCodec messageCodec,
                                 Invoker<?> invoker,
                                 FrameworkModel frameworkModel
    ) {
        this.serviceDescriptor = Objects.requireNonNull(getServiceDescriptor(invoker.getUrl()),
            "No service descriptor found for " + invoker.getUrl());
        this.serviceName = serviceName;
        this.methodName = methodName;
        this.messageCodec = messageCodec;
        this.invoker = invoker;
        this.frameworkModel = frameworkModel;
        this.startCall();
    }

    private static ServiceDescriptor getServiceDescriptor(URL url) {
        ProviderModel providerModel = (ProviderModel) url.getServiceModel();
        if (providerModel == null || providerModel.getServiceModel() == null) {
            return null;
        }
        return providerModel.getServiceModel();
    }

    protected void startCall() {
        RpcInvocation invocation = buildInvocation(methodDescriptor);
        serverCallListener = startInternalCall(invocation, methodDescriptor, invoker);
    }

    protected RpcInvocation buildInvocation(MethodDescriptor methodDescriptor) {
        final URL url = invoker.getUrl();
        RpcInvocation inv = new RpcInvocation(url.getServiceModel(),
            methodDescriptor.getMethodName(),
            serviceDescriptor.getInterfaceName(), url.getProtocolServiceKey(),
            methodDescriptor.getParameterClasses(),
            new Object[0]);
        inv.setTargetServiceUniqueName(url.getServiceKey());
        inv.setReturnTypes(methodDescriptor.getReturnTypes());
//        inv.setObjectAttachments(StreamUtils.toAttachments(requestMetadata));
//        inv.put(REMOTE_ADDRESS_KEY, stream.remoteAddress());
        // handle timeout
//        String timeout = (String) requestMetadata.get(TripleHeaderEnum.TIMEOUT.getHeader());
//        try {
//            if (Objects.nonNull(timeout)) {
//                this.timeout = parseTimeoutToMills(timeout);
//            }
//        } catch (Throwable t) {
//            LOGGER.warn(PROTOCOL_FAILED_PARSE, "", "", String.format("Failed to parse request timeout set from:%s, service=%s "
//                + "method=%s", timeout, serviceDescriptor.getInterfaceName(), methodName));
//        }
//        if (null != requestMetadata.get(TripleHeaderEnum.CONSUMER_APP_NAME_KEY.getHeader())) {
//            inv.put(TripleHeaderEnum.CONSUMER_APP_NAME_KEY,
//                requestMetadata.get(TripleHeaderEnum.CONSUMER_APP_NAME_KEY.getHeader()));
//        }
        return inv;
    }


    protected ServerCall.Listener startInternalCall(
        RpcInvocation invocation,
        MethodDescriptor methodDescriptor,
        Invoker<?> invoker) {
        this.cancellationContext = RpcContext.getCancellationContext();
//        try {
//            ServerCall.Listener listener;
//            switch (methodDescriptor.getRpcType()) {
//                case UNARY:
//                    listener = new UnaryServerCallListener(invocation, invoker, responseObserver);
//                    break;
//                case SERVER_STREAM:
//                    listener = new ServerStreamServerCallListener(invocation, invoker,
//                        responseObserver);
//                    break;
//                case BI_STREAM:
//                case CLIENT_STREAM:
//                default:
//                    throw new IllegalStateException("Can not reach here");
//            }
//            return listener;
//        } catch (Exception e) {
//            LOGGER.error(PROTOCOL_FAILED_CREATE_STREAM_TRIPLE, "", "", "Create triple stream failed", e);
//        }
        return null;
    }
}
