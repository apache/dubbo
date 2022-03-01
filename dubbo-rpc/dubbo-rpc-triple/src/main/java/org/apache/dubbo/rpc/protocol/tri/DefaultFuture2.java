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

package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.resource.GlobalResourceInitializer;
import org.apache.dubbo.common.threadpool.ThreadlessExecutor;
import org.apache.dubbo.common.timer.HashedWheelTimer;
import org.apache.dubbo.common.timer.Timeout;
import org.apache.dubbo.common.timer.Timer;
import org.apache.dubbo.common.timer.TimerTask;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.TimeoutException;
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;

import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DefaultFuture2.
 * This class is duplicated with {@link DefaultFuture} because the underlying connection abstraction was not designed for
 * multiple protocol.
 * TODO Remove this class and abstract common logic for waiting async result.
 */
public class DefaultFuture2 extends CompletableFuture<Object> {

    private static final AtomicLong INVOKE_ID = new AtomicLong(0);

    private static final Logger logger = LoggerFactory.getLogger(DefaultFuture2.class);
    private static final Map<Long, DefaultFuture2> FUTURES = new ConcurrentHashMap<>();
    public final long requestId;
    private final Invocation invocation;
    private final Connection connection;
    private final int timeout;
    private final long start = System.currentTimeMillis();
    private final List<Runnable> timeoutListeners = new ArrayList<>();    private static final GlobalResourceInitializer<Timer> TIME_OUT_TIMER = new GlobalResourceInitializer<>(() -> new HashedWheelTimer(new NamedThreadFactory("dubbo-future-timeout", true), 30, TimeUnit.MILLISECONDS), DefaultFuture2::destroy);
    private Timeout timeoutCheckTask;
    private volatile long sent;
    private ExecutorService executor;
    private DefaultFuture2(Connection connection, Invocation invocation, int timeout) {
        this.connection = connection;
        this.invocation = invocation;
        this.timeout = timeout;
        this.requestId = INVOKE_ID.getAndIncrement();
        // put into waiting map.
        FUTURES.put(requestId, this);
    }

    public static void addTimeoutListener(long id, Runnable runnable) {
        DefaultFuture2 future = FUTURES.get(id);
        if (future == null) {
            // already time out
            runnable.run();
        } else {
            future.addTimeoutListener(runnable);
        }
    }

    /**
     * check time out of the future
     */
    private static void timeoutCheck(DefaultFuture2 future) {
        TimeoutCheckTask task = new TimeoutCheckTask(future.requestId);
        future.timeoutCheckTask = TIME_OUT_TIMER.get().newTimeout(task, future.getTimeout(), TimeUnit.MILLISECONDS);
    }

    public static void destroy() {
        TIME_OUT_TIMER.remove(Timer::stop);
        FUTURES.clear();
    }

    /**
     * init a DefaultFuture
     * 1.init a DefaultFuture
     * 2.timeout check
     *
     * @param connection connection
     * @param invocation the invocation
     * @param timeout    timeout
     * @return a new DefaultFuture
     */
    public static DefaultFuture2 newFuture(Connection connection, Invocation invocation, int timeout, ExecutorService executor) {
        final DefaultFuture2 future = new DefaultFuture2(connection, invocation, timeout);
        future.setExecutor(executor);
        // ThreadlessExecutor needs to hold the waiting future in case of circuit return.
        if (executor instanceof ThreadlessExecutor) {
            ((ThreadlessExecutor) executor).setWaitingFuture(future);
        }
        // timeout check
        timeoutCheck(future);
        return future;
    }

    public static DefaultFuture2 getFuture(long id) {
        return FUTURES.get(id);
    }

    public static void sent(long requestId) {
        DefaultFuture2 future = FUTURES.get(requestId);
        if (future != null) {
            future.doSent();
        }
    }

    public static void received(long requestId, RpcStatus status, AppResponse appResponse) {
        DefaultFuture2 future = FUTURES.remove(requestId);
        if (future != null) {
            Timeout t = future.timeoutCheckTask;
            if (status.code != RpcStatus.Code.DEADLINE_EXCEEDED) {
                // decrease Time
                t.cancel();
            }
            if (future.executor != null) {
                future.executor.execute(() -> future.doReceived(status, appResponse));
            } else {
                future.doReceived(status, appResponse);
            }
        }
    }

    public void addTimeoutListener(Runnable runnable) {
        timeoutListeners.add(runnable);
    }

    public List<Runnable> getTimeoutListeners() {
        return timeoutListeners;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        doReceived(RpcStatus.CANCELLED, null);
        FUTURES.remove(requestId);
        timeoutCheckTask.cancel();
        return true;
    }

    public void cancel() {
        this.cancel(true);
    }

    private void doReceived(RpcStatus status, AppResponse appResponse) {
        if (status.isOk()) {
            this.complete(appResponse);
        } else if (status.code == RpcStatus.Code.DEADLINE_EXCEEDED) {
            this.completeExceptionally(new TimeoutException(isSent(), null, connection.getRemote(), "Request timeout"));
        } else {
            final InetSocketAddress local;
            if (connection.getChannel() != null) {
                local = (InetSocketAddress) connection.getChannel().localAddress();
            } else {
                local = null;
            }
            this.completeExceptionally(new RemotingException(local, connection.getRemote(), status.toMessage()));
        }

        // the result is returning, but the caller thread may still waiting
        // to avoid endless waiting for whatever reason, notify caller thread to return.
        if (executor != null && executor instanceof ThreadlessExecutor) {
            ThreadlessExecutor threadlessExecutor = (ThreadlessExecutor) executor;
            if (threadlessExecutor.isWaiting()) {
                threadlessExecutor.notifyReturn(new IllegalStateException("The result has returned, but the biz thread is still waiting" + " which is not an expected state, interrupt the thread manually by returning an exception."));
            }
        }
    }

    private boolean isSent() {
        return sent > 0;
    }

    private int getTimeout() {
        return timeout;
    }

    private void doSent() {
        sent = System.currentTimeMillis();
    }

    private String getTimeoutMessage() {
        long nowTimestamp = System.currentTimeMillis();
        return (sent > 0 ? "Waiting server-side response timeout" : "Sending request timeout in client-side") + " by scan timer. start time: " + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(start))) + ", end time: " + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(nowTimestamp))) + "," + (sent > 0 ? " client elapsed: " + (sent - start) + " ms, server elapsed: " + (nowTimestamp - sent) : " elapsed: " + (nowTimestamp - start)) + " ms, timeout: " + timeout + " ms, request: " + (logger.isDebugEnabled() ? invocation : getRequestWithoutPayload()) + ", channel: " + connection.getChannel();
    }

    private String getRequestWithoutPayload() {
        return "Request(id=" + requestId + ", service=" + invocation.getTargetServiceUniqueName() + ", method=" + invocation.getMethodName() + ")";
    }

    private static class TimeoutCheckTask implements TimerTask {

        private final Long requestID;

        TimeoutCheckTask(Long requestID) {
            this.requestID = requestID;
        }

        @Override
        public void run(Timeout timeout) {
            DefaultFuture2 future = DefaultFuture2.getFuture(requestID);
            if (future == null || future.isDone()) {
                return;
            }

            if (future.getExecutor() != null) {
                future.getExecutor().execute(() -> {
                    notifyTimeout(future);
                    for (Runnable timeoutListener : future.getTimeoutListeners()) {
                        timeoutListener.run();
                    }
                });
            } else {
                notifyTimeout(future);
            }
        }

        private void notifyTimeout(DefaultFuture2 future) {
            final RpcStatus status = RpcStatus.DEADLINE_EXCEEDED.withDescription(future.getTimeoutMessage());
            DefaultFuture2.received(future.requestId, status, null);
        }
    }




}
