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
package org.apache.dubbo.remoting.http3.netty4;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.transport.netty4.ChannelAddressAccessor;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;

@Activate(order = -100, onClass = "io.netty.incubator.codec.quic.QuicChannel")
public class Http3ChannelAddressAccessor implements ChannelAddressAccessor {

    @Override
    public String getProtocol() {
        return "UDP";
    }

    @Override
    public InetSocketAddress getRemoteAddress(Channel channel) {
        if (channel instanceof QuicStreamChannel) {
            return (InetSocketAddress) ((QuicStreamChannel) channel).parent().remoteSocketAddress();
        }
        if (channel instanceof QuicChannel) {
            return (InetSocketAddress) ((QuicChannel) channel).remoteSocketAddress();
        }
        return null;
    }

    @Override
    public InetSocketAddress getLocalAddress(Channel channel) {
        if (channel instanceof QuicStreamChannel) {
            return (InetSocketAddress) ((QuicStreamChannel) channel).parent().localSocketAddress();
        }
        if (channel instanceof QuicChannel) {
            return (InetSocketAddress) ((QuicChannel) channel).localSocketAddress();
        }
        return null;
    }
}
