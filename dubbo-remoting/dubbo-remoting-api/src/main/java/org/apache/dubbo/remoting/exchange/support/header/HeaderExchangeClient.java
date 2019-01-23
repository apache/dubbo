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

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.timer.HashedWheelTimer;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Client;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.exchange.ExchangeClient;
import org.apache.dubbo.remoting.exchange.ExchangeHandler;
import org.apache.dubbo.remoting.exchange.ResponseFuture;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * DefaultMessageClient
 */
public class HeaderExchangeClient implements ExchangeClient {

    private final Client client;
    private final ExchangeChannel channel;
    private int heartbeat;
    private int idleTimeout;

    private static final HashedWheelTimer IDLE_CHECK_TIMER = new HashedWheelTimer(new NamedThreadFactory("dubbo-client-idleCheck", true), 1,
            TimeUnit.SECONDS, Constants.TICKS_PER_WHEEL);

    private HeartbeatTimerTask heartBeatTimerTask;

    private ReconnectTimerTask reconnectTimerTask;

    public HeaderExchangeClient(Client client, boolean needHeartbeat) {
        Assert.notNull(client, "Client can't be null");
        this.client = client;
        this.channel = new HeaderExchangeChannel(client);
        String dubbo = client.getUrl().getParameter(Constants.DUBBO_VERSION_KEY);

        this.heartbeat = client.getUrl().getParameter(Constants.HEARTBEAT_KEY, dubbo != null &&
                dubbo.startsWith("1.0.") ? Constants.DEFAULT_HEARTBEAT : 0);
        this.idleTimeout = client.getUrl().getParameter(Constants.HEARTBEAT_TIMEOUT_KEY, heartbeat * 3);
        if (idleTimeout < heartbeat * 2) {
            throw new IllegalStateException("idleTimeout < heartbeatInterval * 2");
        }

        if (needHeartbeat) {
            startIdleCheckTask();
        }
    }

    @Override
    public ResponseFuture request(Object request) throws RemotingException {
        return channel.request(request);
    }

    @Override
    public URL getUrl() {
        return channel.getUrl();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return channel.getRemoteAddress();
    }

    @Override
    public ResponseFuture request(Object request, int timeout) throws RemotingException {
        return channel.request(request, timeout);
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return channel.getChannelHandler();
    }

    @Override
    public boolean isConnected() {
        return channel.isConnected();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return channel.getLocalAddress();
    }

    @Override
    public ExchangeHandler getExchangeHandler() {
        return channel.getExchangeHandler();
    }

    @Override
    public void send(Object message) throws RemotingException {
        channel.send(message);
    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {
        channel.send(message, sent);
    }

    @Override
    public boolean isClosed() {
        return channel.isClosed();
    }

    @Override
    public void close() {
        doClose();
        channel.close();
    }

    @Override
    public void close(int timeout) {
        // Mark the client into the closure process
        startClose();
        doClose();
        channel.close(timeout);
    }

    @Override
    public void startClose() {
        channel.startClose();
    }

    @Override
    public void reset(URL url) {
        client.reset(url);
    }

    @Override
    @Deprecated
    public void reset(org.apache.dubbo.common.Parameters parameters) {
        reset(getUrl().addParameters(parameters.getParameters()));
    }

    @Override
    public void reconnect() throws RemotingException {
        client.reconnect();
    }

    @Override
    public Object getAttribute(String key) {
        return channel.getAttribute(key);
    }

    @Override
    public void setAttribute(String key, Object value) {
        channel.setAttribute(key, value);
    }

    @Override
    public void removeAttribute(String key) {
        channel.removeAttribute(key);
    }

    @Override
    public boolean hasAttribute(String key) {
        return channel.hasAttribute(key);
    }

    private void startIdleCheckTask() {
        AbstractTimerTask.ChannelProvider cp = () -> Collections.singletonList(HeaderExchangeClient.this);

        long heartbeatTick = calculateLeastDuration(heartbeat);
        long heartbeatTimeoutTick = calculateLeastDuration(idleTimeout);
        HeartbeatTimerTask heartBeatTimerTask = new HeartbeatTimerTask(cp, heartbeatTick, heartbeat);
        ReconnectTimerTask reconnectTimerTask = new ReconnectTimerTask(cp, heartbeatTimeoutTick, idleTimeout);

        this.heartBeatTimerTask = heartBeatTimerTask;
        this.reconnectTimerTask = reconnectTimerTask;

        // init task and start timer.
        IDLE_CHECK_TIMER.newTimeout(heartBeatTimerTask, heartbeatTick, TimeUnit.MILLISECONDS);
        IDLE_CHECK_TIMER.newTimeout(reconnectTimerTask, heartbeatTimeoutTick, TimeUnit.MILLISECONDS);
    }

    private void doClose() {
        heartBeatTimerTask.cancel();
        reconnectTimerTask.cancel();
    }

    /**
     * Each interval cannot be less than 1000ms.
     */
    private long calculateLeastDuration(int time) {
        if (time / Constants.HEARTBEAT_CHECK_TICK <= 0) {
            return Constants.LEAST_HEARTBEAT_DURATION;
        } else {
            return time / Constants.HEARTBEAT_CHECK_TICK;
        }
    }

    @Override
    public String toString() {
        return "HeaderExchangeClient [channel=" + channel + "]";
    }
}
