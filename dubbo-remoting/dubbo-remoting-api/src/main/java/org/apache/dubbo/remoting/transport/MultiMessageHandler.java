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
package org.apache.dubbo.remoting.transport;

<<<<<<< HEAD
import org.apache.dubbo.common.logger.Logger;
=======
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
>>>>>>> origin/3.2
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.support.MultiMessage;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;

/**
 * @see MultiMessage
 */
public class MultiMessageHandler extends AbstractChannelHandlerDelegate {

<<<<<<< HEAD
    protected static final Logger logger = LoggerFactory.getLogger(MultiMessageHandler.class);
=======
    protected static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(MultiMessageHandler.class);
>>>>>>> origin/3.2

    public MultiMessageHandler(ChannelHandler handler) {
        super(handler);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void received(Channel channel, Object message) throws RemotingException {
        if (message instanceof MultiMessage) {
            MultiMessage list = (MultiMessage) message;
            for (Object obj : list) {
                try {
                    handler.received(channel, obj);
                } catch (Throwable t) {
<<<<<<< HEAD
                    logger.error("MultiMessageHandler received fail.", t);
                    try {
                        handler.caught(channel, t);
                    } catch (Throwable t1) {
                        logger.error("MultiMessageHandler caught fail.", t1);
=======
                    logger.error(INTERNAL_ERROR, "unknown error in remoting module", "", "MultiMessageHandler received fail.", t);
                    try {
                        handler.caught(channel, t);
                    } catch (Throwable t1) {
                        logger.error(INTERNAL_ERROR, "unknown error in remoting module", "", "MultiMessageHandler caught fail.", t1);
>>>>>>> origin/3.2
                    }
                }
            }
        } else {
            handler.received(channel, message);
        }
    }
}
