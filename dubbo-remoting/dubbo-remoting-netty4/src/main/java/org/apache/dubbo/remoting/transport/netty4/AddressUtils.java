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

import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

import io.netty.channel.Channel;

public final class AddressUtils {

    private static final List<ChannelAddressAccessor> ACCESSORS =
            FrameworkModel.defaultModel().getActivateExtensions(ChannelAddressAccessor.class);

    private AddressUtils() {}

    public static InetSocketAddress getRemoteAddress(Channel channel) {
        InetSocketAddress address;
        for (int i = 0, len = ACCESSORS.size(); i < len; i++) {
            address = ACCESSORS.get(i).getRemoteAddress(channel);
            if (address != null) {
                return address;
            }
        }
        return (InetSocketAddress) channel.remoteAddress();
    }

    public static InetSocketAddress getLocalAddress(Channel channel) {
        InetSocketAddress address;
        for (int i = 0, len = ACCESSORS.size(); i < len; i++) {
            address = ACCESSORS.get(i).getLocalAddress(channel);
            if (address != null) {
                return address;
            }
        }
        return (InetSocketAddress) channel.localAddress();
    }

    public static String getRemoteAddressKey(Channel channel) {
        InetSocketAddress address;
        for (int i = 0, len = ACCESSORS.size(); i < len; i++) {
            ChannelAddressAccessor accessor = ACCESSORS.get(i);
            address = accessor.getRemoteAddress(channel);
            if (address != null) {
                return accessor.getProtocol() + ' ' + NetUtils.toAddressString(address);
            }
        }
        InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
        if (remoteAddress == null) {
            return "UNKNOWN";
        }
        return NetUtils.toAddressString(remoteAddress);
    }

    public static String getLocalAddressKey(Channel channel) {
        InetSocketAddress address;
        for (int i = 0, len = ACCESSORS.size(); i < len; i++) {
            ChannelAddressAccessor accessor = ACCESSORS.get(i);
            address = accessor.getLocalAddress(channel);
            if (address != null) {
                return accessor.getProtocol() + ' ' + NetUtils.toAddressString(address);
            }
        }
        SocketAddress localAddress = channel.localAddress();
        if (localAddress == null) {
            return "UNKNOWN";
        }
        return NetUtils.toAddressString((InetSocketAddress) localAddress);
    }
}
