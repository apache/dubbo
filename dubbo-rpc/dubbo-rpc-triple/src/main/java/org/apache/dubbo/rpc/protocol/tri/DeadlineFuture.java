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
import org.apache.dubbo.common.timer.HashedWheelTimer;
import org.apache.dubbo.common.timer.Timeout;
import org.apache.dubbo.common.timer.Timer;
import org.apache.dubbo.common.timer.TimerTask;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.TriRpcStatus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class DeadlineFuture extends CompletableFuture<AppResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeadlineFuture.class);
    private final String serviceName;
    private final String methodName;
    private final String address;
    private final int timeout;
    private final long start = System.currentTimeMillis();
    private final List<Runnable> timeoutListeners = new ArrayList<>();
    private final Timeout timeoutTask;
    private Executor executor;

    private DeadlineFuture(String serviceName, String methodName, String address, int timeout) {
        this.serviceName = serviceName;
        this.methodName = methodName;
        this.address = address;
        this.timeout = timeout;
        TimeoutCheckTask timeoutCheckTask = new TimeoutCheckTask();
        this.timeoutTask = TIME_OUT_TIMER.get()
            .newTimeout(timeoutCheckTask, timeout, TimeUnit.MILLISECONDS);
    }

    public static void destroy() {
        TIME_OUT_TIMER.remove(Timer::stop);
    }

    /**
     * init a DeadlineFuture 1.init a DeadlineFuture 2.timeout check
     *
     * @param timeout timeout in Mills
     * @return a new DeadlineFuture
     */
    public static DeadlineFuture newFuture(String serviceName, String methodName, String address,
        int timeout, Executor executor) {
        final DeadlineFuture future = new DeadlineFuture(serviceName, methodName, address, timeout);
        future.setExecutor(executor);
        return future;
    }

    public void received(TriRpcStatus status, AppResponse appResponse) {
        if (status.code != TriRpcStatus.Code.DEADLINE_EXCEEDED) {
            // decrease Time
            if (!timeoutTask.isCancelled()) {
                timeoutTask.cancel();
            }
        }
        if (getExecutor() != null) {
            getExecutor().execute(() -> doReceived(status, appResponse));
        } else {
            doReceived(status, appResponse);
        }
    }    private static final GlobalResourceInitializer<Timer> TIME_OUT_TIMER = new GlobalResourceInitializer<>(
        () -> new HashedWheelTimer(new NamedThreadFactory("dubbo-future-timeout", true), 30,
            TimeUnit.MILLISECONDS), DeadlineFuture::destroy);

    public void addTimeoutListener(Runnable runnable) {
        timeoutListeners.add(runnable);
    }

    public List<Runnable> getTimeoutListeners() {
        return timeoutListeners;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        timeoutTask.cancel();
        doReceived(TriRpcStatus.CANCELLED, new AppResponse(TriRpcStatus.CANCELLED.asException()));
        return true;
    }

    public void cancel() {
        this.cancel(true);
    }

    private void doReceived(TriRpcStatus status, AppResponse appResponse) {
        if (isDone() || isCancelled() || isCompletedExceptionally()) {
            return;
        }
        // Still needs to be discussed here, but for now, that's it
        // Remove the judgment of status is ok,
        // because the completelyExceptionally method will lead to the onError method in the filter,
        // but there are also exceptions in the onResponse in the filter,which is a bit confusing.
        // We recommend only handling onResponse in which onError is called for handling
        this.complete(appResponse);


    }

    private String getTimeoutMessage() {
        long nowTimestamp = System.currentTimeMillis();
        return "Waiting server-side response timeout by scan timer. start time: "
            + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(start)))
            + ", end time: " + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(
            new Date(nowTimestamp))) + ", timeout: " + timeout + " ms, service: " + serviceName
            + ", method: " + methodName;
    }

    private class TimeoutCheckTask implements TimerTask {

        @Override
        public void run(Timeout timeout) {
            if (DeadlineFuture.this.isDone()) {
                return;
            }

            if (getExecutor() != null) {
                getExecutor().execute(() -> {
                    notifyTimeout();
                    for (Runnable timeoutListener : getTimeoutListeners()) {
                        timeoutListener.run();
                    }
                });
            } else {
                notifyTimeout();
            }
        }

        private void notifyTimeout() {
            final TriRpcStatus status = TriRpcStatus.DEADLINE_EXCEEDED.withDescription(
                getTimeoutMessage());
            AppResponse timeoutResponse = new AppResponse();
            timeoutResponse.setException(status.asException());
            DeadlineFuture.this.doReceived(status, timeoutResponse);
        }
    }

}
