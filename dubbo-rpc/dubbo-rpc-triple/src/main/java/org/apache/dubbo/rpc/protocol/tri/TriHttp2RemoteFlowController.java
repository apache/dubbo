/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionAdapter;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2RemoteFlowController;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.handler.codec.http2.Http2StreamVisitor;
import io.netty.handler.codec.http2.StreamByteDistributor;
import io.netty.handler.codec.http2.WeightedFairQueueByteDistributor;
import io.netty.util.internal.UnstableApi;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;

import static io.netty.handler.codec.http2.Http2CodecUtil.DEFAULT_WINDOW_SIZE;
import static io.netty.handler.codec.http2.Http2CodecUtil.MAX_WEIGHT;
import static io.netty.handler.codec.http2.Http2CodecUtil.MIN_WEIGHT;
import static io.netty.handler.codec.http2.Http2Error.FLOW_CONTROL_ERROR;
import static io.netty.handler.codec.http2.Http2Error.INTERNAL_ERROR;
import static io.netty.handler.codec.http2.Http2Error.STREAM_CLOSED;
import static io.netty.handler.codec.http2.Http2Exception.streamError;
import static io.netty.handler.codec.http2.Http2Stream.State.HALF_CLOSED_LOCAL;
import static io.netty.util.internal.ObjectUtil.checkNotNull;
import static io.netty.util.internal.ObjectUtil.checkPositiveOrZero;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.apache.dubbo.rpc.Constants.H2_SETTINGS_INITIAL_WINDOW_SIZE_KEY;

/**
 * This design is learning from {@see io.netty.handler.codec.http2.DefaultHttp2RemoteFlowController} which is in Netty.
 */
@UnstableApi
public class TriHttp2RemoteFlowController implements Http2RemoteFlowController {
    private static final InternalLogger logger =
        InternalLoggerFactory.getInstance(TriHttp2RemoteFlowController.class);
    private static final int MIN_WRITABLE_CHUNK = 32 * 1024;
    private final Http2Connection connection;
    private final Http2Connection.PropertyKey stateKey;
    private final StreamByteDistributor streamByteDistributor;
    private final FlowState connectionState;
    private final Configuration config;
    private int initialWindowSize;
    private WritabilityMonitor monitor;
    private ChannelHandlerContext ctx;

    public TriHttp2RemoteFlowController(Http2Connection connection, ApplicationModel applicationModel) {
        this(connection, (Listener) null, applicationModel);
    }

    public TriHttp2RemoteFlowController(Http2Connection connection,
                                        StreamByteDistributor streamByteDistributor,
                                        ApplicationModel applicationModel) {
        this(connection, streamByteDistributor, null, applicationModel);
    }

    public TriHttp2RemoteFlowController(Http2Connection connection, final Listener listener, ApplicationModel applicationModel) {
        this(connection, new WeightedFairQueueByteDistributor(connection), listener, applicationModel);
    }

    public TriHttp2RemoteFlowController(Http2Connection connection,
                                        StreamByteDistributor streamByteDistributor,
                                        final Listener listener,
                                        ApplicationModel applicationModel) {
        this.connection = checkNotNull(connection, "connection");
        this.streamByteDistributor = checkNotNull(streamByteDistributor, "streamWriteDistributor");
        this.config = ConfigurationUtils.getGlobalConfiguration(applicationModel);
        this.initialWindowSize = config.getInt(H2_SETTINGS_INITIAL_WINDOW_SIZE_KEY, DEFAULT_WINDOW_SIZE);

        // Add a flow state for the connection.
        stateKey = connection.newKey();
        connectionState = new FlowState(connection.connectionStream());
        connection.connectionStream().setProperty(stateKey, connectionState);

        // Monitor may depend upon connectionState, and so initialize after connectionState
        listener(listener);
        monitor.windowSize(connectionState, initialWindowSize);

        // Register for notification of new streams.
        connection.addListener(new Http2ConnectionAdapter() {
            @Override
            public void onStreamAdded(Http2Stream stream) {
                // If the stream state is not open then the stream is not yet eligible for flow controlled frames and
                // only requires the ReducedFlowState. Otherwise the full amount of memory is required.
                stream.setProperty(stateKey, new FlowState(stream));
            }

            @Override
            public void onStreamActive(Http2Stream stream) {
                // If the object was previously created, but later activated then we have to ensure the proper
                // initialWindowSize is used.
                monitor.windowSize(state(stream), initialWindowSize);
            }

            @Override
            public void onStreamClosed(Http2Stream stream) {
                // Any pending frames can never be written, cancel and
                // write errors for any pending frames.
                state(stream).cancel(STREAM_CLOSED, null);
            }

            @Override
            public void onStreamHalfClosed(Http2Stream stream) {
                if (HALF_CLOSED_LOCAL == stream.state()) {
                    /*
                     * When this method is called there should not be any
                     * pending frames left if the API is used correctly. However,
                     * it is possible that a erroneous application can sneak
                     * in a frame even after having already written a frame with the
                     * END_STREAM flag set, as the stream state might not transition
                     * immediately to HALF_CLOSED_LOCAL / CLOSED due to flow control
                     * delaying the write.
                     *
                     * This is to cancel any such illegal writes.
                     */
                    state(stream).cancel(STREAM_CLOSED, null);
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     * <p>
     * Any queued {@link FlowControlled} objects will be sent.
     */
    @Override
    public void channelHandlerContext(ChannelHandlerContext ctx) throws Http2Exception {
        this.ctx = checkNotNull(ctx, "ctx");

        // Writing the pending bytes will not check writability change and instead a writability change notification
        // to be provided by an explicit call.
        channelWritabilityChanged();

        // Don't worry about cleaning up queued frames here if ctx is null. It is expected that all streams will be
        // closed and the queue cleanup will occur when the stream state transitions occur.

        // If any frames have been queued up, we should send them now that we have a channel context.
        if (isChannelWritable()) {
            writePendingBytes();
        }
    }

    @Override
    public ChannelHandlerContext channelHandlerContext() {
        return ctx;
    }

    @Override
    public void initialWindowSize(int newWindowSize) throws Http2Exception {
        assert ctx == null || ctx.executor().inEventLoop();
        monitor.initialWindowSize(newWindowSize);
    }

    @Override
    public int initialWindowSize() {
        return initialWindowSize;
    }

    @Override
    public int windowSize(Http2Stream stream) {
        return state(stream).windowSize();
    }

    @Override
    public boolean isWritable(Http2Stream stream) {
        return monitor.isWritable(state(stream));
    }

    @Override
    public void channelWritabilityChanged() throws Http2Exception {
        monitor.channelWritabilityChange();
    }

    @Override
    public void updateDependencyTree(int childStreamId, int parentStreamId, short weight, boolean exclusive) {
        // It is assumed there are all validated at a higher level. For example in the Http2FrameReader.
        assert weight >= MIN_WEIGHT && weight <= MAX_WEIGHT : "Invalid weight";
        assert childStreamId != parentStreamId : "A stream cannot depend on itself";
        assert childStreamId > 0 && parentStreamId >= 0 : "childStreamId must be > 0. parentStreamId must be >= 0.";

        streamByteDistributor.updateDependencyTree(childStreamId, parentStreamId, weight, exclusive);
    }

    private boolean isChannelWritable() {
        return ctx != null && isChannelWritable0();
    }

    private boolean isChannelWritable0() {
        return ctx.channel().isWritable();
    }

    @Override
    public void listener(Listener listener) {
        monitor = listener == null ? new WritabilityMonitor() : new ListenerWritabilityMonitor(listener);
    }

    @Override
    public void incrementWindowSize(Http2Stream stream, int delta) throws Http2Exception {
        assert ctx == null || ctx.executor().inEventLoop();
        monitor.incrementWindowSize(state(stream), delta);
    }

    @Override
    public void addFlowControlled(Http2Stream stream, FlowControlled frame) {
        // The context can be null assuming the frame will be queued and send later when the context is set.
        assert ctx == null || ctx.executor().inEventLoop();
        checkNotNull(frame, "frame");
        try {
            monitor.enqueueFrame(state(stream), frame);
        } catch (Throwable t) {
            frame.error(ctx, t);
        }
    }

    @Override
    public boolean hasFlowControlled(Http2Stream stream) {
        return state(stream).hasFrame();
    }

    private FlowState state(Http2Stream stream) {
        return (FlowState) stream.getProperty(stateKey);
    }

    /**
     * Returns the flow control window for the entire connection.
     */
    private int connectionWindowSize() {
        return connectionState.windowSize();
    }

    private int minUsableChannelBytes() {
        // The current allocation algorithm values "fairness" and doesn't give any consideration to "goodput". It
        // is possible that 1 byte will be allocated to many streams. In an effort to try to make "goodput"
        // reasonable with the current allocation algorithm we have this "cheap" check up front to ensure there is
        // an "adequate" amount of connection window before allocation is attempted. This is not foolproof as if the
        // number of streams is >= this minimal number then we may still have the issue, but the idea is to narrow the
        // circumstances in which this can happen without rewriting the allocation algorithm.
        return max(ctx.channel().config().getWriteBufferLowWaterMark(), MIN_WRITABLE_CHUNK);
    }

    private int maxUsableChannelBytes() {
        // If the channel isWritable, allow at least minUsableChannelBytes.
        int channelWritableBytes = (int) min(Integer.MAX_VALUE, ctx.channel().bytesBeforeUnwritable());
        int usableBytes = channelWritableBytes > 0 ? max(channelWritableBytes, minUsableChannelBytes()) : 0;

        // Clip the usable bytes by the connection window.
        return min(connectionState.windowSize(), usableBytes);
    }

    /**
     * The amount of bytes that can be supported by underlying {@link io.netty.channel.Channel} without
     * queuing "too-much".
     */
    private int writableBytes() {
        return min(connectionWindowSize(), maxUsableChannelBytes());
    }

    @Override
    public void writePendingBytes() throws Http2Exception {
        monitor.writePendingBytes();
    }

    /**
     * The remote flow control state for a single stream.
     */
    private final class FlowState implements StreamByteDistributor.StreamState {
        private final Http2Stream stream;
        private final Deque<FlowControlled> pendingWriteQueue;
        private int window;
        private long pendingBytes;
        private boolean markedWritable;

        /**
         * Set to true while a frame is being written, false otherwise.
         */
        private boolean writing;
        /**
         * Set to true if cancel() was called.
         */
        private boolean cancelled;

        FlowState(Http2Stream stream) {
            this.stream = stream;
            pendingWriteQueue = new ArrayDeque<FlowControlled>(2);
        }

        /**
         * Determine if the stream associated with this object is writable.
         * @return {@code true} if the stream associated with this object is writable.
         */
        boolean isWritable() {
            return windowSize() > pendingBytes() && !cancelled;
        }

        /**
         * The stream this state is associated with.
         */
        @Override
        public Http2Stream stream() {
            return stream;
        }

        /**
         * Returns the parameter from the last call to {@link #markedWritability(boolean)}.
         */
        boolean markedWritability() {
            return markedWritable;
        }

        /**
         * Save the state of writability.
         */
        void markedWritability(boolean isWritable) {
            this.markedWritable = isWritable;
        }

        @Override
        public int windowSize() {
            return window;
        }

        /**
         * Reset the window size for this stream.
         */
        void windowSize(int initialWindowSize) {
            window = initialWindowSize;
        }

        /**
         * Write the allocated bytes for this stream.
         * @return the number of bytes written for a stream or {@code -1} if no write occurred.
         */
        int writeAllocatedBytes(int allocated) {
            final int initialAllocated = allocated;
            int writtenBytes;
            // In case an exception is thrown we want to remember it and pass it to cancel(Throwable).
            Throwable cause = null;
            FlowControlled frame;
            try {
                assert !writing;
                writing = true;

                // Write the remainder of frames that we are allowed to
                boolean writeOccurred = false;
                while (!cancelled && (frame = peek()) != null) {
                    int maxBytes = min(allocated, writableWindow());
                    if (maxBytes <= 0 && frame.size() > 0) {
                        // The frame still has data, but the amount of allocated bytes has been exhausted.
                        // Don't write needless empty frames.
                        break;
                    }
                    writeOccurred = true;
                    int initialFrameSize = frame.size();
                    try {
                        frame.write(ctx, max(0, maxBytes));
                        if (frame.size() == 0) {
                            // This frame has been fully written, remove this frame and notify it.
                            // Since we remove this frame first, we're guaranteed that its error
                            // method will not be called when we call cancel.
                            pendingWriteQueue.remove();
                            frame.writeComplete();
                        }
                    } finally {
                        // Decrement allocated by how much was actually written.
                        allocated -= initialFrameSize - frame.size();
                    }
                }

                if (!writeOccurred) {
                    // Either there was no frame, or the amount of allocated bytes has been exhausted.
                    return -1;
                }

            } catch (Throwable t) {
                // Mark the state as cancelled, we'll clear the pending queue via cancel() below.
                cancelled = true;
                cause = t;
            } finally {
                writing = false;
                // Make sure we always decrement the flow control windows
                // by the bytes written.
                writtenBytes = initialAllocated - allocated;

                decrementPendingBytes(writtenBytes, false);
                decrementFlowControlWindow(writtenBytes);

                // If a cancellation occurred while writing, call cancel again to
                // clear and error all of the pending writes.
                if (cancelled) {
                    cancel(INTERNAL_ERROR, cause);
                }
                if(monitor.isOverFlowControl()){
                    cause = new Throwable();
                    cancel(FLOW_CONTROL_ERROR,cause);
                }
            }
            return writtenBytes;
        }

        /**
         * Increments the flow control window for this stream by the given delta and returns the new value.
         */
        int incrementStreamWindow(int delta) throws Http2Exception {
            if (delta > 0 && Integer.MAX_VALUE - delta < window) {
                throw streamError(stream.id(), FLOW_CONTROL_ERROR,
                    "Window size overflow for stream: %d", stream.id());
            }
            window += delta;
            streamByteDistributor.updateStreamableBytes(this);
            return window;
        }

        /**
         * Returns the maximum writable window (minimum of the stream and connection windows).
         */
        private int writableWindow() {
            return min(window, connectionWindowSize());
        }

        @Override
        public long pendingBytes() {
            return pendingBytes;
        }

        /**
         * Adds the {@code frame} to the pending queue and increments the pending byte count.
         */
        void enqueueFrame(FlowControlled frame) {
            FlowControlled last = pendingWriteQueue.peekLast();
            if (last == null) {
                enqueueFrameWithoutMerge(frame);
                return;
            }

            int lastSize = last.size();
            if (last.merge(ctx, frame)) {
                incrementPendingBytes(last.size() - lastSize, true);
                return;
            }
            enqueueFrameWithoutMerge(frame);
        }

        private void enqueueFrameWithoutMerge(FlowControlled frame) {
            pendingWriteQueue.offer(frame);
            // This must be called after adding to the queue in order so that hasFrame() is
            // updated before updating the stream state.
            incrementPendingBytes(frame.size(), true);
        }

        @Override
        public boolean hasFrame() {
            return !pendingWriteQueue.isEmpty();
        }

        /**
         * Returns the head of the pending queue, or {@code null} if empty.
         */
        private FlowControlled peek() {
            return pendingWriteQueue.peek();
        }

        /**
         * Clears the pending queue and writes errors for each remaining frame.
         * @param error the {@link Http2Error} to use.
         * @param cause the {@link Throwable} that caused this method to be invoked.
         */
        void cancel(Http2Error error, Throwable cause) {
            cancelled = true;
            // Ensure that the queue can't be modified while we are writing.
            if (writing) {
                return;
            }

            FlowControlled frame = pendingWriteQueue.poll();
            if (frame != null) {
                // Only create exception once and reuse to reduce overhead of filling in the stacktrace.
                final Http2Exception exception = streamError(stream.id(), error, cause,
                    "Stream closed before write could take place");
                do {
                    writeError(frame, exception);
                    frame = pendingWriteQueue.poll();
                } while (frame != null);
            }

            streamByteDistributor.updateStreamableBytes(this);

            monitor.stateCancelled(this);
        }

        /**
         * Increments the number of pending bytes for this node and optionally updates the
         * {@link StreamByteDistributor}.
         */
        private void incrementPendingBytes(int numBytes, boolean updateStreamableBytes) {
            pendingBytes += numBytes;
            monitor.incrementPendingBytes(numBytes);
            if (updateStreamableBytes) {
                streamByteDistributor.updateStreamableBytes(this);
            }
        }

        /**
         * If this frame is in the pending queue, decrements the number of pending bytes for the stream.
         */
        private void decrementPendingBytes(int bytes, boolean updateStreamableBytes) {
            incrementPendingBytes(-bytes, updateStreamableBytes);
        }

        /**
         * Decrement the per stream and connection flow control window by {@code bytes}.
         */
        private void decrementFlowControlWindow(int bytes) {
            try {
                int negativeBytes = -bytes;
                connectionState.incrementStreamWindow(negativeBytes);
                incrementStreamWindow(negativeBytes);
            } catch (Http2Exception e) {
                // Should never get here since we're decrementing.
                throw new IllegalStateException("Invalid window state when writing frame: " + e.getMessage(), e);
            }
        }

        /**
         * Discards this {@link FlowControlled}, writing an error. If this frame is in the pending queue,
         * the unwritten bytes are removed from this branch of the priority tree.
         */
        private void writeError(FlowControlled frame, Http2Exception cause) {
            assert ctx != null;
            decrementPendingBytes(frame.size(), true);
            frame.error(ctx, cause);
        }
    }

    /**
     * Abstract class which provides common functionality for writability monitor implementations.
     */
    private class WritabilityMonitor implements StreamByteDistributor.Writer {
        private boolean inWritePendingBytes;
        private long totalPendingBytes;

        @Override
        public final void write(Http2Stream stream, int numBytes) {
            state(stream).writeAllocatedBytes(numBytes);
        }

        /**
         * Called when the writability of the underlying channel changes.
         * @throws Http2Exception If a write occurs and an exception happens in the write operation.
         */
        void channelWritabilityChange() throws Http2Exception { }

        /**
         * Called when the state is cancelled.
         * @param state the state that was cancelled.
         */
        void stateCancelled(FlowState state) { }

        /**
         * Set the initial window size for {@code state}.
         * @param state the state to change the initial window size for.
         * @param initialWindowSize the size of the window in bytes.
         */
        void windowSize(FlowState state, int initialWindowSize) {
            state.windowSize(initialWindowSize);
        }

        /**
         * Increment the window size for a particular stream.
         * @param state the state associated with the stream whose window is being incremented.
         * @param delta The amount to increment by.
         * @throws Http2Exception If this operation overflows the window for {@code state}.
         */
        void incrementWindowSize(FlowState state, int delta) throws Http2Exception {
            state.incrementStreamWindow(delta);
        }

        /**
         * Add a frame to be sent via flow control.
         * @param state The state associated with the stream which the {@code frame} is associated with.
         * @param frame the frame to enqueue.
         * @throws Http2Exception If a writability error occurs.
         */
        void enqueueFrame(FlowState state, FlowControlled frame) throws Http2Exception {
            state.enqueueFrame(frame);
        }

        /**
         * Increment the total amount of pending bytes for all streams. When any stream's pending bytes changes
         * method should be called.
         * @param delta The amount to increment by.
         */
        final void incrementPendingBytes(int delta) {
            totalPendingBytes += delta;

            // Notification of writibilty change should be delayed until the end of the top level event.
            // This is to ensure the flow controller is more consistent state before calling external listener methods.
        }

        /**
         * Determine if the stream associated with {@code state} is writable.
         * @param state The state which is associated with the stream to test writability for.
         * @return {@code true} if {@link FlowState#stream()} is writable. {@code false} otherwise.
         */
        final boolean isWritable(FlowState state) {
            return isWritableConnection() && state.isWritable();
        }

        final void writePendingBytes() throws Http2Exception {
            // Reentry is not permitted during the byte distribution process. It may lead to undesirable distribution of
            // bytes and even infinite loops. We protect against reentry and make sure each call has an opportunity to
            // cause a distribution to occur. This may be useful for example if the channel's writability changes from
            // Writable -> Not Writable (because we are writing) -> Writable (because the user flushed to make more room
            // in the channel outbound buffer).
            if (inWritePendingBytes) {
                return;
            }
            inWritePendingBytes = true;
            try {
                int bytesToWrite = writableBytes();
                // Make sure we always write at least once, regardless if we have bytesToWrite or not.
                // This ensures that zero-length frames will always be written.
                for (;;) {
                    if (!streamByteDistributor.distribute(bytesToWrite, this) ||
                        (bytesToWrite = writableBytes()) <= 0 ||
                        !isChannelWritable0()) {
                        break;
                    }
                }
            } finally {
                inWritePendingBytes = false;
            }
        }

        void initialWindowSize(int newWindowSize) throws Http2Exception {
            checkPositiveOrZero(newWindowSize, "newWindowSize");

            final int delta = newWindowSize - initialWindowSize;
            initialWindowSize = newWindowSize;
            connection.forEachActiveStream(new Http2StreamVisitor() {
                @Override
                public boolean visit(Http2Stream stream) throws Http2Exception {
                    state(stream).incrementStreamWindow(delta);
                    return true;
                }
            });

            if (delta > 0 && isChannelWritable()) {
                // The window size increased, send any pending frames for all streams.
                writePendingBytes();
            }
        }

        final boolean isWritableConnection() {
            return connectionState.windowSize() - totalPendingBytes > 0 && isChannelWritable();
        }

        final boolean isOverFlowControl() {
            if(connectionState.windowSize() == 0){
                return true;
            }else {
                return false;
            }
        }
    }

    /**
     * Writability of a {@code stream} is calculated using the following:
     * <pre>
     * Connection Window - Total Queued Bytes > 0 &&
     * Stream Window - Bytes Queued for Stream > 0 &&
     * isChannelWritable()
     * </pre>
     */
    private final class ListenerWritabilityMonitor extends WritabilityMonitor implements Http2StreamVisitor {
        private final Listener listener;

        ListenerWritabilityMonitor(Listener listener) {
            this.listener = listener;
        }

        @Override
        public boolean visit(Http2Stream stream) throws Http2Exception {
            FlowState state = state(stream);
            if (isWritable(state) != state.markedWritability()) {
                notifyWritabilityChanged(state);
            }
            return true;
        }

        @Override
        void windowSize(FlowState state, int initialWindowSize) {
            super.windowSize(state, initialWindowSize);
            try {
                checkStateWritability(state);
            } catch (Http2Exception e) {
                throw new RuntimeException("Caught unexpected exception from window", e);
            }
        }

        @Override
        void incrementWindowSize(FlowState state, int delta) throws Http2Exception {
            super.incrementWindowSize(state, delta);
            checkStateWritability(state);
        }

        @Override
        void initialWindowSize(int newWindowSize) throws Http2Exception {
            super.initialWindowSize(newWindowSize);
            if (isWritableConnection()) {
                // If the write operation does not occur we still need to check all streams because they
                // may have transitioned from writable to not writable.
                checkAllWritabilityChanged();
            }
        }

        @Override
        void enqueueFrame(FlowState state, FlowControlled frame) throws Http2Exception {
            super.enqueueFrame(state, frame);
            checkConnectionThenStreamWritabilityChanged(state);
        }

        @Override
        void stateCancelled(FlowState state) {
            try {
                checkConnectionThenStreamWritabilityChanged(state);
            } catch (Http2Exception e) {
                throw new RuntimeException("Caught unexpected exception from checkAllWritabilityChanged", e);
            }
        }

        @Override
        void channelWritabilityChange() throws Http2Exception {
            if (connectionState.markedWritability() != isChannelWritable()) {
                checkAllWritabilityChanged();
            }
        }

        private void checkStateWritability(FlowState state) throws Http2Exception {
            if (isWritable(state) != state.markedWritability()) {
                if (state == connectionState) {
                    checkAllWritabilityChanged();
                } else {
                    notifyWritabilityChanged(state);
                }
            }
        }

        private void notifyWritabilityChanged(FlowState state) {
            state.markedWritability(!state.markedWritability());
            try {
                listener.writabilityChanged(state.stream);
            } catch (Throwable cause) {
                logger.error("Caught Throwable from listener.writabilityChanged", cause);
            }
        }

        private void checkConnectionThenStreamWritabilityChanged(FlowState state) throws Http2Exception {
            // It is possible that the connection window and/or the individual stream writability could change.
            if (isWritableConnection() != connectionState.markedWritability()) {
                checkAllWritabilityChanged();
            } else if (isWritable(state) != state.markedWritability()) {
                notifyWritabilityChanged(state);
            }else if(isOverFlowControl()){
                throw streamError(state.stream().id(), FLOW_CONTROL_ERROR,
                    "TotalPendingBytes size overflow for stream: %d", state.stream().id());
            }
        }

        private void checkAllWritabilityChanged() throws Http2Exception {
            // Make sure we mark that we have notified as a result of this change.
            connectionState.markedWritability(isWritableConnection());
            connection.forEachActiveStream(this);
        }
    }
}

