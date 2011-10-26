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
package com.alibaba.dubbo.registry.dubbo;

import java.net.InetSocketAddress;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;
import com.alibaba.dubbo.remoting.exchange.ExchangeHandler;
import com.alibaba.dubbo.remoting.exchange.ResponseFuture;

public class MockChannel implements ExchangeChannel {

    final InetSocketAddress localAddress;

    final InetSocketAddress remoteAddress;

    public static boolean   closed = false;

    public MockChannel(String localHostname, int localPort, String remoteHostName, int remotePort){
        localAddress = new InetSocketAddress(localHostname, localPort);
        remoteAddress = new InetSocketAddress(remoteHostName, remotePort);
        closed = false;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public boolean isConnected() {
        return true;
    }

    public void close() {
        closed = true;
    }

    public void send(Object message) throws RemotingException {
    }

    public void close(int timeout) {
    }

    public URL getUrl() {
        return null;
    }

    public ResponseFuture send(Object request, int timeout) throws RemotingException {
        return null;
    }

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

    public Object getAttribute(String key) {
        return null;
    }

    public void setAttribute(String key, Object value) {

    }

    public boolean hasAttribute(String key) {
        return false;
    }

    public boolean isClosed() {
        return false;
    }

    public void removeAttribute(String key) {

    }

    public void send(Object message, boolean sent) throws RemotingException {

    }

}