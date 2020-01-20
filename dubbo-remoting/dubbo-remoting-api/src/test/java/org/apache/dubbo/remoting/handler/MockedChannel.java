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
package org.apache.dubbo.remoting.handler;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class MockedChannel implements Channel {
    private boolean isClosed;
    private volatile boolean closing = false;
    private URL url;
    private ChannelHandler handler;
    private Map<String, Object> map = new HashMap<String, Object>();

    public MockedChannel() {
        super();
    }


    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public ChannelHandler getChannelHandler() {

        return this.handler;
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
        this.send(message);
    }

    @Override
    public void close() {
        isClosed = true;
    }

    @Override
    public void close(int timeout) {
        this.close();
    }

    @Override
    public void startClose() {
        closing = true;
    }

    @Override
    public boolean isClosed() {
        return isClosed;
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
        return map.containsKey(key);
    }

    @Override
    public Object getAttribute(String key) {
        return map.get(key);
    }

    @Override
    public void setAttribute(String key, Object value) {
        map.put(key, value);
    }

    @Override
    public void removeAttribute(String key) {
        map.remove(key);
    }
}
