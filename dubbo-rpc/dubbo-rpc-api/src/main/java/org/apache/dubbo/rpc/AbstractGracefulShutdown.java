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
package org.apache.dubbo.rpc;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.ChannelEvent;

import java.util.Collection;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_CLOSE_STREAM;

/**
 * Abstract base class for graceful shutdown implementations.
 * <p>
 * This class provides common functionality for graceful shutdown across different protocols.
 * </p>
 */
public abstract class AbstractGracefulShutdown implements GracefulShutdown {

    protected final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    /**
     * Get the collection of protocol servers.
     *
     * @return collection of protocol servers
     */
    protected abstract Collection<ProtocolServer> getServers();

    /**
     * Fire a channel event to all servers.
     *
     * @param event the channel event to fire
     */
    protected void fireChannelEvent(ChannelEvent event) {
        try {
            for (ProtocolServer server : getServers()) {
                server.getRemotingServer().fireChannelEvent(event);
            }
        } catch (Throwable e) {
            logger.warn(
                    TRANSPORT_FAILED_CLOSE_STREAM, "", "", "Failed to fire channel event during graceful shutdown.", e);
        }
    }
}
