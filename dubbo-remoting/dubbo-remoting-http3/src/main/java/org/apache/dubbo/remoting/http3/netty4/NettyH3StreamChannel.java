package org.apache.dubbo.remoting.http3.netty4;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.incubator.codec.quic.QuicStreamChannel;

import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.HttpOutputMessage;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2OutputMessage;
import org.apache.dubbo.remoting.http12.netty4.NettyHttpChannelFutureListener;
import org.apache.dubbo.remoting.http3.h3.Http3OutputMessageFrame;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

public class NettyH3StreamChannel implements H2StreamChannel {
    private final QuicStreamChannel quicStreamChannel;

    public NettyH3StreamChannel(QuicStreamChannel quicStreamChannel) {
        this.quicStreamChannel = quicStreamChannel;
    }

    @Override
    public CompletableFuture<Void> writeHeader(HttpMetadata httpMetadata) {
        NettyHttpChannelFutureListener nettyHttpChannelFutureListener = new NettyHttpChannelFutureListener();
        quicStreamChannel.write(httpMetadata).addListener(nettyHttpChannelFutureListener);
        return nettyHttpChannelFutureListener;
    }

    @Override
    public CompletableFuture<Void> writeMessage(HttpOutputMessage httpOutputMessage) {
        NettyHttpChannelFutureListener nettyHttpChannelFutureListener = new NettyHttpChannelFutureListener();
        quicStreamChannel.write(httpOutputMessage).addListener(nettyHttpChannelFutureListener);
        return nettyHttpChannelFutureListener;
    }

    @Override
    public SocketAddress remoteAddress() {
        // todo
        return new InetSocketAddress(Long.toString(quicStreamChannel.remoteAddress().streamId()), 0);
    }

    @Override
    public SocketAddress localAddress() {
        // todo
        return new InetSocketAddress(Long.toString(quicStreamChannel.localAddress().streamId()), 0);
    }

    @Override
    public void flush() {
        quicStreamChannel.flush();
    }

    @Override
    public CompletableFuture<Void> writeResetFrame(long errorCode) {
        // todo
        return null;
    }

    @Override
    public Http2OutputMessage newOutputMessage(boolean endStream) {
        ByteBuf buffer = quicStreamChannel.alloc().buffer();
        ByteBufOutputStream outputStream = new ByteBufOutputStream(buffer);
        return new Http3OutputMessageFrame(outputStream, endStream);
    }
}
