/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.remoting.transport;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;

import java.net.InetSocketAddress;

/**
 * ChannelDelegate
 *
 * @author william.liangf
 */
public class ChannelDelegate implements Channel {

    private transient Channel channel;

    public ChannelDelegate() {
    }

    public ChannelDelegate(Channel channel) {
        setChannel(channel);
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("channel == null");
        }
        this.channel = channel;
    }

    public URL getUrl() {
        return channel.getUrl();
    }

    public InetSocketAddress getRemoteAddress() {
        return channel.getRemoteAddress();
    }

    public ChannelHandler getChannelHandler() {
        return channel.getChannelHandler();
    }

    public boolean isConnected() {
        return channel.isConnected();
    }

    public InetSocketAddress getLocalAddress() {
        return channel.getLocalAddress();
    }

    public boolean hasAttribute(String key) {
        return channel.hasAttribute(key);
    }

    public void send(Object message) throws RemotingException {
        channel.send(message);
    }

    public Object getAttribute(String key) {
        return channel.getAttribute(key);
    }

    public void setAttribute(String key, Object value) {
        channel.setAttribute(key, value);
    }

    public void send(Object message, boolean sent) throws RemotingException {
        channel.send(message, sent);
    }

    public void removeAttribute(String key) {
        channel.removeAttribute(key);
    }

    public void close() {
        channel.close();
    }

    public void close(int timeout) {
        channel.close(timeout);
    }

    @Override
    public void startClose() {
        channel.startClose();
    }

    public boolean isClosed() {
        return channel.isClosed();
    }

}