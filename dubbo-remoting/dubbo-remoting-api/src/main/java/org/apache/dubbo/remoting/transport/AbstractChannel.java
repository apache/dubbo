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
package org.apache.dubbo.remoting.transport;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbstractChannel
 */
public abstract class AbstractChannel extends AbstractPeer implements Channel {

    private final Map<Long, Request> UN_FINISH_REQUESTS_MAP = new ConcurrentHashMap<Long, Request>();

    public AbstractChannel(URL url, ChannelHandler handler) {
        super(url, handler);
    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {
        if (isClosed()) {
            throw new RemotingException(this, "Failed to send message "
                    + (message == null ? "" : message.getClass().getName()) + ":" + message
                    + ", cause: Channel closed. channel: " + getLocalAddress() + " -> " + getRemoteAddress());
        }
        if (message instanceof Request) {
            Request r = (Request) message;
            UN_FINISH_REQUESTS_MAP.put(r.getId(), r);
        }
    }

    @Override
    public void finishRequest(Response response) {
        UN_FINISH_REQUESTS_MAP.remove(response.getId());
    }

    @Override
    public void clearUnFinishedRequests() {
        // clear unfinish requests when close the channel.
        if (UN_FINISH_REQUESTS_MAP.size() > 0) {
            for (Request r : UN_FINISH_REQUESTS_MAP.values()) {
                Response disconnectResponse = new Response(r.getId());
                disconnectResponse.setStatus(Response.CHANNEL_INACTIVE);
                disconnectResponse.setErrorMessage("Channel " + this + " is inactive. Directly return the unFinished request.");
                DefaultFuture.received(this, disconnectResponse);
            }
            UN_FINISH_REQUESTS_MAP.clear();
        }
    }

    @Override
    public String toString() {
        return getLocalAddress() + " -> " + getRemoteAddress();
    }
}
