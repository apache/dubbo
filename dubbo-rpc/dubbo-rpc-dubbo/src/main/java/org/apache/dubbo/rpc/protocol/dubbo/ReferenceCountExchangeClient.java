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
package org.apache.dubbo.rpc.protocol.dubbo;


import org.apache.dubbo.common.Parameters;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ExchangeClient;
import org.apache.dubbo.remoting.exchange.ExchangeHandler;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * dubbo protocol support class.
 */
@SuppressWarnings("deprecation")
class ReferenceCountExchangeClient implements ExchangeClient {

    private static final Logger logger = LoggerFactory.getLogger(ReferenceCountExchangeClient.class);
    private final AtomicInteger referenceCount = new AtomicInteger(0);
    private ExchangeClient client;

    public ReferenceCountExchangeClient(ExchangeClient client) {
        this.client = client;
        referenceCount.incrementAndGet();
    }

    protected ExchangeClient getExchangeClient(){
        return this.client;
    }

    protected void setExchangClient(ExchangeClient client){
        this.client = client;
    }

    @Override
    public void reset(URL url) {
        getExchangeClient().reset(url);
    }

    @Override
    public CompletableFuture<Object> request(Object request) throws RemotingException {
        return getExchangeClient().request(request);
    }

    @Override
    public URL getUrl() {
        return getExchangeClient().getUrl();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return getExchangeClient().getRemoteAddress();
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return getExchangeClient().getChannelHandler();
    }

    @Override
    public CompletableFuture<Object> request(Object request, int timeout) throws RemotingException {
        return getExchangeClient().request(request, timeout);
    }

    @Override
    public CompletableFuture<Object> request(Object request, ExecutorService executor) throws RemotingException {
        return getExchangeClient().request(request, executor);
    }

    @Override
    public CompletableFuture<Object> request(Object request, int timeout, ExecutorService executor) throws RemotingException {
        return getExchangeClient().request(request, timeout, executor);
    }

    @Override
    public boolean isConnected() {
        return getExchangeClient().isConnected();
    }

    @Override
    public void reconnect() throws RemotingException {
        getExchangeClient().reconnect();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return getExchangeClient().getLocalAddress();
    }

    @Override
    public boolean hasAttribute(String key) {
        return getExchangeClient().hasAttribute(key);
    }

    @Override
    public void reset(Parameters parameters) {
        getExchangeClient().reset(parameters);
    }

    @Override
    public void send(Object message) throws RemotingException {
        getExchangeClient().send(message);
    }

    @Override
    public ExchangeHandler getExchangeHandler() {
        return getExchangeClient().getExchangeHandler();
    }

    @Override
    public Object getAttribute(String key) {
        return getExchangeClient().getAttribute(key);
    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {
        getExchangeClient().send(message, sent);
    }

    @Override
    public void setAttribute(String key, Object value) {
        getExchangeClient().setAttribute(key, value);
    }

    @Override
    public void removeAttribute(String key) {
        getExchangeClient().removeAttribute(key);
    }

    /**
     * close() is not idempotent any longer
     */
    @Override
    public void close() {
        close(0);
    }

    @Override
    public void close(int timeout) {
        closeInternal(timeout, false);
    }

    @Override
    public void closeAll(int timeout) {
        closeInternal(timeout, true);
    }

    /**
     * when destroy unused invoker, closeAll should be true
     *
     * @param timeout
     * @param closeAll
     */
    protected boolean closeInternal(int timeout, boolean closeAll) {
        if (closeAll || refCountDec() <= 0) {
            closeImpl(timeout);
            client = null;
            return true;
        }
        return false;
    }

    protected void closeImpl(int timeout){
        ExchangeClient exchangeClient = getExchangeClient();
        if (timeout == 0) {
            exchangeClient.close();
        } else {
            exchangeClient.close(timeout);
        }
    }

    @Override
    public void startClose() {
        getExchangeClient().startClose();
    }

    @Override
    public boolean isClosed() {
        return getExchangeClient().isClosed();
    }

    /**
     * The reference count of current ExchangeClient, connection will be closed if all invokers destroyed.
     */
    public boolean addRef() {
        if(isClosed()){
            return false;
        }
        return refCountInc() > 0;
    }

    protected int refCountInc(){
        return referenceCount.getAndIncrement();
    }

    protected int refCountDec(){
        return referenceCount.decrementAndGet();
    }

    public int getCount() {
        return referenceCount.get();
    }
}

