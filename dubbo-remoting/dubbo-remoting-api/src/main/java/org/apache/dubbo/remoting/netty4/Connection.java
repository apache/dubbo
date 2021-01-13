package org.apache.dubbo.remoting.netty4;

import org.apache.dubbo.remoting.ChannelStatus;
import org.apache.dubbo.remoting.RemotingException;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;

public class Connection {
    private static final AttributeKey<Connection> CONNECTION = AttributeKey.valueOf("connection");
    private final InetSocketAddress remote;
    private volatile ChannelStatus status;
    private volatile Channel channel;

    public Connection(InetSocketAddress remote, Channel channel) {
        this.remote = remote;
        this.channel = channel;
        this.channel.attr(CONNECTION).set(this);
    }


    public Channel getChannel(){
        return channel;
    }
    public static Connection getConnectionFromChannel(Channel channel) {
        return channel.attr(CONNECTION).get();
    }

    public void onConnected() {
        this.status = ChannelStatus.CONNECTED;
    }
    public void onConnected(Channel channel) {
        onConnected();
        this.channel = channel;
        channel.attr(CONNECTION).set(this);
    }

    public boolean isAvailable() {
        return ChannelStatus.CONNECTED == getStatus();
    }

    public ChannelStatus getStatus() {
        return status;
    }

    public void setStatus(ChannelStatus status) {
        this.status = status;
    }

    public boolean isClosed() {
        return getStatus() == ChannelStatus.CLOSED;
    }

    public void close() {
        setStatus(ChannelStatus.CLOSED);
        if (channel != null) {
            channel.close();
        }
    }

    public ChannelFuture write(Object request) throws RemotingException {
        return this.channel.writeAndFlush(request);
    }

    public InetSocketAddress getRemote() {
        return remote;
    }
}
