package org.apache.dubbo.remoting.http3.netty4;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.incubator.codec.http3.DefaultHttp3DataFrame;
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame;
import io.netty.incubator.codec.http3.Http3DataFrame;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.quic.QuicStreamChannel;

import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.HttpOutputMessage;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2OutputMessage;
import org.apache.dubbo.remoting.http12.netty4.NettyHttpChannelFutureListener;
import org.apache.dubbo.remoting.http3.h3.Http3OutputMessageFrame;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

public class NettyH3StreamChannel implements H2StreamChannel {
    private final QuicStreamChannel quicStreamChannel;

    public NettyH3StreamChannel(QuicStreamChannel quicStreamChannel) {
        this.quicStreamChannel = quicStreamChannel;
    }

    @Override
    public CompletableFuture<Void> writeHeader(HttpMetadata httpMetadata) {
        NettyHttpChannelFutureListener nettyHttpChannelFutureListener = new NettyHttpChannelFutureListener();
        Http3HeadersFrame headers = Metadata2Headers(httpMetadata);
        quicStreamChannel.write(headers).addListener(nettyHttpChannelFutureListener);
        return nettyHttpChannelFutureListener;
    }

    private Http3HeadersFrame Metadata2Headers(HttpMetadata metadata) {
        Http3HeadersFrame headers = new DefaultHttp3HeadersFrame();
        HttpHeaders metadataHeaders = metadata.headers();
        for (Entry<String, List<String>> entry: metadataHeaders.entrySet()) {
            headers.headers().add(entry.getKey(), entry.getValue());
        }
        return headers;
    }

    @Override
    public CompletableFuture<Void> writeMessage(HttpOutputMessage httpOutputMessage) {
        NettyHttpChannelFutureListener nettyHttpChannelFutureListener = new NettyHttpChannelFutureListener();
        Http3DataFrame dataFrame = Output2Data(httpOutputMessage);
        quicStreamChannel.write(dataFrame).addListener(nettyHttpChannelFutureListener);
        return nettyHttpChannelFutureListener;
    }

    private Http3DataFrame Output2Data(HttpOutputMessage output) {
        ByteBufOutputStream stream = (ByteBufOutputStream)output.getBody();
        return new DefaultHttp3DataFrame(stream.buffer());
    }

    @Override
    public SocketAddress remoteAddress() {
        return new InetSocketAddress(Long.toString(quicStreamChannel.remoteAddress().streamId()), 0);
    }

    @Override
    public SocketAddress localAddress() {
        return new InetSocketAddress(Long.toString(quicStreamChannel.localAddress().streamId()), 0);
    }

    @Override
    public void flush() {
        quicStreamChannel.flush();
    }

    @Override
    public CompletableFuture<Void> writeResetFrame(long errorCode) {
        DefaultHttp3HeadersFrame frame = new DefaultHttp3HeadersFrame();
        frame.headers().addLong("reset", errorCode);
        NettyHttpChannelFutureListener nettyHttpChannelFutureListener = new NettyHttpChannelFutureListener();
        quicStreamChannel.write(frame).addListener(nettyHttpChannelFutureListener);
        return nettyHttpChannelFutureListener;
    }

    @Override
    public Http2OutputMessage newOutputMessage(boolean endStream) {
        ByteBuf buffer = quicStreamChannel.alloc().buffer();
        ByteBufOutputStream outputStream = new ByteBufOutputStream(buffer);
        return new Http3OutputMessageFrame(outputStream, endStream);
    }
}
