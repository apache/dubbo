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
package org.apache.dubbo.rpc.protocol.tri.h3.grpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http3.Http3ServerTransportListenerFactory;
import org.apache.dubbo.remoting.http3.Http3TransportListener;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.h12.grpc.GrpcUtils;

@Activate(order = -100, onClass = CommonConstants.PROTOBUF_MESSAGE_CLASS_NAME)
public class GrpcHttp3ServerTransportListenerFactory implements Http3ServerTransportListenerFactory {

    @Override
    public Http3TransportListener newInstance(H2StreamChannel streamChannel, URL url, FrameworkModel frameworkModel) {
        return new GrpcHttp3ServerTransportListener(streamChannel, url, frameworkModel);
    }

    @Override
    public boolean supportContentType(String contentType) {
        return GrpcUtils.isGrpcRequest(contentType);
    }
}
