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
package org.apache.dubbo.remoting.p2p.exchange.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Client;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.RemotingServer;
import org.apache.dubbo.remoting.exchange.ExchangeClient;
import org.apache.dubbo.remoting.exchange.ExchangeHandler;
import org.apache.dubbo.remoting.exchange.ExchangeServer;
import org.apache.dubbo.remoting.exchange.Exchangers;
import org.apache.dubbo.remoting.exchange.support.ExchangeHandlerDispatcher;
import org.apache.dubbo.remoting.p2p.Peer;
import org.apache.dubbo.remoting.p2p.exchange.ExchangeGroup;
import org.apache.dubbo.remoting.p2p.exchange.ExchangePeer;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbstractGroup
 */
public abstract class AbstractExchangeGroup implements ExchangeGroup {

    // log  output
    protected static final Logger logger = LoggerFactory.getLogger(AbstractExchangeGroup.class);

    protected final URL url;

    protected final Map<URL, ExchangeServer> servers = new ConcurrentHashMap<URL, ExchangeServer>();

    protected final Map<URL, ExchangeClient> clients = new ConcurrentHashMap<URL, ExchangeClient>();

    protected final ExchangeHandlerDispatcher dispatcher = new ExchangeHandlerDispatcher();

    public AbstractExchangeGroup(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        this.url = url;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public void close() {
        for (URL url : new ArrayList<URL>(servers.keySet())) {
            try {
                leave(url);
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
            }
        }
        for (URL url : new ArrayList<URL>(clients.keySet())) {
            try {
                disconnect(url);
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
            }
        }
    }

    @Override
    public Peer join(URL url, ChannelHandler handler) throws RemotingException {
        return join(url, (ExchangeHandler) handler);
    }

    @Override
    public ExchangePeer join(URL url, ExchangeHandler handler) throws RemotingException {
        ExchangeServer server = servers.get(url);
        if (server == null) { // TODO exist concurrent gap
            server = Exchangers.bind(url, handler);
            servers.put(url, server);
            dispatcher.addChannelHandler(handler);
        }
        return new ExchangeServerPeer(server, clients, this);
    }

    @Override
    public void leave(URL url) throws RemotingException {
        RemotingServer server = servers.remove(url);
        if (server != null) {
            server.close();
        }
    }

    protected Client connect(URL url) throws RemotingException {
        if (servers.containsKey(url)) {
            return null;
        }
        ExchangeClient client = clients.get(url);
        if (client == null) { // TODO exist concurrent gap
            client = Exchangers.connect(url, dispatcher);
            clients.put(url, client);
        }
        return client;
    }

    protected void disconnect(URL url) throws RemotingException {
        Client client = clients.remove(url);
        if (client != null) {
            client.close();
        }
    }

}
