package org.apache.dubbo.rpc.protocol.tri.stream;

import com.google.protobuf.Any;
import com.google.rpc.DebugInfo;
import com.google.rpc.ErrorInfo;
import com.google.rpc.Status;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3Headers;
import io.netty.incubator.codec.http3.Http3RequestStreamInitializer;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.ClassLoadUtil;
import org.apache.dubbo.rpc.protocol.tri.ExceptionUtils;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.command.CancelQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.h3.Http3DataQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.h3.Http3EndStreamQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.h3.Http3HeaderQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.compressor.DeCompressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.frame.Deframer;
import org.apache.dubbo.rpc.protocol.tri.frame.TriDecoder;
import org.apache.dubbo.rpc.protocol.tri.transport.AbstractH2TransportListener;
import org.apache.dubbo.rpc.protocol.tri.transport.H2TransportListener;
import org.apache.dubbo.rpc.protocol.tri.transport.H3TransportListener;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleCommandOutBoundHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleHttp3ClientResponseHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleWriteQueue;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_RESPONSE;

public class TripleHttp3ClientStream extends AbstractStream implements ClientStream {
    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(TripleHttp3ClientStream.class);

    public final ClientStream.Listener listener;
    private final TripleWriteQueue writeQueue;
    private final QuicChannel parent;
    private final Future<QuicStreamChannel> streamChannelFuture;
    private Deframer deframer;
    private boolean halfClosed;
    private boolean rst;
    private boolean isReturnTriException = false;

    public TripleHttp3ClientStream(
            FrameworkModel frameworkModel,
            Executor executor,
            QuicChannel parent,
            ClientStream.Listener listener,
            TripleWriteQueue writeQueue) {
        super(executor, frameworkModel);
        this.listener = listener;
        this.writeQueue = writeQueue;
        this.parent = parent;
        this.streamChannelFuture = initQuicStreamChannel();

        // todo: NullPointerException
//        try {
//            streamChannelFuture.sync().getNow();
//        } catch (InterruptedException e) {
//            LOGGER.error(e.getMessage());
//        }
    }

    private Future<QuicStreamChannel> initQuicStreamChannel() {
        return Http3.newRequestStream(parent, new Http3RequestStreamInitializer() {
            @Override
            protected void initRequestStream(QuicStreamChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new TripleCommandOutBoundHandler());
                pipeline.addLast(new TripleHttp3ClientResponseHandler(createTransportListener()));
            }
        });
    }

    @Override
    public Future<?> sendMessage(byte[] message, int compressFlag, boolean eos) {
        ChannelFuture checkResult = preCheck();
        if (!checkResult.isSuccess()) {
            return checkResult;
        }
        final Http3DataQueueCommand cmd = Http3DataQueueCommand.create(streamChannelFuture, message, compressFlag);
        return this.writeQueue.enqueueFuture(cmd, parent.eventLoop()).addListener(future -> {
            if (!future.isSuccess()) {
                cancelByLocal(TriRpcStatus.INTERNAL
                        .withDescription("Client write message failed")
                        .withCause(future.cause()));
                transportException(future.cause());
            }
        });
    }

    @Override
    public Future<?> sendHeader(Http2Headers headers) {
        if (this.writeQueue == null) {
            // already processed at createStream()
            return parent.newFailedFuture(new IllegalStateException("Stream already closed"));
        }
        ChannelFuture checkResult = preCheck();
        if (!checkResult.isSuccess()) {
            return checkResult;
        }
        final Http3HeaderQueueCommand headerCmd = Http3HeaderQueueCommand.createHeaders(streamChannelFuture, headers);
        return writeQueue.enqueueFuture(headerCmd, parent.eventLoop()).addListener(future -> {
            if (!future.isSuccess()) {
                transportException(future.cause());
            }
        });
    }

    @Override
    public Future<?> halfClose() {
        ChannelFuture checkResult = preCheck();
        if (!checkResult.isSuccess()) {
            return checkResult;
        }

        final Http3EndStreamQueueCommand cmd = Http3EndStreamQueueCommand.create(streamChannelFuture);
        return this.writeQueue.enqueueFuture(cmd, parent.eventLoop()).addListener(future -> {
            if (future.isSuccess()) {
                halfClosed = true;
            }
        });
    }

    @Override
    public Future<?> cancelByLocal(TriRpcStatus status) {
        return null;
    }

    @Override
    public SocketAddress remoteAddress() {
        return parent.remoteAddress();
    }

    @Override
    public void request(int n) {
        deframer.request(n);
    }

    private void transportException(Throwable cause) {
        final TriRpcStatus status =
                TriRpcStatus.INTERNAL.withDescription("Http3 exception").withCause(cause);
        listener.onComplete(status, null, null, false);
    }

    private ChannelFuture preCheck() {
        if (rst) {
            return streamChannelFuture.getNow().newFailedFuture(new IOException("stream channel has reset"));
        }
        return parent.newSucceededFuture();
    }

    private H3TransportListener createTransportListener() {
        return new H3ClientTransportListenerAdapter();
    }

    class H3ClientTransportListenerAdapter implements H3TransportListener {
        private final H2ClientTransportListener h2TransportListener = new H2ClientTransportListener();

        @Override
        public void onHeader(Http3Headers headers) {
            Http2Headers http2Headers = H3Headers2H2Headers(headers);
            final boolean endStream = headers.contains("end-stream");
            h2TransportListener.onHeader(http2Headers, endStream);
        }

        @Override
        public void onData(ByteBuf data, boolean endStream) {
            h2TransportListener.onData(data, endStream);
        }

        private Http2Headers H3Headers2H2Headers(Http3Headers h3h) {
            Http2Headers h2h = new DefaultHttp2Headers();
            for (Entry<CharSequence, CharSequence> entry: h3h) {
                h2h.add(entry.getKey(), entry.getValue());
            }
            return h2h;
        }
    }

    class H2ClientTransportListener extends AbstractH2TransportListener implements H2TransportListener {
        private TriRpcStatus transportError;
        private DeCompressor decompressor;
        private boolean headerReceived;
        private Http2Headers trailers;

        void handleH2TransportError(TriRpcStatus status) {
            // todo: Http3CancelQueueCommand
            /*writeQueue.enqueue(CancelQueueCommand.createCommand(streamChannelFuture, Http2Error.NO_ERROR));*/
            TripleHttp3ClientStream.this.rst = true;
            finishProcess(status, null, false);
        }

        void finishProcess(TriRpcStatus status, Http2Headers trailers, boolean isReturnTriException) {
            final Map<String, String> reserved = filterReservedHeaders(trailers);
            final Map<String, Object> attachments =
                    headersToMap(trailers, () -> reserved.get(TripleHeaderEnum.TRI_HEADER_CONVERT.getHeader()));
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
            final CharSequence contentType = headers.get(TripleHeaderEnum.CONTENT_TYPE_KEY.getHeader());
            if (contentType == null
                    || !contentType.toString().startsWith(TripleHeaderEnum.APPLICATION_GRPC.getHeader())) {
                return TriRpcStatus.fromCode(TriRpcStatus.httpStatusToGrpcCode(httpStatus))
                        .withDescription("invalid content-type: " + contentType);
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
            CharSequence messageEncoding = headers.get(TripleHeaderEnum.GRPC_ENCODING.getHeader());
            CharSequence triExceptionCode = headers.get(TripleHeaderEnum.TRI_EXCEPTION_CODE.getHeader());
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
                public void onRawMessage(byte[] data) {
                    TripleHttp3ClientStream.this.listener.onMessage(data, isReturnTriException);
                }

                public void close() {
                    finishProcess(statusFromTrailers(trailers), trailers, isReturnTriException);
                }
            };
            deframer = new TriDecoder(decompressor, listener);
            TripleHttp3ClientStream.this.listener.onStart();
        }

        void onTrailersReceived(Http2Headers trailers) {
            if (transportError == null && !headerReceived) {
                transportError = validateHeaderStatus(trailers);
            }
            if (transportError != null) {
                transportError = transportError.appendDescription("trailers: " + trailers);
            } else {
                this.trailers = trailers;
                TriRpcStatus status = statusFromTrailers(trailers);
                if (deframer == null) {
                    finishProcess(status, trailers, false);
                }
                if (deframer != null) {
                    deframer.close();
                }
            }
        }

        /**
         * Extract the response status from trailers.
         */
        private TriRpcStatus statusFromTrailers(Http2Headers trailers) {
            final Integer intStatus = trailers.getInt(TripleHeaderEnum.STATUS_KEY.getHeader());
            TriRpcStatus status = intStatus == null ? null : TriRpcStatus.fromCode(intStatus);
            if (status != null) {
                final CharSequence message = trailers.get(TripleHeaderEnum.MESSAGE_KEY.getHeader());
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

        private TriRpcStatus getStatusFromTrailers(Map<String, String> metadata) {
            if (null == metadata) {
                return null;
            }
            if (!getGrpcStatusDetailEnabled()) {
                return null;
            }
            // second get status detail
            if (!metadata.containsKey(TripleHeaderEnum.STATUS_DETAIL_KEY.getHeader())) {
                return null;
            }
            final String raw = (metadata.remove(TripleHeaderEnum.STATUS_DETAIL_KEY.getHeader()));
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
            executor.execute(() -> {
                if (endStream) {
                    if (!halfClosed) {
                        QuicStreamChannel channel = streamChannelFuture.getNow();
                        if (channel.isActive() && !rst) {
                            // todo: Http3CancelQueueCommand
                            /*writeQueue.enqueue(
                                    CancelQueueCommand.createCommand(streamChannelFuture, Http2Error.CANCEL));*/
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
    }
}
