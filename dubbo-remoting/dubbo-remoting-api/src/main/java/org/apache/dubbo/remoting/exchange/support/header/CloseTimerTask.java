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

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_RESPONSE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_CLOSE;

/**
 * CloseTimerTask
 */
public class CloseTimerTask extends AbstractTimerTask {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(CloseTimerTask.class);

    private final int closeTimeout;

    public CloseTimerTask(ChannelProvider channelProvider, HashedWheelTimer hashedWheelTimer, Long tick, int closeTimeout) {
        super(channelProvider, hashedWheelTimer, tick);
        this.closeTimeout = closeTimeout;
    }

    @Override
    protected void doTask(Channel channel) {
        try {
            Long lastRead = lastRead(channel);
            Long lastWrite = lastWrite(channel);
            Long now = now();
            // check ping & pong at server
            if ((lastRead != null && now - lastRead > closeTimeout)
                || (lastWrite != null && now - lastWrite > closeTimeout)) {
                logger.warn(PROTOCOL_FAILED_RESPONSE, "", "", "Close channel " + channel + ", because idleCheck timeout: " + closeTimeout + "ms");
                channel.close();
            }
        } catch (Throwable t) {
            logger.warn(TRANSPORT_FAILED_CLOSE, "", "", "Exception when close remote channel " + channel.getRemoteAddress(), t);
        }
    }
}
