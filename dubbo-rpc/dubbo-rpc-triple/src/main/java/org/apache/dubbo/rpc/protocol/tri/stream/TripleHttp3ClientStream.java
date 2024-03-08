package org.apache.dubbo.rpc.protocol.tri.stream;

import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3RequestStreamInitializer;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.Future;

import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.frame.Deframer;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleCommandOutBoundHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleWriteQueue;

import java.net.SocketAddress;
import java.util.concurrent.Executor;

public class TripleHttp3ClientStream extends AbstractStream implements ClientStream {
    public final ClientStream.Listener listener;
    private final TripleWriteQueue writeQueue;
    private final QuicChannel parent;
    private final Future<QuicStreamChannel> streamChannelFuture;
    private Deframer deframer;

    public TripleHttp3ClientStream(
            FrameworkModel frameworkModel,
            Executor executor,
            ClientStream.Listener listener,
            TripleWriteQueue writeQueue,
            QuicChannel parent) {
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
        return null;
    }

    @Override
    public Future<?> sendHeader(Http2Headers headers) {
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
}
