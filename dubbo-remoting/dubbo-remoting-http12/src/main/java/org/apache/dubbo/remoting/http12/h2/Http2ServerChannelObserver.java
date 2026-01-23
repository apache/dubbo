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
package org.apache.dubbo.remoting.http12.h2;

import org.apache.dubbo.common.stream.ServerCallStreamObserver;
import org.apache.dubbo.remoting.http12.AbstractServerHttpChannelObserver;
import org.apache.dubbo.remoting.http12.ErrorCodeHolder;
import org.apache.dubbo.remoting.http12.FlowControlStreamObserver;
import org.apache.dubbo.remoting.http12.HttpConstants;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.HttpOutputMessage;
import org.apache.dubbo.remoting.http12.message.StreamingDecoder;
import org.apache.dubbo.remoting.http12.netty4.NettyHttpHeaders;
import org.apache.dubbo.rpc.CancellationContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import io.netty.handler.codec.http2.DefaultHttp2Headers;

/**
 * HTTP/2 server-side stream observer with flow control and backpressure support.
 * <p>
 * Backpressure is implemented using a byte-counting strategy. Outbound messages are
 * tracked in {@code numSentBytesQueued}, which represents the approximate number of
 * bytes that have been queued but not yet acknowledged as sent.
 * <p>
 * The {@code ON_READY_THRESHOLD} (32KB) defines when this observer is considered "ready":
 * <ul>
 *     <li>{@link #isReady()} returns {@code true} when {@code numSentBytesQueued < ON_READY_THRESHOLD}</li>
 *     <li>When the queued byte count drops from at or above the threshold to below it,
 *         the registered {@code onReadyHandler} is invoked to signal that more data can be sent</li>
 * </ul>
 */
public class Http2ServerChannelObserver extends AbstractServerHttpChannelObserver<H2StreamChannel>
        implements FlowControlStreamObserver<Object>,
                Http2CancelableStreamObserver<Object>,
                ServerCallStreamObserver<Object> {

    /**
     * Number of bytes currently queued, waiting to be sent.
     * When this falls below ON_READY_THRESHOLD, onReady will be triggered.
     */
    private final AtomicLong numSentBytesQueued = new AtomicLong(0);

    /**
     * The threshold below which isReady() returns true (32KB).
     */
    protected static final long ON_READY_THRESHOLD = 32 * 1024;

    private CancellationContext cancellationContext;

    private StreamingDecoder streamingDecoder;

    private boolean autoRequestN = true;

    private volatile Runnable onReadyHandler;

    private volatile Executor executor = Runnable::run;

    public Http2ServerChannelObserver(H2StreamChannel h2StreamChannel) {
        super(h2StreamChannel);
    }

    /**
     * Sets the executor for async dispatch of callbacks.
     */
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    /**
     * Returns whether the stream is ready for writing.
     * <p>
     * Ready state is determined by byte counting: returns {@code true} when the number
     * of queued bytes is below the threshold (32KB). If {@code false}, the caller should
     * avoid calling {@code onNext} to prevent excessive buffering.
     *
     * @return {@code true} if the stream is ready for more data, {@code false} otherwise
     */
    public boolean isReady() {
        H2StreamChannel channel = getHttpChannel();
        if (channel == null) {
            return false;
        }
        return numSentBytesQueued.get() < ON_READY_THRESHOLD;
    }

    /**
     * Sets a callback to be invoked when the stream becomes ready for writing.
     */
    public void setOnReadyHandler(Runnable onReadyHandler) {
        this.onReadyHandler = onReadyHandler;
    }

    /**
     * Called by the transport layer when the underlying channel's writability changes.
     * <p>
     * This serves as an additional trigger point for notifying the {@code onReadyHandler}
     * when the channel becomes writable again. The actual ready state is still determined
     * by the byte counting mechanism in {@link #isReady()}.
     */
    public void onWritabilityChanged() {
        if (isReady()) {
            notifyOnReady();
        }
    }

    public void setStreamingDecoder(StreamingDecoder streamingDecoder) {
        this.streamingDecoder = streamingDecoder;
    }

    /**
     * Override to add byte counting for backpressure support.
     */
    @Override
    protected CompletableFuture<Void> sendMessage(HttpOutputMessage message) throws Throwable {
        if (message == null) {
            return CompletableFuture.completedFuture(null);
        }

        int messageSize = message.messageSize();
        onSendingBytes(messageSize);

        CompletableFuture<Void> future = super.sendMessage(message);

        future.whenComplete((v, t) -> {
            if (t == null) {
                onSentBytes(messageSize);
            } else {
                rollbackSendingBytes(messageSize);
            }
        });

        return future;
    }

    /**
     * Called before bytes are sent to track pending bytes.
     */
    protected void onSendingBytes(int numBytes) {
        numSentBytesQueued.addAndGet(numBytes);
    }

    /**
     * Called when sending fails to rollback the pending bytes count.
     */
    protected void rollbackSendingBytes(int numBytes) {
        numSentBytesQueued.addAndGet(-numBytes);
    }

    /**
     * Called when bytes have been successfully sent to the remote endpoint.
     */
    protected void onSentBytes(int numBytes) {
        long oldValue = numSentBytesQueued.getAndAdd(-numBytes);
        long newValue = oldValue - numBytes;
        // Trigger onReady when transitioning from "not ready" to "ready"
        if (oldValue >= ON_READY_THRESHOLD && newValue < ON_READY_THRESHOLD) {
            notifyOnReady();
        }
    }

    /**
     * Returns the number of bytes currently queued for sending.
     * Visible for testing.
     */
    protected long getNumSentBytesQueued() {
        return numSentBytesQueued.get();
    }

    /**
     * Notify the onReadyHandler that the stream is ready for writing.
     */
    protected void notifyOnReady() {
        Runnable handler = this.onReadyHandler;
        if (handler == null) {
            return;
        }
        executor.execute(handler);
    }

    @Override
    protected HttpMetadata encodeHttpMetadata(boolean endStream) {
        HttpHeaders headers = new NettyHttpHeaders<>(new DefaultHttp2Headers(false, 8));
        headers.set(HttpHeaderNames.TE.getKey(), HttpConstants.TRAILERS);
        return new Http2MetadataFrame(headers, endStream);
    }

    @Override
    protected HttpMetadata encodeTrailers(Throwable throwable) {
        return new Http2MetadataFrame(new NettyHttpHeaders<>(new DefaultHttp2Headers(false, 4)), true);
    }

    @Override
    public void setCancellationContext(CancellationContext cancellationContext) {
        this.cancellationContext = cancellationContext;
    }

    @Override
    public CancellationContext getCancellationContext() {
        return cancellationContext;
    }

    @Override
    public void cancel(Throwable throwable) {
        if (throwable instanceof CancelStreamException) {
            if (((CancelStreamException) throwable).isCancelByRemote()) {
                closed();
            }
        }
        if (cancellationContext != null) {
            cancellationContext.cancel(throwable);
        }
        long errorCode = 0;
        if (throwable instanceof ErrorCodeHolder) {
            errorCode = ((ErrorCodeHolder) throwable).getErrorCode();
        }
        getHttpChannel().writeResetFrame(errorCode);
    }

    @Override
    public void request(int count) {
        streamingDecoder.request(count);
    }

    @Override
    public void setCompression(String compression) {
        // not supported yet
    }

    @Override
    public void disableAutoFlowControl() {
        autoRequestN = false;
    }

    @Override
    public boolean isAutoRequestN() {
        return autoRequestN;
    }

    @Override
    public void close() {
        super.close();
        streamingDecoder.onStreamClosed();
    }
}
