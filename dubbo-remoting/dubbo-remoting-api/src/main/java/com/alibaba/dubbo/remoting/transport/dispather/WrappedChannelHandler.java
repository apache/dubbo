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
package com.alibaba.dubbo.remoting.transport.dispather;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.threadpool.ThreadPool;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.Request;
import com.alibaba.dubbo.remoting.exchange.Response;
import com.alibaba.dubbo.remoting.exchange.support.header.HeaderExchangeHandler;
import com.alibaba.dubbo.remoting.transport.ChannelHandlerDelegate;

public class WrappedChannelHandler implements ChannelHandlerDelegate {
    
    protected static final Logger logger = LoggerFactory.getLogger(WrappedChannelHandler.class);

    protected static final ExecutorService SHARED_EXECUTOR = Executors.newCachedThreadPool(new NamedThreadFactory("DubboSharedHandler", true));
    
    protected final ExecutorService executor;
    
    protected final ChannelHandler handler;

    protected final URL url;
    
    public WrappedChannelHandler(ChannelHandler handler, URL url) {
        this.handler = handler;
        this.url = url;
        executor = (ExecutorService) ExtensionLoader.getExtensionLoader(ThreadPool.class).getAdaptiveExtension().getExecutor(url);
    }
    
    public void close() {
        try {
            if (executor instanceof ExecutorService) {
                ((ExecutorService)executor).shutdown();
            }
        } catch (Throwable t) {
            logger.warn("fail to destroy thread pool of server: " + t.getMessage(), t);
        }
    }

    public void connected(Channel channel) throws RemotingException {
        handler.connected(channel);
    }

    public void disconnected(Channel channel) throws RemotingException {
        handler.disconnected(channel);
    }

    public void sent(Channel channel, Object message) throws RemotingException {
        handler.sent(channel, message);
    }

    @SuppressWarnings("deprecation")
    public void received(Channel channel, Object message) throws RemotingException {
        if (message instanceof Request && ((Request)message).isHeartbeat()){
            Request req = (Request) message;
            if (req.isTwoWay()){
                Response res = new Response(req.getId(),req.getVersion());
                res.setHeartbeat(true);
                channel.send(res);
            }
        }
        handler.received(channel, message);
    }

    public void caught(Channel channel, Throwable exception) throws RemotingException {
        handler.caught(channel, exception);
    }
    
    public ExecutorService getExecutor() {
        return executor;
    }
    
    public ChannelHandler getHandler() {
        if (handler instanceof ChannelHandlerDelegate) {
            return ((ChannelHandlerDelegate) handler).getHandler();
        } else {
            return handler;
        }
    }
    
    public URL getUrl() {
        return url;
    }

    protected final boolean isHeartbeatResponse(Object message) {
        return (message instanceof Response) && ((Response)message).isHeartbeat();
    }

    protected void setReadTimestamp(Channel channel) {
        channel.setAttribute(
            HeaderExchangeHandler.KEY_READ_TIMESTAMP, System.currentTimeMillis());
    }
}