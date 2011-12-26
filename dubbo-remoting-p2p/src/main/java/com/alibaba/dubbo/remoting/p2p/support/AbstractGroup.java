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
package com.alibaba.dubbo.remoting.p2p.support;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Client;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.Server;
import com.alibaba.dubbo.remoting.Transporters;
import com.alibaba.dubbo.remoting.p2p.Group;
import com.alibaba.dubbo.remoting.p2p.Peer;
import com.alibaba.dubbo.remoting.transport.ChannelHandlerDispatcher;

/**
 * AbstractGroup
 * 
 * @author william.liangf
 */
public abstract class AbstractGroup implements Group {

    // 日志输出
    protected static final Logger logger = LoggerFactory.getLogger(AbstractGroup.class);
    
    protected final URL url;
    
    protected final Map<URL, Server> servers = new ConcurrentHashMap<URL, Server>();

    protected final Map<URL, Client> clients = new ConcurrentHashMap<URL, Client>();
    
    protected final ChannelHandlerDispatcher dispatcher = new ChannelHandlerDispatcher();

    public AbstractGroup(URL url){
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        this.url = url;
    }
    
    public URL getUrl() {
        return url;
    }

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
    
    public Peer join(URL url, ChannelHandler handler) throws RemotingException {
        Server server = servers.get(url);
        if (server == null) { // TODO 有并发间隙
            server = Transporters.bind(url, handler);
            servers.put(url, server);
            dispatcher.addChannelHandler(handler);
        }
        return new ServerPeer(server, clients, this);
    }

    public void leave(URL url) throws RemotingException {
        Server server = servers.remove(url);
        if (server != null) {
            server.close();
        }
    }

    protected Client connect(URL url) throws RemotingException {
        if (servers.containsKey(url)) {
            return null;
        }
        Client client = clients.get(url);
        if (client == null) { // TODO 有并发间隙
            client = Transporters.connect(url, dispatcher);
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