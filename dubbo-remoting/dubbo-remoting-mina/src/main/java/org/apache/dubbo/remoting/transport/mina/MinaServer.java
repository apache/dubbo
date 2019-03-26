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
package org.apache.dubbo.remoting.transport.mina;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.transport.AbstractServer;
import org.apache.dubbo.remoting.transport.dispatcher.ChannelHandlers;

import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * MinaServer
 */
public class MinaServer extends AbstractServer {

    private static final Logger logger = LoggerFactory.getLogger(MinaServer.class);

    private SocketAcceptor acceptor;

    public MinaServer(URL url, ChannelHandler handler) throws RemotingException {
        super(url, ChannelHandlers.wrap(handler, ExecutorUtil.setThreadName(url, SERVER_THREAD_POOL_NAME)));
    }

    @Override
    protected void doOpen() throws Throwable {
        // set thread pool.
        acceptor = new SocketAcceptor(getUrl().getPositiveParameter(Constants.IO_THREADS_KEY, Constants.DEFAULT_IO_THREADS),
                Executors.newCachedThreadPool(new NamedThreadFactory("MinaServerWorker",
                        true)));
        // config
        SocketAcceptorConfig cfg = (SocketAcceptorConfig) acceptor.getDefaultConfig();
        cfg.setThreadModel(ThreadModel.MANUAL);
        // set codec.
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MinaCodecAdapter(getCodec(), getUrl(), this)));

        acceptor.bind(getBindAddress(), new MinaHandler(getUrl(), this));
    }

    @Override
    protected void doClose() throws Throwable {
        try {
            if (acceptor != null) {
                acceptor.unbind(getBindAddress());
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }

    @Override
    public Collection<Channel> getChannels() {
        Set<IoSession> sessions = acceptor.getManagedSessions(getBindAddress());
        Collection<Channel> channels = new HashSet<Channel>();
        for (IoSession session : sessions) {
            if (session.isConnected()) {
                channels.add(MinaChannel.getOrAddChannel(session, getUrl(), this));
            }
        }
        return channels;
    }

    @Override
    public Channel getChannel(InetSocketAddress remoteAddress) {
        Set<IoSession> sessions = acceptor.getManagedSessions(getBindAddress());
        for (IoSession session : sessions) {
            if (session.getRemoteAddress().equals(remoteAddress)) {
                return MinaChannel.getOrAddChannel(session, getUrl(), this);
            }
        }
        return null;
    }

    @Override
    public boolean isBound() {
        return acceptor.isManaged(getBindAddress());
    }

}
