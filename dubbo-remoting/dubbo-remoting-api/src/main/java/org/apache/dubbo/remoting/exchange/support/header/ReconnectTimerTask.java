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

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.timer.Timeout;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.Client;

/**
 * ReconnectTimerTask
 */
public class ReconnectTimerTask extends AbstractTimerTask {

    private static final Logger logger = LoggerFactory.getLogger(ReconnectTimerTask.class);

    ReconnectTimerTask(Channel channel, long reconnectedTick) {
        super(channel, reconnectedTick);
    }

    @Override
    protected void doTask(Channel channel, Timeout timeout) {
        Long lastRead = lastRead(channel);
        long heartBeatDuration = lastRead == null ? 0 : now() - lastRead;
        if (heartBeatDuration > getTick()) {
            if (Constants.CONSUMER_SIDE.equals(channel.getUrl().getParameter(Constants.SIDE_KEY))) {
                try {
                    logger.warn("Reconnect to remote channel " + channel.getRemoteAddress() + ", because heartbeat read idle time out: "
                            + getTick() + "ms");
                    ((Client) channel).reconnect();
                } catch (Throwable t) {
                    // do nothing
                }
            } else {
                try {
                    logger.warn("Close channel " + channel + ", because heartbeat read idle time out: "
                            + getTick() + "ms");
                    channel.close();
                    // For provider side, if the channel is closed, just return.
                    return;
                } catch (Throwable t) {
                    logger.warn("Exception when close channel " + channel, t);
                }
            }
        }
        // Set the next heartbeat task with recalculate tick duration.
        if (!channel.isClosed()) {
            reput(timeout, Math.min(getTick() - heartBeatDuration > 0 ? getTick() - heartBeatDuration : getTick(), getTick()));
        }

    }
}
