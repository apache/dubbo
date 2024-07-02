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
package org.apache.dubbo.common.threadpool.support.loom;

import org.apache.dubbo.common.timer.Timeout;
import org.apache.dubbo.common.timer.Timer;
import org.apache.dubbo.common.timer.TimerTask;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class VirtualTimeout implements Timeout {
    private final VirtualTimer timer;
    private final TimerTask task;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicReference<Future<?>> taskFuture = new AtomicReference<>();

    public VirtualTimeout(VirtualTimer timer, TimerTask task) {
        this.timer = timer;
        this.task = task;
    }

    @Override
    public Timer timer() {
        return timer;
    }

    @Override
    public TimerTask task() {
        return task;
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public boolean cancel() {
        cancelled.set(true);
        if (taskFuture.get() != null) {
            taskFuture.get().cancel(true);
        }
        timer.removeTimeout(this);
        return true;
    }

    protected void setTaskFuture(Future<?> future) {
        taskFuture.set(future);
    }
}
