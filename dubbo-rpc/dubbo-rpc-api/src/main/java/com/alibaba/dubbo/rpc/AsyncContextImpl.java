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
package com.alibaba.dubbo.rpc;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.remoting.Channel;

import java.util.concurrent.CompletableFuture;

public class AsyncContextImpl implements AsyncContext {
    private static final Logger logger = LoggerFactory.getLogger(AsyncContextImpl.class);

    private boolean started = false;

    private CompletableFuture<Object> future;

    public AsyncContextImpl() {
    }

    public AsyncContextImpl(Channel channel, CompletableFuture<Object> future) {
        this.future = future;
        /*this.future.whenCompleteAsync((result, t) -> {
            Response res = new Response();
            try {
                if (t == null) {
                    res.setStatus(Response.OK);
                    res.setResult(result);
                } else {
                    res.setStatus(Response.SERVICE_ERROR);
                    res.setErrorMessage(StringUtils.toString(t));
                }
                channel.send(res);
            } catch (RemotingException e) {
                logger.warn("Send result to consumer failed, channel is " + channel + ", msg is " + e);
            } finally {
                // HeaderExchangeChannel.removeChannelIfDisconnected(channel);
            }
        });*/
    }

    @Override
    public void addListener(Runnable run) {

    }

    @Override
    public void write(Object value) {
        if (value instanceof Throwable) {
            // TODO check exception type like ExceptionFilter do.
        }
        Result result = new RpcResult(value);
        future.complete(result);
    }

    @Override
    public boolean isAsyncStarted() {
        return started;
    }

    @Override
    public void stop() {
        this.started = false;
        future.cancel(true);
    }

    @Override
    public void start() {
        this.started = true;
    }
}
