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
package com.alibaba.dubbo.remoting.transport.dispatcher;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.store.DataStore;
import com.alibaba.dubbo.common.threadpool.ThreadPool;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.transport.ChannelHandlerDelegate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.alibaba.dubbo.common.Constants.SHARED_CONSUMER_EXECUTOR_PORT;
import static com.alibaba.dubbo.common.Constants.SHARE_EXECUTOR_KEY;

public class WrappedChannelHandler implements ChannelHandlerDelegate {

    protected static final Logger logger = LoggerFactory.getLogger(WrappedChannelHandler.class);

    protected static final ExecutorService SHARED_EXECUTOR = Executors.newCachedThreadPool(new NamedThreadFactory("DubboSharedHandler", true));

    protected final ExecutorService executor;

    protected final ChannelHandler handler;

    protected final URL url;

    protected DataStore dataStore = ExtensionLoader.getExtensionLoader(DataStore.class).getDefaultExtension();

    public WrappedChannelHandler(ChannelHandler handler, URL url) {
        this.handler = handler;
        this.url = url;

        String componentKey;
        if (Constants.CONSUMER_SIDE.equalsIgnoreCase(url.getParameter(Constants.SIDE_KEY))) {
            componentKey = Constants.CONSUMER_SIDE;
            if (url.getParameter(SHARE_EXECUTOR_KEY, false)) {
                ExecutorService cExecutor = (ExecutorService) dataStore.get(componentKey, SHARED_CONSUMER_EXECUTOR_PORT);
                if (cExecutor == null) {
                    cExecutor = (ExecutorService) ExtensionLoader.getExtensionLoader(ThreadPool.class).getAdaptiveExtension().getExecutor(url);
                    dataStore.put(componentKey, SHARED_CONSUMER_EXECUTOR_PORT, cExecutor);
                    cExecutor = (ExecutorService) dataStore.get(componentKey, SHARED_CONSUMER_EXECUTOR_PORT);
                }
                executor = cExecutor;
            } else {
                executor = (ExecutorService) ExtensionLoader.getExtensionLoader(ThreadPool.class).getAdaptiveExtension().getExecutor(url);
                dataStore.put(componentKey, Integer.toString(url.getPort()), executor);
            }
        } else {
            componentKey = Constants.EXECUTOR_SERVICE_COMPONENT_KEY;
            executor = (ExecutorService) ExtensionLoader.getExtensionLoader(ThreadPool.class).getAdaptiveExtension().getExecutor(url);
            dataStore.put(componentKey, Integer.toString(url.getPort()), executor);
        }
    }

    public void close() {
        try {
            if (executor != null) {
                executor.shutdown();
            }
        } catch (Throwable t) {
            logger.warn("fail to destroy thread pool of server: " + t.getMessage(), t);
        }
    }

    @Override
    public void connected(Channel channel) throws RemotingException {
        handler.connected(channel);
    }

    @Override
    public void disconnected(Channel channel) throws RemotingException {
        handler.disconnected(channel);
    }

    @Override
    public void sent(Channel channel, Object message) throws RemotingException {
        handler.sent(channel, message);
    }

    @Override
    public void received(Channel channel, Object message) throws RemotingException {
        handler.received(channel, message);
    }

    @Override
    public void caught(Channel channel, Throwable exception) throws RemotingException {
        handler.caught(channel, exception);
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    @Override
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

    public ExecutorService getExecutorService() {
        ExecutorService cexecutor = executor;
        if (cexecutor == null || cexecutor.isShutdown()) {
            cexecutor = SHARED_EXECUTOR;
        }
        return cexecutor;
    }

}
