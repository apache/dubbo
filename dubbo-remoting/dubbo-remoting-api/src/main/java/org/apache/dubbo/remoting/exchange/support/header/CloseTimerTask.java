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

/**
 * CloseTimerTask
 */
public class CloseTimerTask extends AbstractTimerTask {

    private static final Logger logger = LoggerFactory.getLogger(CloseTimerTask.class);

    private final int idleTimeout;

    public CloseTimerTask(ChannelProvider channelProvider, Long heartbeatTimeoutTick, int idleTimeout) {
        super(channelProvider, heartbeatTimeoutTick);
        this.idleTimeout = idleTimeout;
    }

    @Override
    protected void doTask(Channel channel) {
        try {
            Long lastRead = lastRead(channel);
            Long lastWrite = lastWrite(channel);
            Long now = now();
            // check ping & pong at server
            if ((lastRead != null && now - lastRead > idleTimeout)
                    || (lastWrite != null && now - lastWrite > idleTimeout)) {
                logger.warn("Close channel " + channel + ", because idleCheck timeout: "
                        + idleTimeout + "ms");
                channel.close();
            }
        } catch (Throwable t) {
            logger.warn("Exception when close remote channel " + channel.getRemoteAddress(), t);
        }
    }
}
