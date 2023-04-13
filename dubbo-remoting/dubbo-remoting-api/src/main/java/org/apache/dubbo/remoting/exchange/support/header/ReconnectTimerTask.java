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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.timer.HashedWheelTimer;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.Client;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_RECONNECT;

/**
 * ReconnectTimerTask
 */
public class ReconnectTimerTask extends AbstractTimerTask {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ReconnectTimerTask.class);

    private final int idleTimeout;

    public ReconnectTimerTask(ChannelProvider channelProvider, HashedWheelTimer hashedWheelTimer, Long heartbeatTimeoutTick, int idleTimeout) {
        super(channelProvider, hashedWheelTimer, heartbeatTimeoutTick);
        this.idleTimeout = idleTimeout;
    }

    @Override
    protected void doTask(Channel channel) {
        try {
            Long lastRead = lastRead(channel);
            Long now = now();

            // Rely on reconnect timer to reconnect when AbstractClient.doConnect fails to init the connection
            if (!channel.isConnected()) {
                try {
                    logger.info("Initial connection to " + channel);
                    ((Client) channel).reconnect();
                } catch (Exception e) {
                    logger.error(TRANSPORT_FAILED_RECONNECT, "", "", "Fail to connect to" + channel, e);
                }
                // check pong at client
            } else if (lastRead != null && now - lastRead > idleTimeout) {
                logger.warn(TRANSPORT_FAILED_RECONNECT, "", "", "Reconnect to channel " + channel + ", because heartbeat read idle time out: "
                    + idleTimeout + "ms");
                try {
                    ((Client) channel).reconnect();
                } catch (Exception e) {
                    logger.error(TRANSPORT_FAILED_RECONNECT, "", "", channel + "reconnect failed during idle time.", e);
                }
            }
        } catch (Throwable t) {
            logger.warn(INTERNAL_ERROR, "unknown error in remoting module", "", "Exception when reconnect to remote channel " + channel.getRemoteAddress(), t);
        }
    }
}
