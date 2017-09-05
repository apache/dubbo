/**
 * File Created at 2011-12-06
 * $Id$
 * <p>
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 * <p>
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.rpc.protocol.thrift;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;

import java.net.InetSocketAddress;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">gang.lvg</a>
 */
public class MockedChannel implements Channel {

    private URL url;

    public MockedChannel(URL url) {
        this.url = url;
    }

    public InetSocketAddress getRemoteAddress() {

        return null;
    }

    public boolean isConnected() {

        return false;
    }

    public boolean hasAttribute(String key) {

        return false;
    }

    public Object getAttribute(String key) {

        return null;
    }

    public void setAttribute(String key, Object value) {

    }

    public void removeAttribute(String key) {

    }

    public URL getUrl() {
        return url;
    }

    public ChannelHandler getChannelHandler() {

        return null;
    }

    public InetSocketAddress getLocalAddress() {

        return null;
    }

    public void send(Object message) throws RemotingException {

    }

    public void send(Object message, boolean sent) throws RemotingException {

    }

    public void close() {

    }

    public void close(int timeout) {

    }

    @Override
    public void startClose() {

    }

    public boolean isClosed() {

        return false;
    }

}
