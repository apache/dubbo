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
package com.alibaba.dubbo.remoting.transport.grizzly;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.transport.AbstractServer;

/**
 * GrizzlyServer
 * 
 * @author william.liangf
 */
public class GrizzlyServer extends AbstractServer {
    
    private static final Logger logger = LoggerFactory.getLogger(GrizzlyServer.class);

    private final Map<String, Channel> channels = new ConcurrentHashMap<String, Channel>(); // <ip:port, channel>
    
    private TCPNIOTransport transport;

    public GrizzlyServer(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
    }

    @Override
    protected void doOpen() throws Throwable {
        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        filterChainBuilder.add(new TransportFilter());
        
        filterChainBuilder.add(new GrizzlyCodecAdapter(getCodec(), getUrl(), this));
        filterChainBuilder.add(new GrizzlyHandler(getUrl(), this));
        TCPNIOTransportBuilder builder = TCPNIOTransportBuilder.newInstance();
        ThreadPoolConfig config = builder.getWorkerThreadPoolConfig(); 
        config.setPoolName(SERVER_THREAD_POOL_NAME).setQueueLimit(-1);
        String threadpool = getUrl().getParameter(Constants.THREADPOOL_KEY, Constants.DEFAULT_THREADPOOL);
        if (Constants.DEFAULT_THREADPOOL.equals(threadpool)) {
            int threads = getUrl().getPositiveParameter(Constants.THREADS_KEY, Constants.DEFAULT_THREADS);
            config.setCorePoolSize(threads).setMaxPoolSize(threads)
                .setKeepAliveTime(0L, TimeUnit.SECONDS); 
        } else if ("cached".equals(threadpool)) {
            int threads = getUrl().getPositiveParameter(Constants.THREADS_KEY, Integer.MAX_VALUE);
            config.setCorePoolSize(0).setMaxPoolSize(threads)
                .setKeepAliveTime(60L, TimeUnit.SECONDS);
        } else {
            throw new IllegalArgumentException("Unsupported threadpool type " + threadpool);
        }
        builder.setKeepAlive(true).setReuseAddress(false)
                .setIOStrategy(SameThreadIOStrategy.getInstance());
        transport = builder.build();
        transport.setProcessor(filterChainBuilder.build());
        transport.bind(getBindAddress());
        transport.start();
    }

    @Override
    protected void doClose() throws Throwable {
        try {
            transport.stop();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }

    public boolean isBound() {
        return ! transport.isStopped();
    }

    public Collection<Channel> getChannels() {
        return channels.values();
    }

    public Channel getChannel(InetSocketAddress remoteAddress) {
        return channels.get(NetUtils.toAddressString(remoteAddress));
    }

    @Override
    public void connected(Channel ch) throws RemotingException {
        channels.put(NetUtils.toAddressString(ch.getRemoteAddress()), ch);
        super.connected(ch);
    }

    @Override
    public void disconnected(Channel ch) throws RemotingException {
        channels.remove(NetUtils.toAddressString(ch.getRemoteAddress()));
        super.disconnected(ch);
    }

}