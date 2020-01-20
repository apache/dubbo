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
package org.apache.dubbo.rpc.protocol.thrift;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;

import java.net.InetSocketAddress;

public class MockedChannel implements Channel {

    private URL url;

    public MockedChannel(URL url) {
        this.url = url;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {

        return null;
    }

    @Override
    public boolean isConnected() {

        return false;
    }

    @Override
    public boolean hasAttribute(String key) {

        return false;
    }

    @Override
    public Object getAttribute(String key) {

        return null;
    }

    @Override
    public void setAttribute(String key, Object value) {

    }

    @Override
    public void removeAttribute(String key) {

    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public ChannelHandler getChannelHandler() {

        return null;
    }

    @Override
    public InetSocketAddress getLocalAddress() {

        return null;
    }

    @Override
    public void send(Object message) throws RemotingException {

    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {

    }

    @Override
    public void close() {

    }

    @Override
    public void close(int timeout) {

    }

    @Override
    public void startClose() {

    }

    @Override
    public boolean isClosed() {

        return false;
    }

}
