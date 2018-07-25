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

package org.apache.dubbo.remoting.exchange.support.header;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockChannel implements Channel {

    private Map<String, Object> attributes = new HashMap<String, Object>();

    private volatile boolean closed = false;
    private volatile boolean closing = false;
    private List<Object> sentObjects = new ArrayList<Object>();

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
        return attributes.containsKey(key);
    }

    @Override
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    @Override
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    @Override
    public URL getUrl() {
        return null;
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
        sentObjects.add(message);
    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {
        sentObjects.add(message);
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public void close(int timeout) {
        closed = true;
    }

    @Override
    public void startClose() {
        closing = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    public List<Object> getSentObjects() {
        return Collections.unmodifiableList(sentObjects);
    }
}
