/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.protocol.dubbo;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.Parameters;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.remoting.exchange.ExchangeHandler;
import com.alibaba.dubbo.remoting.exchange.Exchangers;
import com.alibaba.dubbo.remoting.exchange.ResponseFuture;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * dubbo protocol support class.
 */
@SuppressWarnings("deprecation")
final class LazyConnectExchangeClient implements ExchangeClient {

    // when this warning rises from invocation, program probably have bug.
    static final String REQUEST_WITH_WARNING_KEY = "lazyclient_request_with_warning";
    private final static Logger logger = LoggerFactory.getLogger(LazyConnectExchangeClient.class);

    //请求时，是否检查告警
    protected final boolean requestWithWarning;

    private final URL url;

    //通道处理器
    private final ExchangeHandler requestHandler;

    //连接锁
    private final Lock connectLock = new ReentrantLock();
    // lazy connect, initial state for connection

    //lazy connect 如果没有初始化时的连接状态
    private final boolean initialState;

    //通信客户端
    private volatile ExchangeClient client;

    //警告计数器。每超过一定次数，打印告警日志
    private AtomicLong warningcount = new AtomicLong(0);

    public LazyConnectExchangeClient(URL url, ExchangeHandler requestHandler) {
        // lazy connect, need set send.reconnect = true, to avoid channel bad status.
        this.url = url.addParameter(Constants.SEND_RECONNECT_KEY, Boolean.TRUE.toString());
        this.requestHandler = requestHandler;
        this.initialState = url.getParameter(Constants.LAZY_CONNECT_INITIAL_STATE_KEY, Constants.DEFAULT_LAZY_CONNECT_INITIAL_STATE);
        this.requestWithWarning = url.getParameter(REQUEST_WITH_WARNING_KEY, false);
    }


    //初始化客户端
    private void initClient() throws RemotingException {
        //已初始化，跳过
        if (client != null)
            return;
        if (logger.isInfoEnabled()) {
            logger.info("Lazy connect to " + url);
        }
        //获得锁
        connectLock.lock();
        try {
            //已初始化，跳过
            if (client != null)
                return;
            //创建client，连接服务器
            this.client = Exchangers.connect(url, requestHandler);
        } finally {
            //释放锁
            connectLock.unlock();
        }
    }

    public ResponseFuture request(Object request) throws RemotingException {
        warning(request);
        initClient();
        return client.request(request);
    }

    public URL getUrl() {
        return url;
    }

    public InetSocketAddress getRemoteAddress() {
        if (client == null) {
            return InetSocketAddress.createUnresolved(url.getHost(), url.getPort());
        } else {
            return client.getRemoteAddress();
        }
    }

    public ResponseFuture request(Object request, int timeout) throws RemotingException {
        warning(request);
        initClient();
        return client.request(request, timeout);
    }

    /**
     * If {@link #REQUEST_WITH_WARNING_KEY} is configured, then warn once every 5000 invocations.
     *
     * @param request
     */
    private void warning(Object request) {
        if (requestWithWarning) {
            if (warningcount.get() % 5000 == 0) {
                logger.warn(new IllegalStateException("safe guard client , should not be called ,must have a bug."));
            }
            warningcount.incrementAndGet();
        }
    }

    public ChannelHandler getChannelHandler() {
        checkClient();
        return client.getChannelHandler();
    }

    public boolean isConnected() {
        //客户端未初始化
        if (client == null) {
            return initialState;
        } else {
            return client.isConnected();
        }
    }

    public InetSocketAddress getLocalAddress() {
        if (client == null) {
            return InetSocketAddress.createUnresolved(NetUtils.getLocalHost(), 0);
        } else {
            return client.getLocalAddress();
        }
    }

    public ExchangeHandler getExchangeHandler() {
        return requestHandler;
    }

    public void send(Object message) throws RemotingException {
        initClient();
        client.send(message);
    }

    public void send(Object message, boolean sent) throws RemotingException {
        initClient();
        client.send(message, sent);
    }

    public boolean isClosed() {
        if (client != null)
            return client.isClosed();
        else
            return true;
    }

    public void close() {
        if (client != null)
            client.close();
    }

    public void close(int timeout) {
        if (client != null)
            client.close(timeout);
    }

    @Override
    public void startClose() {
        if (client != null) {
            client.startClose();
        }
    }

    public void reset(URL url) {
        checkClient();
        client.reset(url);
    }

    @Deprecated
    public void reset(Parameters parameters) {
        reset(getUrl().addParameters(parameters.getParameters()));
    }

    public void reconnect() throws RemotingException {
        checkClient();
        client.reconnect();
    }

    public Object getAttribute(String key) {
        if (client == null) {
            return null;
        } else {
            return client.getAttribute(key);
        }
    }

    public void setAttribute(String key, Object value) {
        checkClient();
        client.setAttribute(key, value);
    }

    public void removeAttribute(String key) {
        checkClient();
        client.removeAttribute(key);
    }

    public boolean hasAttribute(String key) {
        if (client == null) {
            return false;
        } else {
            return client.hasAttribute(key);
        }
    }

    private void checkClient() {
        if (client == null) {
            throw new IllegalStateException(
                    "LazyConnectExchangeClient state error. the client has not be init .url:" + url);
        }
    }
}