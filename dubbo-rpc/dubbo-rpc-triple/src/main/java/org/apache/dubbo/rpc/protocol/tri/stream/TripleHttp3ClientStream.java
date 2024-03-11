package org.apache.dubbo.rpc.protocol.tri.stream;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3RequestStreamInitializer;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.Future;

import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.command.DataQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.HeaderQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.h3.Http3DataQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.h3.Http3EndStreamQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.h3.Http3HeaderQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.frame.Deframer;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleCommandOutBoundHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleWriteQueue;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.Executor;

public class TripleHttp3ClientStream extends AbstractStream implements ClientStream {
    public final ClientStream.Listener listener;
    private final TripleWriteQueue writeQueue;
    private final QuicChannel parent;
    private final Future<QuicStreamChannel> streamChannelFuture;
    private Deframer deframer;
    private boolean halfClosed;

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
    }

    private Future<QuicStreamChannel> initQuicStreamChannel() {
        return Http3.newRequestStream(parent, new Http3RequestStreamInitializer() {
            @Override
            protected void initRequestStream(QuicStreamChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new TripleCommandOutBoundHandler());
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
        /*if (rst) {
            return streamChannelFuture.getNow().newFailedFuture(new IOException("stream channel has reset"));
        }*/
        return parent.newSucceededFuture();
    }
}
