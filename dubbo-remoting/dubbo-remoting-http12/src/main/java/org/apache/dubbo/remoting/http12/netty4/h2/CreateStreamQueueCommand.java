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
package org.apache.dubbo.remoting.http12.netty4.h2;

import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.dubbo.remoting.http12.HttpChannel;
import org.apache.dubbo.remoting.http12.HttpChannelHolder;
import org.apache.dubbo.remoting.http12.command.QueueCommand;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;

import java.util.concurrent.CompletableFuture;

public class CreateStreamQueueCommand extends CompletableFuture<H2StreamChannel> implements QueueCommand, HttpChannelHolder {

    private final Http2StreamChannelBootstrap bootstrap;

    private CreateStreamQueueCommand(Http2StreamChannelBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public static CreateStreamQueueCommand create(Http2StreamChannelBootstrap bootstrap){
        return new CreateStreamQueueCommand(bootstrap);
    }

    @Override
    public void run() {
        bootstrap.open().addListener(new GenericFutureListener<Future<Http2StreamChannel>>() {
            @Override
            public void operationComplete(Future<Http2StreamChannel> future) throws Exception {
                if (future.isSuccess()) {
                    complete(new NettyH2StreamChannel(future.getNow()));
                } else {
                    completeExceptionally(future.cause());
                }
            }
        });
    }

    @Override
    public HttpChannel getHttpChannel() {
        return getNow(null);
    }
}
