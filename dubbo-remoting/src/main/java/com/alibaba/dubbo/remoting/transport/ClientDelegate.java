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

import java.net.InetSocketAddress;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Client;
import com.alibaba.dubbo.remoting.RemotingException;

/**
 * ClientDelegate
 * 
 * @author william.liangf
 */
public class ClientDelegate implements Client {
    
    private transient Client client;

    public ClientDelegate() {
    }

    public ClientDelegate(Client client){
        setClient(client);
    }
    
    public Client getClient() {
        return client;
    }
    
    public void setClient(Client client) {
        if (client == null) {
            throw new IllegalArgumentException("client == null");
        }
        this.client = client;
    }

    public void reset(URL url) {
        client.reset(url);
    }
    
    @Deprecated
    public void reset(com.alibaba.dubbo.common.Parameters parameters){
        reset(getUrl().addParameters(parameters.getParameters()));
    }

    public URL getUrl() {
        return client.getUrl();
    }

    public InetSocketAddress getRemoteAddress() {
        return client.getRemoteAddress();
    }

    public void reconnect() throws RemotingException {
        client.reconnect();
    }

    public ChannelHandler getChannelHandler() {
        return client.getChannelHandler();
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    public InetSocketAddress getLocalAddress() {
        return client.getLocalAddress();
    }

    public boolean hasAttribute(String key) {
        return client.hasAttribute(key);
    }

    public void send(Object message) throws RemotingException {
        client.send(message);
    }

    public Object getAttribute(String key) {
        return client.getAttribute(key);
    }

    public void setAttribute(String key, Object value) {
        client.setAttribute(key, value);
    }

    public void send(Object message, boolean sent) throws RemotingException {
        client.send(message, sent);
    }

    public void removeAttribute(String key) {
        client.removeAttribute(key);
    }

    public void close() {
        client.close();
    }
    public void close(int timeout) {
        client.close(timeout);
    }

    public boolean isClosed() {
        return client.isClosed();
    }

}