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
package org.apache.dubbo.registry.dubbo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.exchange.ExchangeHandler;
import org.apache.dubbo.remoting.exchange.ResponseFuture;

import java.net.InetSocketAddress;

public class MockChannel implements ExchangeChannel {

    public static boolean closed = false;
    public static boolean closing = true;
    final InetSocketAddress localAddress;
    final InetSocketAddress remoteAddress;

    public MockChannel(String localHostname, int localPort, String remoteHostName, int remotePort) {
        localAddress = new InetSocketAddress(localHostname, localPort);
        remoteAddress = new InetSocketAddress(remoteHostName, remotePort);
        closed = false;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public void send(Object message) throws RemotingException {
    }

    @Override
    public void close(int timeout) {
    }

    @Override
    public void startClose() {
        closing = true;
    }

    @Override
    public URL getUrl() {
        return null;
    }

    public ResponseFuture send(Object request, int timeout) throws RemotingException {
        return null;
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return null;
    }

    public ResponseFuture request(Object request) throws RemotingException {
        return null;
    }

    public ResponseFuture request(Object request, int timeout) throws RemotingException {
        return null;
    }

    public ExchangeHandler getExchangeHandler() {
        return null;
    }

    @Override
    public Object getAttribute(String key) {
        return null;
    }

    @Override
    public void setAttribute(String key, Object value) {

    }

    @Override
    public boolean hasAttribute(String key) {
        return false;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public void removeAttribute(String key) {

    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {

    }

}
