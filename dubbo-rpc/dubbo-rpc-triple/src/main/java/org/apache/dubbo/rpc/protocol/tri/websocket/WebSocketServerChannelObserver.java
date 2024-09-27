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
package org.apache.dubbo.rpc.protocol.tri.websocket;

import org.apache.dubbo.remoting.http12.HttpOutputMessage;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.h12.http2.Http2StreamServerChannelObserver;

public class WebSocketServerChannelObserver extends Http2StreamServerChannelObserver {

    protected WebSocketServerChannelObserver(FrameworkModel frameworkModel, H2StreamChannel h2StreamChannel) {
        super(frameworkModel, h2StreamChannel);
    }

    @Override
    protected void doOnNext(Object data) throws Throwable {
        int statusCode = resolveStatusCode(data);
        HttpOutputMessage httpOutputMessage = buildMessage(statusCode, data);
        sendMessage(httpOutputMessage);
    }

    @Override
    protected void doOnError(Throwable throwable) {}
}
