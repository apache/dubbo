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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.concurrent.TimeUnit;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

public class ReconnectTimerTask implements TimerTask {

    private static final Logger logger = LoggerFactory.getLogger(ReconnectTimerTask.class);

    private final Runnable reconnectTask;
    private final HashedWheelTimer timer;
    private final long tick;

    private volatile Timeout timeout;

    public ReconnectTimerTask(Runnable reconnectTask, HashedWheelTimer timer, long tick, int unused) {
        this.reconnectTask = reconnectTask;
        this.timer = timer;
        this.tick = tick;
    }

    @Override
    public void run(Timeout timeout) {
        this.timeout = timeout;
        reconnectTask.run();
    }

    /**
     * Start reconnect timer.
     * If already started, cancel previous timeout to avoid duplicate reconnects.
     */
    public synchronized void start() {
        cancel();
        logger.info("ReconnectTimerTask.start(), scheduling reconnect after {} ms", tick);
        this.timeout = timer.newTimeout(this, tick, TimeUnit.MILLISECONDS);
    }

    /**
     * Cancel reconnect timer.
     */
    public synchronized void cancel() {
        logger.info("ReconnectTimerTask.cancel() called");
        if (timeout != null) {
            timeout.cancel();
            timeout = null;
        }
    }
}
