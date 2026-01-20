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
package org.apache.dubbo.rpc.protocol.tri.stream;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.ClassLoadUtil;
import org.apache.dubbo.rpc.protocol.tri.ExceptionUtils;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.command.CancelQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.DataQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.EndStreamQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.HeaderQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.InitOnReadyQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.compressor.DeCompressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.frame.Deframer;
import org.apache.dubbo.rpc.protocol.tri.frame.TriDecoder;
import org.apache.dubbo.rpc.protocol.tri.h12.grpc.GrpcUtils;
import org.apache.dubbo.rpc.protocol.tri.transport.AbstractH2TransportListener;
import org.apache.dubbo.rpc.protocol.tri.transport.H2TransportListener;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleWriteQueue;

import javax.net.ssl.SSLSession;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import com.google.protobuf.Any;
import com.google.rpc.DebugInfo;
import com.google.rpc.ErrorInfo;
import com.google.rpc.Status;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_RESPONSE;

/**
 * ClientStream is an abstraction for bidirectional messaging. It maintains a {@link TripleWriteQueue} to
 * write Http2Frame to remote. A {@link H2TransportListener} receives Http2Frame from remote.
 * Instead of maintaining state, this class depends on upper layer or transport layer's states.
 */
public abstract class AbstractTripleClientStream extends AbstractStream implements ClientStream {

    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(AbstractTripleClientStream.class);
    private static final AttributeKey<SSLSession> SSL_SESSION_KEY = AttributeKey.valueOf(Constants.SSL_SESSION_KEY);

    private final ClientStream.Listener listener;
    protected final TripleWriteQueue writeQueue;
    private Deframer deframer;
    private final Channel parent;
    private TripleStreamChannelFuture streamChannelFuture;
    private boolean halfClosed;
    private boolean rst;

    private boolean isReturnTriException = false;

    /**
     * Number of bytes currently queued, waiting to be sent.
     * When this falls below ON_READY_THRESHOLD, onReady will be triggered.
     */
    private final AtomicLong numSentBytesQueued = new AtomicLong(0);

    /**
     * The threshold below which isReady() returns true (32KB).
     */
    protected static final long ON_READY_THRESHOLD = 32 * 1024;

    protected AbstractTripleClientStream(
            FrameworkModel frameworkModel,
            Executor executor,
            TripleWriteQueue writeQueue,
            ClientStream.Listener listener,
            Http2StreamChannel http2StreamChannel) {
        super(executor, frameworkModel);
        this.parent = http2StreamChannel.parent();
        this.listener = listener;
        this.writeQueue = writeQueue;
    }

    protected AbstractTripleClientStream(
            FrameworkModel frameworkModel,
            Executor executor,
            TripleWriteQueue writeQueue,
            ClientStream.Listener listener,
            Channel parent) {
        super(executor, frameworkModel);
        this.parent = parent;
        this.listener = listener;
        this.writeQueue = writeQueue;
    }

    @Override
    public void initStream() {
        initStreamChannel(this.parent);
    }

    private TripleStreamChannelFuture initStreamChannel(Channel parent) {
        TripleStreamChannelFuture tripleStreamChannelFuture = initStreamChannel0(parent);
        this.streamChannelFuture = tripleStreamChannelFuture;
        /**
         * Enqueue InitOnReadyQueueCommand after the stream creation command.
         * Since WriteQueue executes commands in order within the EventLoop,
         * this command will run after the stream channel has been created by CreateStreamQueueCommand.
         *
         * This is necessary because onReady is only triggered by channelWritabilityChanged,
         * which won't fire if the channel is always writable from creation.
         */
        writeQueue.enqueue(InitOnReadyQueueCommand.create(tripleStreamChannelFuture, this));
        return tripleStreamChannelFuture;
    }

    protected abstract TripleStreamChannelFuture initStreamChannel0(Channel parent);

    /**
     * Get the stream channel future for flow control.
     */
    protected TripleStreamChannelFuture getStreamChannelFuture() {
        return streamChannelFuture;
    }

    public ChannelFuture sendHeader(Http2Headers headers) {
        if (this.writeQueue == null) {
            // already processed at createStream()
            return parent.newFailedFuture(new IllegalStateException("Stream already closed"));
        }
        ChannelFuture checkResult = preCheck();
        if (!checkResult.isSuccess()) {
            return checkResult;
        }
        final HeaderQueueCommand headerCmd = HeaderQueueCommand.createHeaders(streamChannelFuture, headers);
        return writeQueue.enqueueFuture(headerCmd, parent.eventLoop()).addListener(future -> {
            if (!future.isSuccess()) {
                transportException(future.cause());
            }
        });
    }

    private void transportException(Throwable cause) {
        final TriRpcStatus status =
                TriRpcStatus.INTERNAL.withDescription("Http2 exception").withCause(cause);
        listener.onComplete(status, null, null, false);
    }

    public ChannelFuture cancelByLocal(TriRpcStatus status) {
        ChannelFuture checkResult = preCheck();
        if (!checkResult.isSuccess()) {
            return checkResult;
        }
        final CancelQueueCommand cmd = CancelQueueCommand.createCommand(streamChannelFuture, Http2Error.CANCEL);

        rst = true;
        return this.writeQueue.enqueue(cmd);
    }

    @Override
    public SocketAddress remoteAddress() {
        return parent.remoteAddress();
    }

    @Override
    public SSLSession getSslSession() {
        return parent.attr(SSL_SESSION_KEY).get();
    }

    @Override
    public ChannelFuture sendMessage(byte[] message, int compressFlag) {
        ChannelFuture checkResult = preCheck();
        if (!checkResult.isSuccess()) {
            return checkResult;
        }

        final int messageSize = message.length;
        onSendingBytes(messageSize);

        final DataQueueCommand cmd = DataQueueCommand.create(streamChannelFuture, message, false, compressFlag);
        return this.writeQueue.enqueueFuture(cmd, parent.eventLoop()).addListener(future -> {
            if (!future.isSuccess()) {
                rollbackSendingBytes(messageSize);
                cancelByLocal(TriRpcStatus.INTERNAL
                        .withDescription("Client write message failed")
                        .withCause(future.cause()));
                transportException(future.cause());
            } else {
                onSentBytes(messageSize);
            }
        });
    }

    /**
     * Called before bytes are sent to track pending bytes.
     *
     * @param numBytes the number of bytes about to be sent
     */
    protected void onSendingBytes(int numBytes) {
        numSentBytesQueued.addAndGet(numBytes);
    }

    /**
     * Called when sending fails to rollback the pending bytes count.
     *
     * @param numBytes the number of bytes to rollback
     */
    protected void rollbackSendingBytes(int numBytes) {
        numSentBytesQueued.addAndGet(-numBytes);
    }

    /**
     * Called when bytes have been successfully sent to the remote endpoint.
     *
     * @param numBytes the number of bytes that were sent
     */
    protected void onSentBytes(int numBytes) {
        long oldValue = numSentBytesQueued.getAndAdd(-numBytes);
        long newValue = oldValue - numBytes;
        // Trigger onReady when transitioning from "not ready" to "ready"
        if (oldValue >= ON_READY_THRESHOLD && newValue < ON_READY_THRESHOLD) {
            listener.onReady();
        }
    }

    /**
     * Returns the number of bytes currently queued for sending.
     * Visible for testing.
     */
    protected long getNumSentBytesQueued() {
        return numSentBytesQueued.get();
    }

    @Override
    public void request(int n) {
        deframer.request(n);
    }

    @Override
    public ChannelFuture halfClose() {
        ChannelFuture checkResult = preCheck();
        if (!checkResult.isSuccess()) {
            return checkResult;
        }
        final EndStreamQueueCommand cmd = EndStreamQueueCommand.create(streamChannelFuture);
        return this.writeQueue.enqueueFuture(cmd, parent.eventLoop()).addListener(future -> {
            if (future.isSuccess()) {
                halfClosed = true;
            }
        });
    }

    private ChannelFuture preCheck() {
        if (rst) {
            return streamChannelFuture.getNow().newFailedFuture(new IOException("stream channel has reset"));
        }
        return parent.newSucceededFuture();
    }

    /**
     * @return transport listener
     */
    protected H2TransportListener createTransportListener() {
        return new ClientTransportListener();
    }

    /**
     * Consume bytes for flow control. This method is called after bytes are read from the stream.
     * It triggers WINDOW_UPDATE frames to allow more data from the remote peer.
     * Subclasses can override this method to provide protocol-specific flow control.
     *
     * @param numBytes the number of bytes consumed
     */
    protected abstract void consumeBytes(int numBytes);

    @Override
    public boolean isReady() {
        Channel channel = streamChannelFuture.getNow();
        if (channel == null) {
            return false;
        }
        return numSentBytesQueued.get() < ON_READY_THRESHOLD;
    }

    /**
     * Called when the channel writability changes.
     */
    protected void onWritabilityChanged() {
        if (isReady()) {
            listener.onReady();
        }
    }

    /**
     * Called by InitOnReadyQueueCommand to trigger the initial onReady notification.
     */
    public void triggerInitialOnReady() {
        listener.onReady();
    }

    class ClientTransportListener extends AbstractH2TransportListener implements H2TransportListener {

        private TriRpcStatus transportError;
        private DeCompressor decompressor;
        private boolean headerReceived;
        private Http2Headers trailers;

        void handleH2TransportError(TriRpcStatus status) {
            writeQueue.enqueue(CancelQueueCommand.createCommand(streamChannelFuture, Http2Error.NO_ERROR));
            rst = true;
            finishProcess(status, null, false);
        }

        void finishProcess(TriRpcStatus status, Http2Headers trailers, boolean isReturnTriException) {
            final Map<CharSequence, String> reserved = filterReservedHeaders(trailers);
            final Map<String, Object> attachments =
                    headersToMap(trailers, () -> reserved.get(TripleHeaderEnum.TRI_HEADER_CONVERT.getKey()));
            final TriRpcStatus detailStatus;
            final TriRpcStatus statusFromTrailers = getStatusFromTrailers(reserved);
            if (statusFromTrailers != null) {
                detailStatus = statusFromTrailers;
            } else {
                detailStatus = status;
            }
            listener.onComplete(detailStatus, attachments, reserved, isReturnTriException);
        }

        private TriRpcStatus validateHeaderStatus(Http2Headers headers) {
            Integer httpStatus = headers.status() == null
                    ? null
                    : Integer.parseInt(headers.status().toString());
            if (httpStatus == null) {
                return TriRpcStatus.INTERNAL.withDescription("Missing HTTP status code");
            }
            final CharSequence contentType = headers.get(HttpHeaderNames.CONTENT_TYPE.getKey());
            if (contentType == null || !GrpcUtils.isGrpcRequest(contentType.toString())) {
                return TriRpcStatus.fromCode(TriRpcStatus.httpStatusToGrpcCode(httpStatus))
                        .withDescription("HTTP status: " + httpStatus + ", invalid content-type: " + contentType);
            }
            return null;
        }

        void onHeaderReceived(Http2Headers headers) {
            if (transportError != null) {
                transportError.appendDescription("headers:" + headers);
                return;
            }
            if (headerReceived) {
                transportError = TriRpcStatus.INTERNAL.withDescription("Received headers twice");
                return;
            }
            Integer httpStatus = headers.status() == null
                    ? null
                    : Integer.parseInt(headers.status().toString());

            if (httpStatus != null && Integer.parseInt(httpStatus.toString()) > 100 && httpStatus < 200) {
                // ignored
                return;
            }
            headerReceived = true;
            transportError = validateHeaderStatus(headers);

            // todo support full payload compressor
            CharSequence messageEncoding = headers.get(TripleHeaderEnum.GRPC_ENCODING.getKey());
            CharSequence triExceptionCode = headers.get(TripleHeaderEnum.TRI_EXCEPTION_CODE.getKey());
            if (triExceptionCode != null) {
                Integer triExceptionCodeNum = Integer.parseInt(triExceptionCode.toString());
                if (!(triExceptionCodeNum.equals(CommonConstants.TRI_EXCEPTION_CODE_NOT_EXISTS))) {
                    isReturnTriException = true;
                }
            }
            if (null != messageEncoding) {
                String compressorStr = messageEncoding.toString();
                if (!Identity.IDENTITY.getMessageEncoding().equals(compressorStr)) {
                    DeCompressor compressor = DeCompressor.getCompressor(frameworkModel, compressorStr);
                    if (null == compressor) {
                        throw TriRpcStatus.UNIMPLEMENTED
                                .withDescription(String.format("Grpc-encoding '%s' is not supported", compressorStr))
                                .asException();
                    } else {
                        decompressor = compressor;
                    }
                }
            }
            TriDecoder.Listener listener = new TriDecoder.Listener() {

                @Override
                public void bytesRead(int numBytes) {
                    consumeBytes(numBytes);
                }

                @Override
                public void onRawMessage(byte[] data) {
                    AbstractTripleClientStream.this.listener.onMessage(data, isReturnTriException);
                }

                public void close() {
                    finishProcess(statusFromTrailers(trailers), trailers, isReturnTriException);
                }
            };
            deframer = new TriDecoder(decompressor, listener);
            AbstractTripleClientStream.this.listener.onStart();
        }

        void onTrailersReceived(Http2Headers trailers) {
            if (transportError == null && !headerReceived) {
                transportError = validateHeaderStatus(trailers);
            }
            this.trailers = trailers;
            TriRpcStatus status;
            if (transportError == null) {
                status = statusFromTrailers(trailers);
            } else {
                transportError = transportError.appendDescription("trailers: " + trailers);
                status = transportError;
            }
            if (deframer == null) {
                finishProcess(status, trailers, false);
            } else {
                deframer.close();
            }
        }

        /**
         * Extract the response status from trailers.
         */
        private TriRpcStatus statusFromTrailers(Http2Headers trailers) {
            final Integer intStatus = trailers.getInt(TripleHeaderEnum.STATUS_KEY.getKey());
            TriRpcStatus status = intStatus == null ? null : TriRpcStatus.fromCode(intStatus);
            if (status != null) {
                final CharSequence message = trailers.get(TripleHeaderEnum.MESSAGE_KEY.getKey());
                if (message != null) {
                    final String description = TriRpcStatus.decodeMessage(message.toString());
                    status = status.withDescription(description);
                }
                return status;
            }
            // No status; something is broken. Try to provide a rational error.
            if (headerReceived) {
                return TriRpcStatus.UNKNOWN.withDescription("missing GRPC status in response");
            }
            Integer httpStatus = trailers.status() == null
                    ? null
                    : Integer.parseInt(trailers.status().toString());
            if (httpStatus != null) {
                status = TriRpcStatus.fromCode(TriRpcStatus.httpStatusToGrpcCode(httpStatus));
            } else {
                status = TriRpcStatus.INTERNAL.withDescription("missing HTTP status code");
            }
            return status.appendDescription("missing GRPC status, inferred error from HTTP status code");
        }

        private TriRpcStatus getStatusFromTrailers(Map<CharSequence, String> metadata) {
            if (null == metadata) {
                return null;
            }
            if (!getGrpcStatusDetailEnabled()) {
                return null;
            }
            // second get status detail
            if (!metadata.containsKey(TripleHeaderEnum.STATUS_DETAIL_KEY.getKey())) {
                return null;
            }
            final String raw = (metadata.remove(TripleHeaderEnum.STATUS_DETAIL_KEY.getKey()));
            byte[] statusDetailBin = StreamUtils.decodeASCIIByte(raw);
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            try {
                final Status statusDetail = Status.parseFrom(statusDetailBin);
                List<Any> detailList = statusDetail.getDetailsList();
                Map<Class<?>, Object> classObjectMap = tranFromStatusDetails(detailList);

                // get common exception from DebugInfo
                TriRpcStatus status = TriRpcStatus.fromCode(statusDetail.getCode())
                        .withDescription(TriRpcStatus.decodeMessage(statusDetail.getMessage()));
                DebugInfo debugInfo = (DebugInfo) classObjectMap.get(DebugInfo.class);
                if (debugInfo != null) {
                    String msg = ExceptionUtils.getStackFrameString(debugInfo.getStackEntriesList());
                    status = status.appendDescription(msg);
                }
                return status;
            } catch (IOException ioException) {
                return null;
            } finally {
                ClassLoadUtil.switchContextLoader(tccl);
            }
        }

        private Map<Class<?>, Object> tranFromStatusDetails(List<Any> detailList) {
            Map<Class<?>, Object> map = new HashMap<>(detailList.size());
            try {
                for (Any any : detailList) {
                    if (any.is(ErrorInfo.class)) {
                        ErrorInfo errorInfo = any.unpack(ErrorInfo.class);
                        map.putIfAbsent(ErrorInfo.class, errorInfo);
                    } else if (any.is(DebugInfo.class)) {
                        DebugInfo debugInfo = any.unpack(DebugInfo.class);
                        map.putIfAbsent(DebugInfo.class, debugInfo);
                    }
                    // support others type but now only support this
                }
            } catch (Throwable t) {
                LOGGER.error(PROTOCOL_FAILED_RESPONSE, "", "", "tran from grpc-status-details error", t);
            }
            return map;
        }

        @Override
        public void onHeader(Http2Headers headers, boolean endStream) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("endStream: {} HEADERS: {}", endStream, headers);
            }
            executor.execute(() -> {
                if (endStream) {
                    if (!halfClosed) {
                        Channel channel = streamChannelFuture.getNow();
                        if (channel.isActive() && !rst) {
                            writeQueue.enqueue(
                                    CancelQueueCommand.createCommand(streamChannelFuture, Http2Error.CANCEL));
                            rst = true;
                        }
                    }
                    onTrailersReceived(headers);
                } else {
                    onHeaderReceived(headers);
                }
            });
        }

        @Override
        public void onData(ByteBuf data, boolean endStream) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("endStream: {} DATA: {}", endStream, data.toString(StandardCharsets.UTF_8));
            }
            try {
                executor.execute(() -> doOnData(data, endStream));
            } catch (Throwable t) {
                // Tasks will be rejected when the thread pool is closed or full,
                // ByteBuf needs to be released to avoid out of heap memory leakage.
                // For example, ThreadLessExecutor will be shutdown when request timeout {@link AsyncRpcResult}
                ReferenceCountUtil.release(data);
                LOGGER.error(PROTOCOL_FAILED_RESPONSE, "", "", "submit onData task failed", t);
            }
        }

        private void doOnData(ByteBuf data, boolean endStream) {
            if (transportError != null) {
                transportError.appendDescription("Data:" + data.toString(StandardCharsets.UTF_8));
                ReferenceCountUtil.release(data);
                if (transportError.description.length() > 512 || endStream) {
                    handleH2TransportError(transportError);
                }
                return;
            }
            if (!headerReceived) {
                handleH2TransportError(TriRpcStatus.INTERNAL.withDescription("headers not received before payload"));
                return;
            }
            deframer.deframe(data);
        }

        @Override
        public void cancelByRemote(long errorCode) {
            executor.execute(() -> {
                transportError =
                        TriRpcStatus.CANCELLED.withDescription("Canceled by remote peer, errorCode=" + errorCode);
                finishProcess(transportError, null, false);
            });
        }

        @Override
        public void onClose() {
            executor.execute(listener::onClose);
        }

        @Override
        public void onWritabilityChanged() {
            AbstractTripleClientStream.this.onWritabilityChanged();
        }
    }
}
