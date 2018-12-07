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
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.Client;

/**
 * ReconnectTimerTask
 */
public class ReconnectTimerTask extends AbstractTimerTask {

    private static final Logger logger = LoggerFactory.getLogger(ReconnectTimerTask.class);

    private final int heartbeatTimeout;

    ReconnectTimerTask(ChannelProvider channelProvider, Long heartbeatTimeoutTick, int heartbeatTimeout1) {
        super(channelProvider, heartbeatTimeoutTick);
        this.heartbeatTimeout = heartbeatTimeout1;
    }

    @Override
    protected void doTask(Channel channel) {
        try {
            Long lastRead = lastRead(channel);
            Long now = now();
            if (lastRead != null && now - lastRead > heartbeatTimeout) {
                logger.warn("Close channel " + channel + ", because heartbeat read idle time out: "
                        + heartbeatTimeout + "ms");
                if (channel instanceof Client) {
                    try {
                        ((Client) channel).reconnect();
                    } catch (Exception e) {
                        //do nothing
                    }
                } else {
                    channel.close();
                }
            }
        } catch (Throwable t) {
            logger.warn("Exception when reconnect to remote channel " + channel.getRemoteAddress(), t);
        }
    }
}
