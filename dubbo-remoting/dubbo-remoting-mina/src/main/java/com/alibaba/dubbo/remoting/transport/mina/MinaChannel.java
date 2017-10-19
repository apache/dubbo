/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.remoting.transport.mina;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.transport.AbstractChannel;

import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;

import java.net.InetSocketAddress;

/**
 * MinaChannel
 *
 * @author qian.lei
 * @author william.liangf
 */
final class MinaChannel extends AbstractChannel {

    private static final Logger logger = LoggerFactory.getLogger(MinaChannel.class);

    private static final String CHANNEL_KEY = MinaChannel.class.getName() + ".CHANNEL";

    private final IoSession session;

    private MinaChannel(IoSession session, URL url, ChannelHandler handler) {
        super(url, handler);
        if (session == null) {
            throw new IllegalArgumentException("mina session == null");
        }
        this.session = session;
    }

    static MinaChannel getOrAddChannel(IoSession session, URL url, ChannelHandler handler) {
        if (session == null) {
            return null;
        }
        MinaChannel ret = (MinaChannel) session.getAttribute(CHANNEL_KEY);
        if (ret == null) {
            ret = new MinaChannel(session, url, handler);
            if (session.isConnected()) {
                MinaChannel old = (MinaChannel) session.setAttribute(CHANNEL_KEY, ret);
                if (old != null) {
                    session.setAttribute(CHANNEL_KEY, old);
                    ret = old;
                }
            }
        }
        return ret;
    }

    static void removeChannelIfDisconnectd(IoSession session) {
        if (session != null && !session.isConnected()) {
            session.removeAttribute(CHANNEL_KEY);
        }
    }

    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) session.getLocalAddress();
    }

    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) session.getRemoteAddress();
    }

    public boolean isConnected() {
        return session.isConnected();
    }

    public void send(Object message, boolean sent) throws RemotingException {
        super.send(message, sent);

        boolean success = true;
        int timeout = 0;
        try {
            WriteFuture future = session.write(message);
            if (sent) {
                timeout = getUrl().getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
                success = future.join(timeout);
            }
        } catch (Throwable e) {
            throw new RemotingException(this, "Failed to send message " + message + " to " + getRemoteAddress() + ", cause: " + e.getMessage(), e);
        }

        if (!success) {
            throw new RemotingException(this, "Failed to send message " + message + " to " + getRemoteAddress()
                    + "in timeout(" + timeout + "ms) limit");
        }
    }

    public void close() {
        try {
            super.close();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            removeChannelIfDisconnectd(session);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            if (logger.isInfoEnabled()) {
                logger.info("CLose mina channel " + session);
            }
            session.close();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
    }

    public boolean hasAttribute(String key) {
        return session.containsAttribute(key);
    }

    public Object getAttribute(String key) {
        return session.getAttribute(key);
    }

    public void setAttribute(String key, Object value) {
        session.setAttribute(key, value);
    }

    public void removeAttribute(String key) {
        session.removeAttribute(key);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((session == null) ? 0 : session.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        MinaChannel other = (MinaChannel) obj;
        if (session == null) {
            if (other.session != null) return false;
        } else if (!session.equals(other.session)) return false;
        return true;
    }

    @Override
    public String toString() {
        return "MinaChannel [session=" + session + "]";
    }

}