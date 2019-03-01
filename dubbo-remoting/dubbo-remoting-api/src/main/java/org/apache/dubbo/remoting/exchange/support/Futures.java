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
package org.apache.dubbo.remoting.exchange.support;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.timer.HashedWheelTimer;
import org.apache.dubbo.common.timer.Timer;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.TimeoutException;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class Futures {

    private static final Logger logger = LoggerFactory.getLogger(Futures.class);

    private static final Map<Long, Channel> CHANNELS = new ConcurrentHashMap<>();

    private static final Map<Long, CompletableFuture<Object>> FUTURES = new ConcurrentHashMap<>();

    public static final Timer TIME_OUT_TIMER = new HashedWheelTimer(
            new NamedThreadFactory("dubbo-future-timeout", true),
            30,
            TimeUnit.MILLISECONDS);

    public static CompletableFuture<Object> newFuture (Channel channel, Request request, int timeout)  {
        final DefaultFuture future = new (channel, request, timeout);
        // timeout check
        timeoutCheck(future);
        return future;
    }

    public static void received(Channel channel, Response response) throws RemotingException {
        try {
            if (response == null) {
                throw new IllegalStateException("response cannot be null");
            }
            CompletableFuture<Object> future = FUTURES.remove(response.getId());
            if (future != null) {
                if (response.getStatus() == Response.OK) {
                    future.complete(response.getResult());
                }
                if (response.getStatus() == Response.CLIENT_TIMEOUT || response.getStatus() == Response.SERVER_TIMEOUT) {
                    future.completeExceptionally(
                            new TimeoutException(response.getStatus() == Response.SERVER_TIMEOUT,
                                                 channel,
                                                 response.getErrorMessage())
                    );
                }
                future.completeExceptionally(new RemotingException(channel, response.getErrorMessage());
            } else {
                logger.warn("The timeout response finally returned at "
                                    + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()))
                                    + ", response " + response
                                    + (channel == null ? "" : ", channel: " + channel.getLocalAddress()
                        + " -> " + channel.getRemoteAddress()));
            }
        } finally {
            CHANNELS.remove(response.getId());
        }
    }

    public static void cancel(CompletableFuture<Object> future) {
        Response errorResult = new Response(id);
        errorResult.setErrorMessage("request future has been canceled.");
        response = errorResult;
        FUTURES.remove(id);
        CHANNELS.remove(id);
    }
}
