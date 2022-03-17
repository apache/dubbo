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
package org.apache.dubbo.qos.legacy.channel;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class MockChannel implements Channel {
    public static final String ERROR_WHEN_SEND = "error_when_send";
    InetSocketAddress localAddress;
    InetSocketAddress remoteAddress;
    private URL remoteUrl;
    private ChannelHandler handler;
    private boolean isClosed;
    private volatile boolean closing;
    private Map<String, Object> attributes = new HashMap<String, Object>(1);
    private List<Object> receivedObjects = new LinkedList<>();
    private CountDownLatch latch;

    public MockChannel() {

    }

    public MockChannel(URL remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    public MockChannel(URL remoteUrl, CountDownLatch latch) {
        this.remoteUrl = remoteUrl;
        this.latch = latch;
    }

    public MockChannel(ChannelHandler handler) {
        this.handler = handler;
    }

    @Override
    public URL getUrl() {
        return remoteUrl;
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return handler;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    public void send(Object message) throws RemotingException {
        if (remoteUrl.getParameter(ERROR_WHEN_SEND, Boolean.FALSE)) {
            throw new RemotingException(localAddress, remoteAddress, "mock error");
        } else {
            receivedObjects.add(message);
            if (latch != null) {
                latch.countDown();
            }
        }
    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {
        send(message);
    }

    @Override
    public void close() {
        close(0);
    }

    @Override
    public void close(int timeout) {
        isClosed = true;
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
        return remoteAddress;
    }

    @Override
    public boolean isConnected() {
        return isClosed;
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

    public List<Object> getReceivedObjects() {
        return receivedObjects;
    }
}
