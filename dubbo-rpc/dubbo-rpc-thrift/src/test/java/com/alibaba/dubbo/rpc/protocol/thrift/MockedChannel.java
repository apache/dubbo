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
package com.alibaba.dubbo.rpc.protocol.thrift;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;

import java.net.InetSocketAddress;

public class MockedChannel implements Channel {

    private URL url;

    public MockedChannel(URL url) {
        this.url = url;
    }

    public InetSocketAddress getRemoteAddress() {

        return null;
    }

    public boolean isConnected() {

        return false;
    }

    public boolean hasAttribute(String key) {

        return false;
    }

    public Object getAttribute(String key) {

        return null;
    }

    public void setAttribute(String key, Object value) {

    }

    public void removeAttribute(String key) {

    }

    public URL getUrl() {
        return url;
    }

    public ChannelHandler getChannelHandler() {

        return null;
    }

    public InetSocketAddress getLocalAddress() {

        return null;
    }

    public void send(Object message) throws RemotingException {

    }

    public void send(Object message, boolean sent) throws RemotingException {

    }

    public void close() {

    }

    public void close(int timeout) {

    }

    @Override
    public void startClose() {

    }

    public boolean isClosed() {

        return false;
    }

}
