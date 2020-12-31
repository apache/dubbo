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
package org.apache.dubbo.remoting.http.support;

import org.apache.dubbo.common.Parameters;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.http.HttpHandler;
import org.apache.dubbo.remoting.http.HttpServer;

import java.net.InetSocketAddress;
import java.util.Collection;

/**
 * AbstractHttpServer
 */
public abstract class AbstractHttpServer implements HttpServer {

    private final URL url;

    private final HttpHandler handler;

    private volatile boolean closed;

    public AbstractHttpServer(URL url, HttpHandler handler) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        this.url = url;
        this.handler = handler;
    }

    @Override
    public HttpHandler getHttpHandler() {
        return handler;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public void reset(URL url) {
    }

    @Override
    public boolean isBound() {
        return true;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return url.toInetSocketAddress();
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public void close(int timeout) {
        close();
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    /**
     * Following methods are extended from RemotingServer, useless for http servers
     */

    @Override
    public boolean canHandleIdle() {
        return false;
    }

    @Override
    public Collection<Channel> getChannels() {
        return null;
    }

    @Override
    public Channel getChannel(InetSocketAddress remoteAddress) {
        return null;
    }

    @Override
    public void reset(Parameters parameters) {

    }

    @Override
    public ChannelHandler getChannelHandler() {
        return null;
    }

    @Override
    public void send(Object message) throws RemotingException {

    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {

    }

    @Override
    public void startClose() {

    }
}
