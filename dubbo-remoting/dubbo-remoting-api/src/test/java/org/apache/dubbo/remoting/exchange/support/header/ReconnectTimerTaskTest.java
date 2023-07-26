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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.remoting.Constants.HEARTBEAT_CHECK_TICK;

class ReconnectTimerTaskTest {

    private URL url = URL.valueOf("dubbo://localhost:20880");

    private MockChannel channel;

    private ReconnectTimerTask reconnectTimerTask;
    private HashedWheelTimer reconnectTimer;
    private boolean isConnected = false;

    @BeforeEach
    public void setup() throws Exception {
        long tickDuration = 1000;
        reconnectTimer = new HashedWheelTimer(tickDuration / HEARTBEAT_CHECK_TICK, TimeUnit.MILLISECONDS);
        channel = new MockChannel() {
            @Override
            public URL getUrl() {
                return url;
            }

            @Override
            public boolean isConnected() {
                return isConnected;
            }
        };

        reconnectTimerTask = new ReconnectTimerTask(() -> Collections.singleton(channel), reconnectTimer, tickDuration / HEARTBEAT_CHECK_TICK, (int) tickDuration);
    }

    @AfterEach
    public void teardown() {
        reconnectTimerTask.cancel();
    }

    @Test
    void testReconnect() throws Exception {
        long now = System.currentTimeMillis();

        url = url.addParameter(DUBBO_VERSION_KEY, "2.1.1");
        channel.setAttribute(HeartbeatHandler.KEY_READ_TIMESTAMP, now - 1000);
        channel.setAttribute(HeartbeatHandler.KEY_WRITE_TIMESTAMP, now - 1000);

        Thread.sleep(2000L);
        Assertions.assertTrue(channel.getReconnectCount() > 0);
        isConnected = true;
        Thread.sleep(2000L);
        Assertions.assertTrue(channel.getReconnectCount() > 1);
    }
}
