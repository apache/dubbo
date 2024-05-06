package org.apache.dubbo.remoting.transport.netty4;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicChannelConfig;
import io.netty.incubator.codec.quic.QuicConnectionStats;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.incubator.codec.quic.QuicStreamChannelBootstrap;
import io.netty.incubator.codec.quic.QuicStreamType;
import io.netty.incubator.codec.quic.QuicTransportParameters;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import javax.net.ssl.SSLEngine;

import java.net.SocketAddress;

public class QuicToNettyChannelAdapter implements QuicChannel {
    private final QuicChannel ch;

    public QuicToNettyChannelAdapter(Channel ch) {
        if (!(ch instanceof QuicChannel)) {
            throw new IllegalArgumentException("ch should be QuicChannel");
        }
        this.ch = (QuicChannel) ch;
    }

    @Override
    public SocketAddress localAddress() {
        // todo: cast address type
        return null;
    }

    @Override
    public SocketAddress remoteAddress() {
        // todo: cast address type
        return null;
    }

    //////////////////////////////////////////////

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        return ch.bind(localAddress);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress) {
        return ch.connect(remoteAddress);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        return ch.connect(remoteAddress, localAddress);
    }

    @Override
    public ChannelFuture disconnect() {
        return ch.disconnect();
    }

    @Override
    public ChannelFuture close() {
        return ch.close();
    }

    @Override
    public ChannelFuture deregister() {
        return ch.deregister();
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        return ch.bind(localAddress, promise);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
        return ch.connect(remoteAddress, promise);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        return ch.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public ChannelFuture disconnect(ChannelPromise promise) {
        return ch.disconnect(promise);
    }

    @Override
    public ChannelFuture close(ChannelPromise promise) {
        return ch.close(promise);
    }

    @Override
    public ChannelFuture deregister(ChannelPromise promise) {
        return ch.deregister(promise);
    }

    @Override
    public ChannelFuture write(Object msg) {
        return ch.write(msg);
    }

    @Override
    public ChannelFuture write(Object msg, ChannelPromise promise) {
        return ch.write(msg, promise);
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        return ch.writeAndFlush(msg, promise);
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg) {
        return ch.writeAndFlush(msg);
    }

    @Override
    public ChannelPromise newPromise() {
        return ch.newPromise();
    }

    @Override
    public ChannelProgressivePromise newProgressivePromise() {
        return ch.newProgressivePromise();
    }

    @Override
    public ChannelFuture newSucceededFuture() {
        return ch.newSucceededFuture();
    }

    @Override
    public ChannelFuture newFailedFuture(Throwable cause) {
        return ch.newFailedFuture(cause);
    }

    @Override
    public ChannelPromise voidPromise() {
        return ch.voidPromise();
    }

    @Override
    public QuicChannel read() {
        return ch.read();
    }

    @Override
    public QuicChannel flush() {
        return ch.flush();
    }

    @Override
    public ChannelId id() {
        return ch.id();
    }

    @Override
    public EventLoop eventLoop() {
        return ch.eventLoop();
    }

    @Override
    public Channel parent() {
        return ch.parent();
    }

    @Override
    public QuicChannelConfig config() {
        return ch.config();
    }

    @Override
    public boolean isOpen() {
        return ch.isOpen();
    }

    @Override
    public boolean isRegistered() {
        return ch.isRegistered();
    }

    @Override
    public boolean isActive() {
        return ch.isActive();
    }

    @Override
    public ChannelMetadata metadata() {
        return ch.metadata();
    }

    @Override
    public ChannelFuture closeFuture() {
        return ch.closeFuture();
    }

    @Override
    public boolean isWritable() {
        return ch.isWritable();
    }

    @Override
    public long bytesBeforeUnwritable() {
        return ch.bytesBeforeUnwritable();
    }

    @Override
    public long bytesBeforeWritable() {
        return ch.bytesBeforeWritable();
    }

    @Override
    public Unsafe unsafe() {
        return ch.unsafe();
    }

    @Override
    public ChannelPipeline pipeline() {
        return ch.pipeline();
    }

    @Override
    public ByteBufAllocator alloc() {
        return ch.alloc();
    }

    @Override
    public SSLEngine sslEngine() {
        return ch.sslEngine();
    }

    @Override
    public long peerAllowedStreams(QuicStreamType quicStreamType) {
        return ch.peerAllowedStreams(quicStreamType);
    }

    @Override
    public boolean isTimedOut() {
        return ch.isTimedOut();
    }

    @Override
    public QuicTransportParameters peerTransportParameters() {
        return ch.peerTransportParameters();
    }

    @Override
    public Future<QuicStreamChannel> createStream(QuicStreamType type, ChannelHandler handler) {
        return ch.createStream(type, handler);
    }

    @Override
    public Future<QuicStreamChannel> createStream(
            QuicStreamType quicStreamType,
            ChannelHandler channelHandler,
            Promise<QuicStreamChannel> promise) {
        return null;
    }

    @Override
    public QuicStreamChannelBootstrap newStreamBootstrap() {
        return ch.newStreamBootstrap();
    }

    @Override
    public ChannelFuture close(boolean applicationClose, int error, ByteBuf reason) {
        return ch.close(applicationClose, error, reason);
    }

    @Override
    public ChannelFuture close(boolean b, int i, ByteBuf byteBuf, ChannelPromise channelPromise) {
        return null;
    }

    @Override
    public Future<QuicConnectionStats> collectStats() {
        return ch.collectStats();
    }

    @Override
    public Future<QuicConnectionStats> collectStats(Promise<QuicConnectionStats> promise) {
        return null;
    }

    @Override
    public <T> Attribute<T> attr(AttributeKey<T> attributeKey) {
        return null;
    }

    @Override
    public <T> boolean hasAttr(AttributeKey<T> attributeKey) {
        return false;
    }

    @Override
    public int compareTo(Channel o) {
        return 0;
    }
}
