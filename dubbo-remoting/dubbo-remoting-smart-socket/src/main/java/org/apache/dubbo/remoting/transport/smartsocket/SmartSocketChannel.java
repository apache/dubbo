package org.apache.dubbo.remoting.transport.smartsocket;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Codec2;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.buffer.ChannelBuffers;
import org.apache.dubbo.remoting.transport.AbstractChannel;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/1/25
 */
public class SmartSocketChannel extends AbstractChannel {
    private static final Logger logger = LoggerFactory.getLogger(SmartSocketChannel.class);
    private final AioSession session;
    private final Codec2 codec;
    private final Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();
    private final String channelKey;
    private InetSocketAddress localAddress;
    private ChannelBuffer frame = ChannelBuffers.EMPTY_BUFFER;

    public SmartSocketChannel(AioSession session, URL url, ChannelHandler handler, Codec2 codec) {
        super(url, handler);
        this.session = session;
        this.codec = codec;
        this.channelKey = NetUtils.toAddressString(getRemoteAddress());
        try {
            localAddress = session.getLocalAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getChannelKey() {
        return channelKey;
    }

    public ChannelBuffer getFrame() {
        return frame;
    }

    public void setFrame(ChannelBuffer frame) {
        this.frame = frame;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        try {
            return session.getRemoteAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isConnected() {
        return !session.isInvalid();
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
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {
        super.send(message, sent);
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer(1024);
        try {
            codec.encode(this, buffer, message);
            session.writeBuffer().write(buffer.array(), buffer.readerIndex(), buffer.readableBytes());
            session.writeBuffer().flush();
            getChannelHandler().sent(this, message);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
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
            attributes.clear();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            if (logger.isInfoEnabled()) {
                logger.info("Close smart-socket channel " + session);
            }
            session.close();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
    }
}
