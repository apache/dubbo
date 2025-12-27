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
package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.http12.h2.H2FlowController;

import io.netty.handler.codec.http2.DefaultHttp2LocalFlowController;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Stream;

public class TriHttp2LocalFlowController extends DefaultHttp2LocalFlowController implements H2FlowController {
    private static final Logger LOGGER = LoggerFactory.getLogger(TriHttp2LocalFlowController.class);
    private final Http2Connection connection;
    private final Http2Connection.PropertyKey autoFlowControlKey;
    private final Http2Connection.PropertyKey pendingBytesKey;

    public TriHttp2LocalFlowController(Http2Connection connection) {
        super(connection);
        this.connection = connection;
        this.autoFlowControlKey = connection.newKey();
        this.pendingBytesKey = connection.newKey();
    }

    /**
     * Disable automatic flow control for a specific stream.
     * When disabled, WINDOW_UPDATE frames will not be sent automatically.
     */
    public void disableAutoFlowControlForStream(Http2Stream stream) {
        stream.setProperty(autoFlowControlKey, Boolean.FALSE);
        // Initialize pending bytes counter
        stream.setProperty(pendingBytesKey, 0);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Disabled auto flow control for stream {}", stream.id());
        }
    }

    /**
     * Enable automatic flow control for a specific stream (default behavior).
     */
    public void enableAutoFlowControlForStream(Http2Stream stream) {
        stream.setProperty(autoFlowControlKey, Boolean.TRUE);
        // Flush any pending bytes
        Integer pendingBytes = stream.getProperty(pendingBytesKey);
        if (pendingBytes != null && pendingBytes > 0) {
            try {
                super.consumeBytes(stream, pendingBytes);
            } catch (Http2Exception e) {
                LOGGER.warn("Failed to flush pending bytes for stream " + stream.id(), e);
            }
            stream.setProperty(pendingBytesKey, 0);
        }
    }

    /**
     * Check if automatic flow control is enabled for a stream.
     */
    public boolean isAutoFlowControlEnabled(Http2Stream stream) {
        Boolean autoFlowControl = stream.getProperty(autoFlowControlKey);
        return autoFlowControl == null || autoFlowControl;
    }

    /**
     * Get pending bytes for a stream (bytes consumed but not yet sent as WINDOW_UPDATE).
     */
    private int getPendingBytes(Http2Stream stream) {
        Integer pending = stream.getProperty(pendingBytesKey);
        return pending != null ? pending : 0;
    }

    /**
     * Add pending bytes for a stream.
     */
    private void addPendingBytes(Http2Stream stream, int bytes) {
        int current = getPendingBytes(stream);
        stream.setProperty(pendingBytesKey, current + bytes);
    }

    @Override
    public boolean consumeBytes(Http2Stream stream, int numBytes) throws Http2Exception {
        if (!isAutoFlowControlEnabled(stream)) {
            // When auto flow control is disabled:
            // - Track the consumed bytes but don't send WINDOW_UPDATE
            // - Application will call consumeBytes(streamId, numBytes) to send WINDOW_UPDATE
            addPendingBytes(stream, numBytes);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "Stream {} auto flow control disabled, accumulated {} bytes, total pending: {}",
                        stream.id(),
                        numBytes,
                        getPendingBytes(stream));
            }
            return false;
        }
        // Default behavior: send WINDOW_UPDATE when appropriate
        return super.consumeBytes(stream, numBytes);
    }

    @Override
    public void consumeBytes(int streamId, int numBytes) {
        try {
            Http2Stream stream = connection.stream(streamId);
            if (stream == null) {
                return;
            }

            // Get current pending bytes
            int pendingBytes = getPendingBytes(stream);

            // If auto flow control is enabled and no pending bytes,
            // Netty is already handling WINDOW_UPDATE automatically
            if (isAutoFlowControlEnabled(stream) && pendingBytes == 0) {
                return;
            }

            // No pending bytes to send
            if (pendingBytes <= 0) {
                return;
            }

            // Send all pending bytes as WINDOW_UPDATE
            // Note: numBytes parameter is the message count from request(n), not byte count
            // We always send all accumulated bytes to ensure the sender has enough window
            stream.setProperty(pendingBytesKey, 0);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Stream {} sending WINDOW_UPDATE for {} bytes", streamId, pendingBytes);
            }
            super.consumeBytes(stream, pendingBytes);
        } catch (Http2Exception e) {
            LOGGER.warn("Failed to consume bytes for stream " + streamId, e);
        }
    }

    @Override
    public void disableAutoFlowControl(int streamId) {
        Http2Stream stream = connection.stream(streamId);
        if (stream != null) {
            disableAutoFlowControlForStream(stream);
        }
    }

    @Override
    public void enableAutoFlowControl(int streamId) {
        Http2Stream stream = connection.stream(streamId);
        if (stream != null) {
            enableAutoFlowControlForStream(stream);
        }
    }

    /**
     * Get the HTTP/2 connection.
     */
    public Http2Connection connection() {
        return connection;
    }
}
