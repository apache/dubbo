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
package com.alibaba.dubbo.remoting.transport;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ExecutorUtil;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Client;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.transport.handler.WrappedChannelHandler;

/**
 * AbstractClient
 * 
 * @author qian.lei
 * @author chao.liuc
 */
public abstract class AbstractClient extends AbstractEndpoint implements Client {
    
    private static final Logger logger = LoggerFactory.getLogger(AbstractClient.class);
    
    protected static final String CLIENT_THREAD_POOL_NAME  ="DubboClientHandler";
    
    private static final AtomicInteger CLIENT_THREAD_POOL_ID = new AtomicInteger();

    private final Lock            connectLock = new ReentrantLock();
    
    private static final ScheduledThreadPoolExecutor reconnectExecutorService = new ScheduledThreadPoolExecutor(2, new NamedThreadFactory("client-connect-check-timer", true));
    
    private volatile  ScheduledFuture<?> reconnectExecutorFuture = null;
    
    protected volatile ExecutorService executor;
    
    private final boolean send_reconnect ;
    
    //the last successed connected time
    private long lastConnectedTime = System.currentTimeMillis();
    
    private final int shutdown_timeout ;
    
    
    public AbstractClient(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
        
        send_reconnect = url.getParameter(Constants.SEND_RECONNECT_KEY, false);
        
        shutdown_timeout = url.getParameter(Constants.SHUTDOWN_TIMEOUT_KEY, Constants.DEFAULT_SHUTDOWN_TIMEOUT);
        
        try {
            doOpen();
        } catch (Throwable t) {
            close();
            throw new RemotingException(url.toInetSocketAddress(), null, 
                                        "Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress() 
                                        + " connect to the server " + getRemoteAddress() + ", cause: " + t.getMessage(), t);
        }
        try {
            // connect.
            connect();
            if (logger.isInfoEnabled()) {
                logger.info("Start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress() + " connect to the server " + getRemoteAddress());
            }
        } catch (RemotingException t) {
            if (url.getParameter(Constants.CHECK_KEY, true)) {
                close();
                throw t;
            } else {
                logger.error("Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress()
                             + " connect to the server " + getRemoteAddress() + " (check == false, ignore and retry later!), cause: " + t.getMessage(), t);
            }
        } catch (Throwable t){
            close();
            throw new RemotingException(url.toInetSocketAddress(), null, 
                    "Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress() 
                    + " connect to the server " + getRemoteAddress() + ", cause: " + t.getMessage(), t);
        }
        
        if (handler instanceof WrappedChannelHandler ){
            executor = ((WrappedChannelHandler)handler).getExecutor();
        }
    }
    
    /**
     * init reconnect thread
     */
    private synchronized void initConnectStatusCheckCommand(){
        //reconnect=false to close reconnect 
        int reconnect = getReconnectParam(getUrl());
        if(reconnect > 0 && reconnectExecutorFuture == null){
            Runnable connectStatusCheckCommand =  new Runnable() {
                String errorMsg = "Unexpected error occur at client reconnect";
                public void run() {
                    try {
                        if (! isConnected()) {
                            connect();
                        }
                    } catch (Throwable t) { 
                        // wait registry sync provider list
                        if (System.currentTimeMillis() - lastConnectedTime > shutdown_timeout){
                            logger.warn(errorMsg, t);
                        } else {
                            logger.error(errorMsg, t);
                        }
                    }
                }
            };
            reconnectExecutorFuture = reconnectExecutorService.scheduleWithFixedDelay(connectStatusCheckCommand, reconnect, reconnect, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * @param url
     * @return 0-false
     */
    private static int getReconnectParam(URL url){
        int reconnect ;
        String param = url.getParameter(Constants.RECONNECT_KEY);
        if (param == null || param.length()==0 || "true".equalsIgnoreCase(param)){
            reconnect = Constants.DEFAULT_RECONNECT_PERIOD;
        }else if ("false".equalsIgnoreCase(param)){
            reconnect = 0;
        } else {
            try{
                reconnect = Integer.parseInt(param);
            }catch (Exception e) {
                throw new IllegalArgumentException("reconnect param must be nonnegative integer or false/true. input is:"+param);
            }
            if(reconnect < 0){
                throw new IllegalArgumentException("reconnect param must be nonnegative integer or false/true. input is:"+param);
            }
        }
        return reconnect;
    }
    
    private synchronized void destroyConnectStatusCheckCommand(){
        try {
            if (reconnectExecutorFuture != null && ! reconnectExecutorFuture.isDone()){
                reconnectExecutorFuture.cancel(true);
                reconnectExecutorService.purge();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }
    
    protected ExecutorService createExecutor() {
        return Executors.newCachedThreadPool(new NamedThreadFactory(CLIENT_THREAD_POOL_NAME + CLIENT_THREAD_POOL_ID.incrementAndGet() + "-" + getUrl().getAddress(), true));
    }
    
    public InetSocketAddress getConnectAddress() {
        return new InetSocketAddress(NetUtils.filterLocalHost(getUrl().getHost()), getUrl().getPort());
    }

    public InetSocketAddress getRemoteAddress() {
        Channel channel = getChannel();
        if (channel == null)
            return getUrl().toInetSocketAddress();
        return channel.getRemoteAddress();
    }

    public InetSocketAddress getLocalAddress() {
        Channel channel = getChannel();
        if (channel == null)
            return InetSocketAddress.createUnresolved(NetUtils.getLocalHost(), 0);
        return channel.getLocalAddress();
    }

    public boolean isConnected() {
        Channel channel = getChannel();
        if (channel == null)
            return false;
        return channel.isConnected();
    }

    public Object getAttribute(String key) {
        Channel channel = getChannel();
        if (channel == null)
            return null;
        return channel.getAttribute(key);
    }

    public void setAttribute(String key, Object value) {
        Channel channel = getChannel();
        if (channel == null)
            return;
        channel.setAttribute(key, value);
    }

    public void removeAttribute(String key) {
        Channel channel = getChannel();
        if (channel == null)
            return;
        channel.removeAttribute(key);
    }

    public boolean hasAttribute(String key) {
        Channel channel = getChannel();
        if (channel == null)
            return false;
        return channel.hasAttribute(key);
    }
    
    public void send(Object message, boolean sent) throws RemotingException {
        if (send_reconnect && !isConnected()){
            connect();
        }
        Channel channel = getChannel();
        if (channel == null || ! channel.isConnected()) {
          throw new RemotingException(this, channel == null ? "channel is null " : (" channel is closed ") +". url:" + getUrl());
        }
        channel.send(message, sent);
    }
    
    private void connect() throws RemotingException {
        connectLock.lock();
        try {
            if (isConnected()) {
                return;
            }
            initConnectStatusCheckCommand();
            doConnect();
            if (! isConnected()) {
                throw new RemotingException(this, "Failed connect to server " + getRemoteAddress() + " from " + getClass().getSimpleName() + " "
                                            + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion()
                                            + ", cause: Connect wait timeout: " + getTimeout() + "ms.");
            }
            lastConnectedTime = System.currentTimeMillis();
        } catch (RemotingException e) {
            throw e;
        } catch (Throwable e) {
            throw new RemotingException(this, "Failed connect to server " + getRemoteAddress() + " from " + getClass().getSimpleName() + " "
                                        + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion()
                                        + ", cause: " + e.getMessage(), e);
        } finally {
            connectLock.unlock();
        }
    }

    public void disconnect() {
        connectLock.lock();
        try {
            destroyConnectStatusCheckCommand();
            try {
                Channel channel = getChannel();
                if (channel != null) {
                    channel.close();
                }
            } catch (Throwable e) {
                logger.warn(e.getMessage(), e);
            }
            try {
                doDisConnect();
            } catch (Throwable e) {
                logger.warn(e.getMessage(), e);
            }
        } finally {
            connectLock.unlock();
        }
    }
    
    public void reconnect() throws RemotingException {
        disconnect();
        connect();
    }
    public void close() {
        ExecutorUtil.shutdownNow(executor, 100);
        try {
            super.close();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        disconnect();
        try {
            doClose();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }

    public void close(int timeout) {
        ExecutorUtil.gracefulShutdown(executor ,timeout);
        close();
    }
    
    @Override
    public String toString() {
        return getClass().getName() + " [" + getLocalAddress() + " -> " + getRemoteAddress() + "]";
    }

    /**
     * Open client.
     * 
     * @throws Throwable
     */
    protected abstract void doOpen() throws Throwable;

    /**
     * Close client.
     * 
     * @throws Throwable
     */
    protected abstract void doClose() throws Throwable;

    /**
     * Connect to server.
     * 
     * @throws Throwable
     */
    protected abstract void doConnect() throws Throwable;
    
    /**
     * disConnect to server.
     * 
     * @throws Throwable
     */
    protected abstract void doDisConnect() throws Throwable;

    /**
     * Get the connected channel.
     * 
     * @return channel
     */
    protected abstract Channel getChannel();

}