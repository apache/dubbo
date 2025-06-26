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
package org.apache.dubbo.remoting.transport.netty4;

import org.apache.dubbo.rpc.model.FrameworkModel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

import io.netty.channel.Channel;

import static org.apache.dubbo.common.utils.NetUtils.toAddressString;

public final class AddressUtils {

    private static final List<ChannelAddressAccessor> ACCESSORS =
            FrameworkModel.defaultModel().getActivateExtensions(ChannelAddressAccessor.class);

    private static final String LOCAL_ADDRESS_KEY = "NETTY_LOCAL_ADDRESS_KEY";
    private static final String REMOTE_ADDRESS_KEY = "NETTY_REMOTE_ADDRESS_KEY";
    private static final String PROTOCOL_KEY = "NETTY_PROTOCOL_KEY";

    private AddressUtils() {}

    public static InetSocketAddress getRemoteAddress(Channel channel) {
        InetSocketAddress address;
        for (int i = 0, size = ACCESSORS.size(); i < size; i++) {
            address = ACCESSORS.get(i).getRemoteAddress(channel);
            if (address != null) {
                return address;
            }
        }
        return (InetSocketAddress) channel.remoteAddress();
    }

    public static InetSocketAddress getLocalAddress(Channel channel) {
        InetSocketAddress address;
        for (int i = 0, size = ACCESSORS.size(); i < size; i++) {
            address = ACCESSORS.get(i).getLocalAddress(channel);
            if (address != null) {
                return address;
            }
        }
        return (InetSocketAddress) channel.localAddress();
    }

    static void initAddressIfNecessary(NettyChannel nettyChannel) {
        Channel channel = nettyChannel.getNioChannel();
        SocketAddress address = channel.localAddress();
        if (address instanceof InetSocketAddress) {
            return;
        }

        for (int i = 0, size = ACCESSORS.size(); i < size; i++) {
            ChannelAddressAccessor accessor = ACCESSORS.get(i);
            InetSocketAddress localAddress = accessor.getLocalAddress(channel);
            if (localAddress != null) {
                nettyChannel.setAttribute(LOCAL_ADDRESS_KEY, localAddress);
                nettyChannel.setAttribute(REMOTE_ADDRESS_KEY, accessor.getRemoteAddress(channel));
                nettyChannel.setAttribute(PROTOCOL_KEY, accessor.getProtocol());
                break;
            }
        }
    }

    static InetSocketAddress getLocalAddress(NettyChannel channel) {
        InetSocketAddress address = (InetSocketAddress) channel.getAttribute(LOCAL_ADDRESS_KEY);
        return address == null ? (InetSocketAddress) (channel.getNioChannel().localAddress()) : address;
    }

    static InetSocketAddress getRemoteAddress(NettyChannel channel) {
        InetSocketAddress address = (InetSocketAddress) channel.getAttribute(REMOTE_ADDRESS_KEY);
        return address == null ? (InetSocketAddress) (channel.getNioChannel().remoteAddress()) : address;
    }

    static String getLocalAddressKey(NettyChannel channel) {
        InetSocketAddress address = getLocalAddress(channel);
        if (address == null) {
            return "UNKNOWN";
        }
        String protocol = (String) channel.getAttribute(PROTOCOL_KEY);
        return protocol == null ? toAddressString(address) : protocol + ' ' + toAddressString(address);
    }

    static String getRemoteAddressKey(NettyChannel channel) {
        InetSocketAddress address = getRemoteAddress(channel);
        if (address == null) {
            return "UNKNOWN";
        }
        String protocol = (String) channel.getAttribute(PROTOCOL_KEY);
        return protocol == null ? toAddressString(address) : protocol + ' ' + toAddressString(address);
    }
}
