package org.apache.dubbo.remoting.http3.netty4;

import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.HttpOutputMessage;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2OutputMessage;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

public class NettyH3StreamChannel implements H2StreamChannel {
    @Override
    public CompletableFuture<Void> writeHeader(HttpMetadata httpMetadata) {
        return null;
    }

    @Override
    public CompletableFuture<Void> writeMessage(HttpOutputMessage httpOutputMessage) {
        return null;
    }

    @Override
    public SocketAddress remoteAddress() {
        return null;
    }

    @Override
    public SocketAddress localAddress() {
        return null;
    }

    @Override
    public void flush() {

    }

    @Override
    public CompletableFuture<Void> writeResetFrame(long errorCode) {
        return null;
    }

    @Override
    public Http2OutputMessage newOutputMessage(boolean endStream) {
        return null;
    }
}
