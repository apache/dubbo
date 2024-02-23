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
package org.apache.dubbo.rpc.protocol.tri.h12.grpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.DescriptorUtils;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.h12.HttpRequestHandlerMapping;
import org.apache.dubbo.rpc.protocol.tri.route.RequestHandler;

@Activate(order = -3000)
public final class GrpcRequestHandlerMapping extends HttpRequestHandlerMapping {

    public static final GrpcCompositeCodecFactory CODEC_FACTORY = new GrpcCompositeCodecFactory();

    public GrpcRequestHandlerMapping(FrameworkModel frameworkModel) {
        super(frameworkModel);
    }

    @Override
    protected boolean supportContentType(String contentType) {
        return contentType != null && contentType.startsWith(MediaType.APPLICATION_GRPC.getName());
    }

    @Override
    protected void determineHttpMessageCodec(RequestHandler handler, URL url, HttpRequest request) {
        GrpcCompositeCodec grpcCompositeCodec =
                (GrpcCompositeCodec) CODEC_FACTORY.createCodec(url, getFrameworkModel(), request.contentType());
        MethodDescriptor methodDescriptor;
        if (request.hasHeader(TripleHeaderEnum.TRI_PARAM_DESC.getHeader())) {
            methodDescriptor = handler.getServiceDescriptor()
                    .getMethod(handler.getMethodName(), request.header(TripleHeaderEnum.TRI_PARAM_DESC.getHeader()));
        } else {
            methodDescriptor = DescriptorUtils.findMethodDescriptor(
                    handler.getServiceDescriptor(), handler.getMethodName(), handler.isHasStub());
        }
        if (methodDescriptor != null) {
            handler.setMethodDescriptor(methodDescriptor);
            grpcCompositeCodec.loadPackableMethod(methodDescriptor);
        }
        handler.setHttpMessageDecoder(grpcCompositeCodec);
        handler.setHttpMessageEncoder(grpcCompositeCodec);
    }

    @Override
    public String getType() {
        return TripleConstant.TRIPLE_HANDLER_TYPE_GRPC;
    }
}
