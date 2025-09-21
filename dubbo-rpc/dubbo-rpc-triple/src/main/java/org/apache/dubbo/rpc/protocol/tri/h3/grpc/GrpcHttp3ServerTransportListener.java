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
import org.apache.dubbo.remoting.http12.MessageTypeToken;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessage;
import org.apache.dubbo.remoting.http3.Http3TransportListener;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.h12.grpc.GrpcHttp2ServerTransportListener;
import org.apache.dubbo.rpc.protocol.tri.h12.http2.Http2ServerChannelObserver;

public final class GrpcHttp3ServerTransportListener<INPUT, OUTPUT>
        extends GrpcHttp2ServerTransportListener<INPUT, OUTPUT> implements Http3TransportListener<INPUT, OUTPUT> {

    public GrpcHttp3ServerTransportListener(
            H2StreamChannel<OUTPUT> h2StreamChannel,
            URL url,
            FrameworkModel frameworkModel,
            MessageTypeToken<INPUT, OUTPUT> typeToken) {
        super(h2StreamChannel, url, frameworkModel, typeToken);
    }

    @Override
    protected Http2ServerChannelObserver<INPUT, OUTPUT> newResponseObserver(H2StreamChannel<OUTPUT> h2StreamChannel) {
        return new GrpcHttp3UnaryServerChannelObserver<>(getFrameworkModel(), h2StreamChannel, getTypeToken());
    }

    @Override
    protected Http2ServerChannelObserver<INPUT, OUTPUT> newStreamResponseObserver(
            H2StreamChannel<OUTPUT> h2StreamChannel) {
        return new GrpcHttp3ServerChannelObserver<>(getFrameworkModel(), h2StreamChannel, getTypeToken());
    }

    @Override
    protected void doOnData(Http2InputMessage<INPUT> message) {
        if (message.isEndStream()) {
            onDataCompletion(message);
            return;
        }
        super.doOnData(message);
    }

    @Override
    protected void initializeAltSvc(URL url) {}
}
