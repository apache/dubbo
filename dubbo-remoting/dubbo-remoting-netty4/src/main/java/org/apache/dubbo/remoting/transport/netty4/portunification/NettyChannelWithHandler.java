package org.apache.dubbo.remoting.transport.netty4.portunification;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.api.newportunification.ChannelWithHandler;
import org.apache.dubbo.remoting.exchange.support.header.HeartbeatHandler;
import org.apache.dubbo.remoting.utils.PayloadDropper;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;

public class NettyChannelWithHandler extends ChannelWithHandler {
    private static final Logger logger = LoggerFactory.getLogger(NettyChannelWithHandler.class);
    /**
     * the cache for netty channel and dubbo channel
     */
    private static final ConcurrentMap<Channel, NettyChannelWithHandler> CHANNEL_MAP = new ConcurrentHashMap
        <Channel, NettyChannelWithHandler>();
    /**
     * netty channel
     */
    private final Channel channel;

    private final Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

    private final AtomicBoolean active = new AtomicBoolean(false);

    public NettyChannelWithHandler(Channel channel, ChannelHandler handler) {
        super(handler); // bind this handler to Class ChannelWithHandler
        this.channel = channel;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) channel.remoteAddress();
    }

    @Override
    public boolean isConnected() {
        return !isClosed() && active.get();
    }

    public boolean isActive() {
        return active.get();
    }

    public void markActive(boolean isActive) {
        active.set(isActive);
    }


    @Override
    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) channel.localAddress();
    }


    @Override
    public void send(Object message, boolean sent) throws RemotingException {
        super.send(message, sent);

        boolean success = true;
        int timeout = 0;
        try {
            ChannelFuture future = channel.writeAndFlush(message);
            if (sent) {
                // wait timeout ms
                timeout = getUrl().getPositiveParameter(TIMEOUT_KEY, DEFAULT_TIMEOUT);
                success = future.await(timeout);
            }
            Throwable cause = future.cause();
            if (cause != null) {
                throw cause;
            }
        } catch (Throwable e) {
            removeChannelIfDisconnected(channel);
            throw new RemotingException(this, "Failed to send message " + PayloadDropper.getRequestWithoutData(message) + " to " + getRemoteAddress() + ", cause: " + e.getMessage(), e);
        }
        if (!success) {
            throw new RemotingException(this, "Failed to send message " + PayloadDropper.getRequestWithoutData(message) + " to " + getRemoteAddress()
                + "in timeout(" + timeout + "ms) limit");
        }
    }

    //  create channel if it not exists, and bind handler to channel
    static NettyChannelWithHandler getOrAddChannel(Channel ch, ChannelHandler handler) {
        if (ch == null) {
            return null;
        }
        NettyChannelWithHandler ret = CHANNEL_MAP.get(ch);
        if (ret == null) {
            NettyChannelWithHandler nettyChannel = new NettyChannelWithHandler(ch, handler);
            if (ch.isActive()) {
                nettyChannel.markActive(true);
                ret = CHANNEL_MAP.putIfAbsent(ch, nettyChannel);
            }
            if (ret == null) {
                ret = nettyChannel;
            }
        }
        return ret;
    }

    static void removeChannelIfDisconnected(Channel ch) {
        if (ch != null && !ch.isActive()) {
            NettyChannelWithHandler nettyChannel = CHANNEL_MAP.remove(ch);
            if (nettyChannel != null) {
                nettyChannel.markActive(false);
            }
        }
    }

    static void removeChannel(Channel ch) {
        if (ch != null) {
            NettyChannelWithHandler nettyChannel = CHANNEL_MAP.remove(ch);
            if (nettyChannel != null) {
                nettyChannel.markActive(false);
            }
        }
    }


    @Override
    public void close() {
        try {
            super.close();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            removeChannelIfDisconnected(channel);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            attributes.clear();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            if (logger.isInfoEnabled()) {
                logger.info("Close netty channel " + channel);
            }
            channel.close();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
    }

    @Override
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    @Override
    public Object getAttribute(String key) {
        return attributes.get(key);
    }


    @Override
    public void setAttribute(String key, Object value) {
        // The null value is not allowed in the ConcurrentHashMap.
        if (value == null) {
            attributes.remove(key);
        } else {
            attributes.put(key, value);
        }
    }

    @Override
    public void removeAttribute(String key) {
        attributes.remove(key);
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((channel == null) ? 0 : channel.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }
        NettyChannelWithHandler other = (NettyChannelWithHandler) obj;
        if (channel == null) {
            if (other.channel != null) {
                return false;
            }
        } else if (!channel.equals(other.channel)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "NettyChannelWithHandler [channel=" + channel + "]";
    }

}
