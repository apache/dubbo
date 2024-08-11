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
package org.apache.dubbo.rpc.protocol.tri.h3;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.remoting.transport.netty4.NettyHttp3ConnectionClient;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.call.TripleClientCall;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStream;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStreamFactory;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleWriteQueue;

import java.util.concurrent.Executor;

import io.netty.channel.Channel;

@Activate(order = -100, onClass = "io.netty.incubator.codec.quic.QuicChannel")
public class Http3ClientStreamFactory implements ClientStreamFactory {

    @Override
    public ClientStream createClientStream(
            AbstractConnectionClient client,
            FrameworkModel frameworkModel,
            Executor executor,
            TripleClientCall clientCall,
            TripleWriteQueue writeQueue) {
        if (client instanceof NettyHttp3ConnectionClient) {
            return new Http3TripleClientStream(
                    frameworkModel, executor, (Channel) client.getChannel(true), clientCall, writeQueue);
        }
        return null;
    }
}
