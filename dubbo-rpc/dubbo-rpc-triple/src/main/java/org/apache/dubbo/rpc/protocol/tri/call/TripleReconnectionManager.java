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
package org.apache.dubbo.rpc.protocol.tri.call;

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.rpc.protocol.tri.command.CreateStreamQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.stream.ReconnectionManager;
import org.apache.dubbo.rpc.protocol.tri.stream.TripleStreamChannelFuture;
import org.apache.dubbo.rpc.protocol.tri.transport.H2TransportListener;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleCommandOutBoundHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleHttp2ClientResponseHandler;

import java.util.concurrent.CompletableFuture;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;

/**
 * Implementation of ReconnectionManager that uses AbstractConnectionClient for reconnection.
 * This provides real reconnection capability by leveraging existing connection management.
 */
public class TripleReconnectionManager implements ReconnectionManager {

    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(TripleReconnectionManager.class);

    private final AbstractConnectionClient connectionClient;
    private final TripleClientCall clientCall;
    private final H2TransportListener transportListener;

    public TripleReconnectionManager(
            AbstractConnectionClient connectionClient,
            TripleClientCall clientCall,
            H2TransportListener transportListener) {
        this.connectionClient = connectionClient;
        this.clientCall = clientCall;
        this.transportListener = transportListener;
    }

    @Override
    public CompletableFuture<Boolean> attemptReconnection() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("Attempting reconnection using connection client: {}", connectionClient);

                // Force get a new channel, which will trigger reconnection if needed
                Object channel = connectionClient.getChannel(true);

                boolean success = (channel != null) && isConnectionActive();

                if (success) {
                    LOGGER.info("Reconnection successful for connection client: {}", connectionClient);
                } else {
                    LOGGER.warn(
                            LoggerCodeConstants.INTERNAL_ERROR,
                            "",
                            "",
                            "Reconnection failed for connection client: " + connectionClient);
                }

                return success;
            } catch (Exception e) {
                LOGGER.error(LoggerCodeConstants.INTERNAL_ERROR, "", "", "Error during reconnection attempt", e);
                return false;
            }
        });
    }

    @Override
    public boolean isConnectionActive() {
        try {
            return connectionClient.isConnected();
        } catch (Exception e) {
            LOGGER.debug("Error checking connection status", e);
            return false;
        }
    }

    @Override
    public Object getNewStreamChannel() {
        try {
            // Get the active channel, this should be the new channel after reconnection
            return connectionClient.getChannel(true);
        } catch (Exception e) {
            LOGGER.error(LoggerCodeConstants.INTERNAL_ERROR, "", "", "Error getting new stream channel", e);
            return null;
        }
    }

    @Override
    public TripleStreamChannelFuture createStreamFuture() {
        try {
            LOGGER.info("Creating new TripleStreamChannelFuture for reconnection");

            // Force get a new channel, which will trigger reconnection if needed
            Object channel = connectionClient.getChannel(true);

            if (channel != null && isConnectionActive()) {
                // Create a new Http2StreamChannel using the same mechanism as initial connection
                Channel parentChannel = (Channel) channel;

                // Create Http2StreamChannelBootstrap to open a new stream channel
                Http2StreamChannelBootstrap bootstrap = new Http2StreamChannelBootstrap(parentChannel);

                // Set up the handler to configure the pipeline when the stream channel is created
                bootstrap.handler(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void handlerAdded(ChannelHandlerContext ctx) {
                        ctx.channel()
                                .pipeline()
                                .addLast(new TripleCommandOutBoundHandler())
                                .addLast(new TripleHttp2ClientResponseHandler(transportListener));
                    }
                });

                // Create the future that will be completed when the stream channel is ready
                TripleStreamChannelFuture streamChannelFuture = new TripleStreamChannelFuture(parentChannel);

                // Use CreateStreamQueueCommand to create the actual Http2StreamChannel
                CreateStreamQueueCommand createCommand =
                        CreateStreamQueueCommand.create(bootstrap, streamChannelFuture);

                // IMPROVED: Execute the creation command and add better error handling
                if (parentChannel.eventLoop().inEventLoop()) {
                    // If we're already in the event loop, execute directly
                    createCommand.run(parentChannel);
                } else {
                    // Execute in the channel's event loop to ensure thread safety
                    parentChannel.eventLoop().execute(() -> createCommand.run(parentChannel));
                }

                // Return the future - caller is now responsible for waiting for completion
                LOGGER.info("Started async creation of new Http2StreamChannel for reconnection, "
                        + "future will be completed when stream is ready");
                return streamChannelFuture;

            } else {
                LOGGER.warn(
                        LoggerCodeConstants.INTERNAL_ERROR,
                        "",
                        "",
                        "Failed to create TripleStreamChannelFuture - no active channel available");
                return null;
            }
        } catch (Exception e) {
            LOGGER.error(
                    LoggerCodeConstants.INTERNAL_ERROR,
                    "",
                    "",
                    "Error creating new TripleStreamChannelFuture for reconnection",
                    e);
            return null;
        }
    }
}
