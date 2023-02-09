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
package org.apache.dubbo.remoting.api.connection.pool;

import org.apache.dubbo.common.Parameters;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Client;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.api.connection.ConnectionProvider;

import java.net.InetSocketAddress;

public class LazyClient implements Client {

    private final URL url;

    private Client client;

    private final ConnectionProvider connectionProvider;

    public LazyClient(URL url, ConnectionProvider connectionProvider) {
        this.url = url;
        this.connectionProvider = connectionProvider;
    }

    private void initIfAbsent() {
        if (client != null) {
            return;
        }
        client = connectionProvider.initConnection(url);
    }

    @Override
    public void reset(URL url) {
        initIfAbsent();
        client.reset(url);
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        initIfAbsent();
        return client.getRemoteAddress();
    }

    @Override
    public boolean isConnected() {
        initIfAbsent();
        return client.isConnected();
    }

    @Override
    public boolean hasAttribute(String key) {
        initIfAbsent();
        return client.hasAttribute(key);
    }

    @Override
    public Object getAttribute(String key) {
        initIfAbsent();
        return client.getAttribute(key);
    }

    @Override
    public void setAttribute(String key, Object value) {
        initIfAbsent();
        client.setAttribute(key,value);
    }

    @Override
    public void removeAttribute(String key) {
        initIfAbsent();
        client.removeAttribute(key);
    }

    @Override
    public void reconnect() throws RemotingException {
        initIfAbsent();
        client.reconnect();
    }

    @Override
    public void reset(Parameters parameters) {
        initIfAbsent();
        client.reset(parameters);
    }

    @Override
    public URL getUrl() {
        initIfAbsent();
        return client.getUrl();
    }

    @Override
    public ChannelHandler getChannelHandler() {
        initIfAbsent();
        return client.getChannelHandler();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        initIfAbsent();
        return client.getLocalAddress();
    }

    @Override
    public void send(Object message) throws RemotingException {
        initIfAbsent();
        client.send(message);
    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {
        initIfAbsent();
        client.send(message,sent);
    }

    @Override
    public void close() {
        initIfAbsent();
        client.close();
    }

    @Override
    public void close(int timeout) {
        initIfAbsent();
        client.close(timeout);
    }

    @Override
    public void startClose() {
        initIfAbsent();
        client.startClose();
    }

    @Override
    public boolean isClosed() {
        initIfAbsent();
        return client.isClosed();
    }

}
