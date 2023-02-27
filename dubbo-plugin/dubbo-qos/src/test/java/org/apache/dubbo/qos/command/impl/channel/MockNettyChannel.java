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
package org.apache.dubbo.qos.command.impl.channel;

import org.apache.dubbo.common.URL;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.AttributeMap;
import io.netty.util.DefaultAttributeMap;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MockNettyChannel implements Channel {

    InetSocketAddress localAddress;
    InetSocketAddress remoteAddress;
    private URL remoteUrl;
    private List<Object> receivedObjects = new LinkedList<>();
    public static final String ERROR_WHEN_SEND = "error_when_send";
    private CountDownLatch latch;
    private AttributeMap attributeMap =  new DefaultAttributeMap();

    public MockNettyChannel(URL remoteUrl, CountDownLatch latch) {
        this.remoteUrl = remoteUrl;
        this.latch = latch;
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg) {
        receivedObjects.add(msg);
        if (latch != null) {
            latch.countDown();
        }
        return newPromise();
    }

    @Override
    public ChannelPromise newPromise() {
        return new ChannelPromise() {
            @Override
            public Channel channel() {
                return null;
            }

            @Override
            public ChannelPromise setSuccess(Void result) {
                return null;
            }

            @Override
            public ChannelPromise setSuccess() {
                return null;
            }

            @Override
            public boolean trySuccess() {
                return false;
            }

            @Override
            public ChannelPromise setFailure(Throwable cause) {
                return null;
            }

            @Override
            public ChannelPromise addListener(GenericFutureListener<? extends Future<? super Void>> listener) {
                return null;
            }

            @Override
            public ChannelPromise addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners) {
                return null;
            }

            @Override
            public ChannelPromise removeListener(GenericFutureListener<? extends Future<? super Void>> listener) {
                return null;
            }

            @Override
            public ChannelPromise removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners) {
                return null;
            }

            @Override
            public ChannelPromise sync() throws InterruptedException {
                return null;
            }

            @Override
            public ChannelPromise syncUninterruptibly() {
                return null;
            }

            @Override
            public ChannelPromise await() throws InterruptedException {
                return this;
            }

            @Override
            public ChannelPromise awaitUninterruptibly() {
                return null;
            }

            @Override
            public ChannelPromise unvoid() {
                return null;
            }

            @Override
            public boolean isVoid() {
                return false;
            }

            @Override
            public boolean trySuccess(Void result) {
                return false;
            }

            @Override
            public boolean tryFailure(Throwable cause) {
                return false;
            }

            @Override
            public boolean setUncancellable() {
                return false;
            }

            @Override
            public boolean isSuccess() {
                return false;
            }

            @Override
            public boolean isCancellable() {
                return false;
            }

            @Override
            public Throwable cause() {
                return null;
            }

            @Override
            public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
                return true;
            }

            @Override
            public boolean await(long timeoutMillis) throws InterruptedException {
                return true;
            }

            @Override
            public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
                return true;
            }

            @Override
            public boolean awaitUninterruptibly(long timeoutMillis) {
                return false;
            }

            @Override
            public Void getNow() {
                return null;
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public Void get() throws InterruptedException, ExecutionException {
                return null;
            }

            @Override
            public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return null;
            }
        };
    }

    @Override
    public ChannelProgressivePromise newProgressivePromise() {
        return null;
    }

    @Override
    public ChannelFuture newSucceededFuture() {
        return null;
    }

    @Override
    public ChannelFuture newFailedFuture(Throwable cause) {
        return null;
    }

    @Override
    public ChannelPromise voidPromise() {
        return null;
    }

    @Override
    public ChannelId id() {
        return null;
    }

    @Override
    public EventLoop eventLoop() {
        return null;
    }

    @Override
    public Channel parent() {
        return null;
    }

    @Override
    public ChannelConfig config() {
        return null;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public boolean isRegistered() {
        return false;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public ChannelMetadata metadata() {
        return null;
    }

    @Override
    public SocketAddress localAddress() {
        return null;
    }

    @Override
    public SocketAddress remoteAddress() {
        return null;
    }

    @Override
    public ChannelFuture closeFuture() {
        return null;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public long bytesBeforeUnwritable() {
        return 0;
    }

    @Override
    public long bytesBeforeWritable() {
        return 0;
    }

    @Override
    public Unsafe unsafe() {
        return null;
    }

    @Override
    public ChannelPipeline pipeline() {
        return null;
    }

    @Override
    public ByteBufAllocator alloc() {
        return null;
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress) {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        return null;
    }

    @Override
    public ChannelFuture disconnect() {
        return null;
    }

    @Override
    public ChannelFuture close() {
        return null;
    }

    @Override
    public ChannelFuture deregister() {
        return null;
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture disconnect(ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture close(ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture deregister(ChannelPromise promise) {
        return null;
    }

    @Override
    public Channel read() {
        return null;
    }

    @Override
    public ChannelFuture write(Object msg) {
        return null;
    }

    @Override
    public ChannelFuture write(Object msg, ChannelPromise promise) {
        return null;
    }

    @Override
    public Channel flush() {
        return null;
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        return null;
    }

    @Override
    public <T> Attribute<T> attr(AttributeKey<T> key) {
        return attributeMap.attr(key);
    }

    @Override
    public <T> boolean hasAttr(AttributeKey<T> key) {
        return attributeMap.hasAttr(key);
    }

    @Override
    public int compareTo(Channel o) {
        return 0;
    }

    public List<Object> getReceivedObjects() {
        return receivedObjects;
    }
}
