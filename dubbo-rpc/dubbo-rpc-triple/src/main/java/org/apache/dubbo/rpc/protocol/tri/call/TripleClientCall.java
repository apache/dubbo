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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.RequestMetadata;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStream;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStreamFactory;
import org.apache.dubbo.rpc.protocol.tri.stream.StreamUtils;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleWriteQueue;

import java.util.Map;
import java.util.concurrent.Executor;

import io.netty.handler.codec.http2.Http2Exception.StreamException;

import static io.netty.handler.codec.http2.Http2Error.FLOW_CONTROL_ERROR;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_RESPONSE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_SERIALIZE_TRIPLE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_STREAM_LISTENER;

public class TripleClientCall implements ClientCall, ClientStream.Listener {

    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(TripleClientCall.class);
    private final AbstractConnectionClient connectionClient;
    private final Executor executor;
    private final FrameworkModel frameworkModel;
    private final TripleWriteQueue writeQueue;
    private RequestMetadata requestMetadata;
    private ClientStream stream;
    private ClientCall.Listener listener;
    private boolean canceled;
    private boolean headerSent;
    private boolean autoRequest = true;
    private int initialRequest = 1;
    private boolean done;
    private boolean streamingResponse;
    private StreamException streamException;

    public TripleClientCall(
            AbstractConnectionClient connectionClient,
            Executor executor,
            FrameworkModel frameworkModel,
            TripleWriteQueue writeQueue) {
        this.connectionClient = connectionClient;
        this.executor = executor;
        this.frameworkModel = frameworkModel;
        this.writeQueue = writeQueue;
    }

    @Override
    public boolean isReady() {
        if (canceled) {
            return false;
        }
        if (done) {
            return false;
        }
        if (stream == null) {
            return false;
        }
        return stream.isReady();
    }

    // stream listener start
    @Override
    public void onMessage(byte[] message, boolean isReturnTriException) {
        if (done) {
            LOGGER.warn(
                    PROTOCOL_STREAM_LISTENER,
                    "",
                    "",
                    "Received message from closed stream,connection=" + connectionClient + " service="
                            + requestMetadata.service + " method=" + requestMetadata.method.getMethodName());
            return;
        }
        try {
            Object unpacked = requestMetadata.packableMethod.parseResponse(message, isReturnTriException);
            listener.onMessage(unpacked, message.length);
        } catch (Throwable t) {
            TriRpcStatus status = TriRpcStatus.INTERNAL
                    .withDescription("Deserialize response failed")
                    .withCause(t);
            cancelByLocal(status.asException());
            listener.onClose(status, null, false);
            LOGGER.error(
                    PROTOCOL_FAILED_RESPONSE,
                    "",
                    "",
                    String.format(
                            "Failed to deserialize triple response, service=%s, method=%s,connection=%s",
                            requestMetadata.service, requestMetadata.service, requestMetadata.method.getMethodName()),
                    t);
        }
    }

    @Override
    public void onCancelByRemote(TriRpcStatus status) {
        if (canceled) {
            return;
        }
        canceled = true;
        if (requestMetadata.cancellationContext != null) {
            requestMetadata.cancellationContext.cancel(status.asException());
        }
        onComplete(status, null, null, false);
    }

    @Override
    public void onComplete(
            TriRpcStatus status,
            Map<String, Object> attachments,
            Map<CharSequence, String> excludeHeaders,
            boolean isReturnTriException) {
        if (done) {
            return;
        }
        done = true;
        try {
            listener.onClose(status, StreamUtils.toAttachments(attachments), isReturnTriException);
        } catch (Throwable t) {
            cancelByLocal(TriRpcStatus.INTERNAL
                    .withDescription("Close stream error")
                    .withCause(t)
                    .asException());
        }
        if (requestMetadata.cancellationContext != null) {
            requestMetadata.cancellationContext.cancel(null);
        }
    }

    /**
     * Called when the stream becomes ready for writing.
     * This method is invoked synchronously from the transport layer (AbstractTripleClientStream.onWritabilityChanged),
     * and it asynchronously dispatches the callback to the business executor to avoid blocking the Netty EventLoop.
     *
     * <p>The call chain is:
     * <pre>
     * Netty channelWritabilityChanged
     *   → AbstractTripleClientStream.onWritabilityChanged() [sync]
     *   → TripleClientCall.onReady() [this method, schedules async execution]
     *   → executor.execute(() → listener.onReady())
     *   → ObserverToClientCallListenerAdapter.onReady() [triggers onReadyHandler]
     * </pre>
     */
    @Override
    public void onReady() {
        if (listener == null) {
            return;
        }
        // ObserverToClientCallListenerAdapter.onReady() triggers the onReadyHandler
        executor.execute(() -> {
            try {
                listener.onReady();
            } catch (Throwable t) {
                LOGGER.warn(PROTOCOL_STREAM_LISTENER, "", "", "Error executing listener.onReady()", t);
            }
        });
    }

    @Override
    public void onClose() {
        if (done) {
            return;
        }
        onCancelByRemote(TriRpcStatus.CANCELLED);
    }

    @Override
    public void onStart() {
        if (streamingResponse) {
            request(initialRequest);
        } else {
            request(2);
        }
        listener.onStart(this);
    }

    @Override
    public void cancelByLocal(Throwable t) {
        if (canceled) {
            return;
        }
        // did not create stream
        if (!headerSent) {
            return;
        }
        canceled = true;
        if (stream == null) {
            return;
        }
        if (t instanceof StreamException && ((StreamException) t).error().equals(FLOW_CONTROL_ERROR)) {
            TriRpcStatus status = TriRpcStatus.CANCELLED
                    .withCause(t)
                    .withDescription("Due flowcontrol over pendingbytes, Cancelled by client");
            stream.cancelByLocal(status);
            streamException = (StreamException) t;
        } else {
            TriRpcStatus status = TriRpcStatus.CANCELLED.withCause(t).withDescription("Cancelled by client");
            stream.cancelByLocal(status);
        }
        TriRpcStatus status = TriRpcStatus.CANCELLED.withCause(t).withDescription("Cancelled by client");
        stream.cancelByLocal(status);
        if (requestMetadata.cancellationContext != null) {
            requestMetadata.cancellationContext.cancel(t);
        }
    }

    @Override
    public void request(int messageNumber) {
        stream.request(messageNumber);
    }

    @Override
    public void sendMessage(Object message) {
        if (canceled && null != streamException) {
            throw new IllegalStateException("Due flowcontrol over pendingbytes, Call already canceled");
        } else if (canceled) {
            throw new IllegalStateException("Call already canceled");
        }
        if (!headerSent) {
            headerSent = true;
            stream.sendHeader(requestMetadata.toHeaders());
        }
        final byte[] data;
        try {
            data = requestMetadata.packableMethod.packRequest(message);
            int compressed = Identity.MESSAGE_ENCODING.equals(requestMetadata.compressor.getMessageEncoding()) ? 0 : 1;
            final byte[] compress = requestMetadata.compressor.compress(data);
            stream.sendMessage(compress, compressed).addListener(f -> {
                if (!f.isSuccess()) {
                    cancelByLocal(f.cause());
                }
            });
        } catch (Throwable t) {
            LOGGER.error(
                    PROTOCOL_FAILED_SERIALIZE_TRIPLE,
                    "",
                    "",
                    String.format(
                            "Serialize triple request failed, service=%s method=%s",
                            requestMetadata.service, requestMetadata.method.getMethodName()),
                    t);
            cancelByLocal(t);
            listener.onClose(
                    TriRpcStatus.INTERNAL
                            .withDescription("Serialize request failed")
                            .withCause(t),
                    null,
                    false);
        }
    }
    // stream listener end

    @Override
    public void halfClose() {
        if (!headerSent) {
            return;
        }
        if (canceled) {
            return;
        }
        stream.halfClose().addListener(f -> {
            if (!f.isSuccess()) {
                cancelByLocal(new IllegalStateException("Half close failed", f.cause()));
            }
        });
    }

    @Override
    public void setCompression(String compression) {
        requestMetadata.compressor = Compressor.getCompressor(frameworkModel, compression);
    }

    @Override
    public void start(RequestMetadata metadata, ClientCall.Listener responseListener) {
        // Set listener BEFORE creating stream, so onReady() can access it
        this.requestMetadata = metadata;
        this.listener = responseListener;
        this.streamingResponse = responseListener.streamingResponse();

        ClientStream stream;
        for (ClientStreamFactory factory : frameworkModel.getActivateExtensions(ClientStreamFactory.class)) {
            stream = factory.createClientStream(connectionClient, frameworkModel, executor, this, writeQueue);
            if (stream != null) {
                this.stream = stream;
                return;
            }
        }
        throw new IllegalStateException("No available ClientStreamFactory");
    }

    @Override
    public boolean isAutoRequest() {
        return autoRequest;
    }

    @Override
    public void setAutoRequestWithInitial(int initialRequest) {
        setAutoRequest(false);
        this.initialRequest = initialRequest;
    }

    @Override
    public void setAutoRequest(boolean autoRequest) {
        this.autoRequest = autoRequest;
    }
}
