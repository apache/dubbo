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
package com.alibaba.dubbo.rpc.protocol.dubbo;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.dubbo.common.Parameters;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.remoting.exchange.ExchangeHandler;
import com.alibaba.dubbo.remoting.exchange.ResponseFuture;

/**
 * dubbo protocol support class.
 * 
 * @author chao.liuc
 */
@SuppressWarnings("deprecation")
final class ReferenceCountExchangeClient implements ExchangeClient {

    private ExchangeClient client;
    
    private final URL url;
    
//    private final ExchangeHandler handler;
    
    private final AtomicInteger refenceCount = new AtomicInteger(0);
    
    private final Map<String, ExchangeClient> clientMap;
    
    public ReferenceCountExchangeClient(ExchangeClient client, Map<String, ExchangeClient> clientMap) {
        this.client = client;
        refenceCount.incrementAndGet();
        this.clientMap = clientMap;
        this.url = client.getUrl();
//        this.handler = client.getExchangeHandler();
    }

    public void reset(URL url) {
        client.reset(url);
    }

    public ResponseFuture request(Object request) throws RemotingException {
        return client.request(request);
    }

    public URL getUrl() {
        return client.getUrl();
    }

    public InetSocketAddress getRemoteAddress() {
        return client.getRemoteAddress();
    }

    public ChannelHandler getChannelHandler() {
        return client.getChannelHandler();
    }

    public ResponseFuture request(Object request, int timeout) throws RemotingException {
        return client.request(request, timeout);
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    public void reconnect() throws RemotingException {
        client.reconnect();
    }

    public InetSocketAddress getLocalAddress() {
        return client.getLocalAddress();
    }

    public boolean hasAttribute(String key) {
        return client.hasAttribute(key);
    }

    public void reset(Parameters parameters) {
        client.reset(parameters);
    }

    public void send(Object message) throws RemotingException {
        client.send(message);
    }

    public ExchangeHandler getExchangeHandler() {
        return client.getExchangeHandler();
    }

    public Object getAttribute(String key) {
        return client.getAttribute(key);
    }

    public void send(Object message, boolean sent) throws RemotingException {
        client.send(message, sent);
    }

    public void setAttribute(String key, Object value) {
        client.setAttribute(key, value);
    }

    public void removeAttribute(String key) {
        client.removeAttribute(key);
    }
    /* 
     * close方法将不再幂等,调用需要注意.
     */
    public void close() {
        if (refenceCount.decrementAndGet() <= 0){
            String key = client.getUrl().getAddress();
            clientMap.remove(key);
            client.close();
            replaceWithLazyClient();
        }
    }

    public void close(int timeout) {
        if (refenceCount.decrementAndGet() <= 0){
            clientMap.remove(url.getAddress());
            client.close(timeout);
            replaceWithLazyClient();
        }
    }
    
    private void replaceWithLazyClient(){
        //FIXME close?
//        client = new LazyConnectExchangeClient(url, client.getExchangeHandler());
    }

    public boolean isClosed() {
        return client.isClosed();
    }
}