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
import org.apache.dubbo.remoting.exchange.Request;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.remoting.Constants.HEARTBEAT_CHECK_TICK;

public class HeartBeatTaskTest {

    private URL url = URL.valueOf("dubbo://localhost:20880");

    private MockChannel channel;

    private HeartbeatTimerTask heartbeatTimerTask;
    private HashedWheelTimer heartbeatTimer;

    @BeforeEach
    public void setup() throws Exception {
        long tickDuration = 1000;
        heartbeatTimer = new HashedWheelTimer(tickDuration / HEARTBEAT_CHECK_TICK, TimeUnit.MILLISECONDS);

        channel = new MockChannel() {

            @Override
            public URL getUrl() {
                return url;
            }
        };

        AbstractTimerTask.ChannelProvider cp = () -> Collections.<Channel>singletonList(channel);
        heartbeatTimerTask = new HeartbeatTimerTask(cp, tickDuration / HEARTBEAT_CHECK_TICK, (int) tickDuration);
    }

    @Test
    public void testHeartBeat() throws Exception {
        long now = System.currentTimeMillis();

        url = url.addParameter(DUBBO_VERSION_KEY, "2.1.1");
        channel.setAttribute(HeartbeatHandler.KEY_READ_TIMESTAMP, now);
        channel.setAttribute(HeartbeatHandler.KEY_WRITE_TIMESTAMP, now);

        heartbeatTimer.newTimeout(heartbeatTimerTask, 250, TimeUnit.MILLISECONDS);

        Thread.sleep(2000L);
        List<Object> objects = channel.getSentObjects();
        Assertions.assertTrue(objects.size() > 0);
        Object obj = objects.get(0);
        Assertions.assertTrue(obj instanceof Request);
        Request request = (Request) obj;
        Assertions.assertTrue(request.isHeartbeat());
    }

}
