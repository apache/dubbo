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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.remoting.api.connection.ConnectionStateProvider;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.RequestMetadata;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStream;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStreamFactory;
import org.apache.dubbo.rpc.protocol.tri.stream.StreamUtils;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleWriteQueue;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private volatile ClientStream stream;
    private final AtomicBoolean transportCancelled = new AtomicBoolean();
    private final AtomicBoolean connectionReleased = new AtomicBoolean();
    private volatile boolean connectionRetained;
    private ClientCall.Listener listener;
    private volatile boolean canceled;
    private Throwable cancellationCause;
    private boolean headerSent;
    private boolean autoRequest = true;
    private int initialRequest = 1;
    private volatile boolean done;
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
    public void onMessage(InputStream message, int messageLength, boolean isReturnTriException) {
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
            // Use the new InputStream-based parseResponse method
            Object unpacked = requestMetadata.packableMethod.parseResponse(message, isReturnTriException);
            listener.onMessage(unpacked, messageLength);
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
        synchronized (this) {
            if (done) {
                return;
            }
            done = true;
        }
        finishStreamResources(status.isOk() ? null : status.asException());
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
        // ObserverToClientCallListenerAdapter.onReady() triggers the onReadyHandler.
        // Note: We do NOT check isReady() here because of the async dispatch model.
        // The handler is always called (following gRPC's "spurious notifications" semantics),
        // and it should check isReady() internally via while(isReady()) { send(); }.
        // Subsequent channelWritabilityChanged events will trigger onReady() again if needed.
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
        synchronized (this) {
            if (canceled) {
                return;
            }
            cancellationCause = t;
            canceled = true;
        }
        // A stream can exist before its first request header, especially for client streaming.
        // Cancellation must also close that stream and prevent a later send from reviving the call.
        cancelAllocatedStream(t);
    }

    private void cancelAllocatedStream(Throwable t) {
        ClientStream allocated = stream;
        if (allocated == null) {
            finishStreamResources(t);
            return;
        }
        if (!transportCancelled.compareAndSet(false, true)) {
            return;
        }
        try {
            if (t instanceof StreamException && ((StreamException) t).error().equals(FLOW_CONTROL_ERROR)) {
                TriRpcStatus status = TriRpcStatus.CANCELLED
                        .withCause(t)
                        .withDescription("Due flowcontrol over pendingbytes, Cancelled by client");
                allocated.cancelByLocal(status);
                streamException = (StreamException) t;
            } else {
                TriRpcStatus status = TriRpcStatus.CANCELLED.withCause(t).withDescription("Cancelled by client");
                allocated.cancelByLocal(status);
            }
            if (requestMetadata.cancellationContext != null) {
                requestMetadata.cancellationContext.cancel(t);
            }
        } finally {
            finishStreamResources(t);
        }
    }

    private void finishStreamResources(Throwable cause) {
        try {
            if (requestMetadata != null && requestMetadata.streamCancellation != null) {
                requestMetadata.streamCancellation.cancel(cause);
            }
        } finally {
            if (connectionRetained && connectionReleased.compareAndSet(false, true)) {
                connectionClient.release();
            }
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
            byte[] retained = requestMetadata.retryAttempt == null
                    ? null
                    : requestMetadata.retryAttempt.getState().getRequest();
            data = retained == null ? requestMetadata.packableMethod.packRequest(message) : retained;
            if (retained == null && requestMetadata.retryAttempt != null) {
                requestMetadata.retryAttempt.getState().remember(data);
            }
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

        // Native xDS address withdrawal retires an Invoker, not its already dispatched calls.
        // Each call keeps the physical connection alive until its own terminal/cancel event.
        URL connectionUrl = connectionClient.getUrl();
        synchronized (this) {
            if (!canceled
                    && !done
                    && connectionUrl != null
                    && Boolean.TRUE.equals(connectionUrl.getAttribute(ConnectionStateProvider.BACKOFF_ATTRIBUTE))) {
                if (!connectionClient.retain()) {
                    throw TriRpcStatus.UNAVAILABLE
                            .withDescription("Native connection has been retired")
                            .asException();
                }
                connectionRetained = true;
            }
        }
        try {
            ClientStream stream;
            for (ClientStreamFactory factory : frameworkModel.getActivateExtensions(ClientStreamFactory.class)) {
                stream = factory.createClientStream(connectionClient, frameworkModel, executor, this, writeQueue);
                if (stream != null) {
                    // Set this.stream BEFORE initStream() to avoid race condition:
                    // initStream() triggers onReady callback asynchronously, which may execute
                    // in another thread before this.stream is set if we set it after initStream().
                    // This would cause isReady() to return false because it checks stream == null.
                    this.stream = stream;
                    stream.initStream();
                    if (metadata.streamCancellation != null) {
                        metadata.streamCancellation.addListener(ignored -> {
                            Throwable cause = metadata.streamCancellation.getCancellationCause();
                            if (cause != null && !done) {
                                cancelByLocal(cause);
                                TriRpcStatus status = TriRpcStatus.getStatus(cause);
                                TriRpcStatus terminal = status.code == TriRpcStatus.Code.UNKNOWN
                                        ? TriRpcStatus.CANCELLED.withCause(cause)
                                        : status;
                                executor.execute(() -> onComplete(terminal, null, null, false));
                            }
                        });
                    }
                    if (canceled && !done) {
                        cancelAllocatedStream(cancellationCause);
                    }
                    return;
                }
            }
            throw new IllegalStateException("No available ClientStreamFactory");
        } catch (RuntimeException | Error error) {
            cancelByLocal(error);
            throw error;
        }
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
