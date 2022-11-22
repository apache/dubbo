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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.timer.HashedWheelTimer;
import org.apache.dubbo.remoting.Channel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.remoting.Constants.HEARTBEAT_CHECK_TICK;

/**
 * {@link CloseTimerTask}
 */
public class CloseTimerTaskTest {

    private URL url = URL.valueOf("dubbo://localhost:20880");

    private MockChannel channel;

    private CloseTimerTask closeTimerTask;
    private HashedWheelTimer closeTimer;

    @BeforeEach
    public void setup() throws Exception {
        long tickDuration = 1000;
        closeTimer = new HashedWheelTimer(tickDuration / HEARTBEAT_CHECK_TICK, TimeUnit.MILLISECONDS);
        channel = new MockChannel() {
            @Override
            public URL getUrl() {
                return url;
            }
        };

        AbstractTimerTask.ChannelProvider cp = () -> Collections.<Channel>singletonList(channel);
        closeTimerTask = new CloseTimerTask(cp, tickDuration / HEARTBEAT_CHECK_TICK, (int) tickDuration);
    }

    @Test
    public void testClose() throws Exception {
        long now = System.currentTimeMillis();

        url = url.addParameter(DUBBO_VERSION_KEY, "2.1.1");
        channel.setAttribute(HeartbeatHandler.KEY_READ_TIMESTAMP, now - 1000);
        channel.setAttribute(HeartbeatHandler.KEY_WRITE_TIMESTAMP, now - 1000);

        closeTimer.newTimeout(closeTimerTask, 250, TimeUnit.MILLISECONDS);

        Thread.sleep(2000L);
        Assertions.assertTrue(channel.isClosed());
    }

}
