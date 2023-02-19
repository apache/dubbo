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
package org.apache.dubbo.remoting.transport.netty4;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import org.apache.dubbo.common.BatchExecutorQueue;
import org.apache.dubbo.remoting.exchange.support.MultiMessage;

import java.util.LinkedList;
import java.util.Queue;

/**
 * netty4 batch write queue
 */
public class Netty4BatchWriteQueue extends BatchExecutorQueue<Netty4BatchWriteQueue.MessageTuple> {

    private final Channel channel;

    private final EventLoop eventLoop;

    private final Queue<ChannelPromise> promises = new LinkedList<>();

    private final MultiMessage multiMessage = MultiMessage.create();

    private Netty4BatchWriteQueue(Channel channel) {
        this.channel = channel;
        this.eventLoop = channel.eventLoop();
    }

    public ChannelFuture enqueue(Object message) {
        return enqueue(message, channel.newPromise());
    }

    public ChannelFuture enqueue(Object message, ChannelPromise channelPromise) {
        MessageTuple messageTuple = new MessageTuple(message, channelPromise);
        super.enqueue(messageTuple, eventLoop);
        return messageTuple.channelPromise;
    }

    @Override
    protected void prepare(MessageTuple item) {
        multiMessage.addMessage(item.originMessage);
        promises.add(item.channelPromise);
    }

    @Override
    protected void flush(MessageTuple item) {
        prepare(item);
        Object finalMessage = multiMessage;
        if (multiMessage.size() == 1) {
            finalMessage = multiMessage.get(0);
        }
        channel.writeAndFlush(finalMessage).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                ChannelPromise cp;
                while ((cp = promises.poll()) != null) {
                    if (future.isSuccess()){
                        cp.setSuccess();
                    } else {
                        cp.setFailure(future.cause());
                    }
                }
            }
        });
        this.multiMessage.removeMessages();
    }

    public static Netty4BatchWriteQueue createWriteQueue(Channel channel) {
        return new Netty4BatchWriteQueue(channel);
    }

    static class MessageTuple {

        private final Object originMessage;

        private final ChannelPromise channelPromise;

        public MessageTuple(Object originMessage, ChannelPromise channelPromise) {
            this.originMessage = originMessage;
            this.channelPromise = channelPromise;
        }

    }
}
