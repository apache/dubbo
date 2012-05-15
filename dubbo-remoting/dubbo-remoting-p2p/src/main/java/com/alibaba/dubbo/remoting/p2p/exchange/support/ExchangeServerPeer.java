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
package com.alibaba.dubbo.remoting.p2p.exchange.support;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.Client;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.remoting.exchange.ExchangeServer;
import com.alibaba.dubbo.remoting.exchange.support.ExchangeServerDelegate;
import com.alibaba.dubbo.remoting.p2p.exchange.ExchangeGroup;
import com.alibaba.dubbo.remoting.p2p.exchange.ExchangePeer;

/**
 * ServerPeer
 * 
 * @author william.liangf
 */
public class ExchangeServerPeer extends ExchangeServerDelegate implements ExchangePeer {
    
    private static final Logger logger = LoggerFactory.getLogger(ExchangeServerPeer.class);

    private final Map<URL, ExchangeClient> clients;

    private final ExchangeGroup group;
    
    public ExchangeServerPeer(ExchangeServer server, Map<URL, ExchangeClient> clients, ExchangeGroup group){
        super(server);
        this.clients = clients;
        this.group = group;
    }

    public void leave() throws RemotingException {
        group.leave(getUrl());
    }

    @Override
    public void close() {
        try {
            leave();
        } catch (RemotingException e) {
            logger.error(e.getMessage(), e);
        }
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Collection<Channel> getChannels() {
        return (Collection) getExchangeChannels();
    }

    @Override
    public Channel getChannel(InetSocketAddress remoteAddress) {
        return getExchangeChannel(remoteAddress);
    }

    @Override
    public Collection<ExchangeChannel> getExchangeChannels() {
        Collection<ExchangeChannel> channels = super.getExchangeChannels();
        if (clients.size() > 0) {
            channels = channels == null ? new ArrayList<ExchangeChannel>() : new ArrayList<ExchangeChannel>(channels);
            channels.addAll(clients.values());
        }
        return channels;
    }

    @Override
    public ExchangeChannel getExchangeChannel(InetSocketAddress remoteAddress) {
        String host = remoteAddress.getAddress() != null ? remoteAddress.getAddress().getHostAddress() : remoteAddress.getHostName();
        int port = remoteAddress.getPort();
        ExchangeChannel channel = super.getExchangeChannel(remoteAddress);
        if (channel == null) {
            for (Map.Entry<URL, ExchangeClient> entry : clients.entrySet()) {
                URL url = entry.getKey();
                if (url.getIp().equals(host) && url.getPort() == port) {
                    return entry.getValue();
                }
            }
        }
        return channel;
    }

    @Override
    public void send(Object message) throws RemotingException {
        send(message, getUrl().getParameter(Constants.SENT_KEY, false));
    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {
        Throwable last = null;
        try {
            super.send(message, sent);
        } catch (Throwable t) {
            last = t;
        }
        for (Client client : clients.values()) {
            try {
                client.send(message, sent);
            } catch (Throwable t) {
                last = t;
            }
        }
        if (last != null) {
            if (last instanceof RemotingException) {
                throw (RemotingException) last;
            } else if (last instanceof RuntimeException) {
                throw (RuntimeException) last;
            } else {
                throw new RuntimeException(last.getMessage(), last);
            }
        }
    }

}