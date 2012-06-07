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
package com.alibaba.dubbo.remoting.exchange.support;

import java.net.InetSocketAddress;
import java.util.Collection;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;
import com.alibaba.dubbo.remoting.exchange.ExchangeServer;

/**
 * ExchangeServerDelegate
 * 
 * @author william.liangf
 */
public class ExchangeServerDelegate implements ExchangeServer {
    
    private transient ExchangeServer server;
    
    public ExchangeServerDelegate() {
    }

    public ExchangeServerDelegate(ExchangeServer server){
        setServer(server);
    }

    public ExchangeServer getServer() {
        return server;
    }
    
    public void setServer(ExchangeServer server) {
        this.server = server;
    }

    public boolean isBound() {
        return server.isBound();
    }

    public void reset(URL url) {
        server.reset(url);
    }

    @Deprecated
    public void reset(com.alibaba.dubbo.common.Parameters parameters){
        reset(getUrl().addParameters(parameters.getParameters()));
    }
    
    public Collection<Channel> getChannels() {
        return server.getChannels();
    }

    public Channel getChannel(InetSocketAddress remoteAddress) {
        return server.getChannel(remoteAddress);
    }

    public URL getUrl() {
        return server.getUrl();
    }

    public ChannelHandler getChannelHandler() {
        return server.getChannelHandler();
    }

    public InetSocketAddress getLocalAddress() {
        return server.getLocalAddress();
    }

    public void send(Object message) throws RemotingException {
        server.send(message);
    }

    public void send(Object message, boolean sent) throws RemotingException {
        server.send(message, sent);
    }

    public void close() {
        server.close();
    }

    public boolean isClosed() {
        return server.isClosed();
    }

    public Collection<ExchangeChannel> getExchangeChannels() {
        return server.getExchangeChannels();
    }

    public ExchangeChannel getExchangeChannel(InetSocketAddress remoteAddress) {
        return server.getExchangeChannel(remoteAddress);
    }

    public void close(int timeout) {
        server.close(timeout);
    }

}