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
package org.apache.dubbo.remoting.exchange;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.RemotingServer;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.remoting.transport.ChannelHandlerAdapter;
import org.apache.dubbo.remoting.transport.netty4.NettyHttp3ConnectionClient;
import org.apache.dubbo.remoting.transport.netty4.NettyHttp3Server;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Http3Exchanger {

    private static final ErrorTypeAwareLogger LOG = LoggerFactory.getErrorTypeAwareLogger(Http3Exchanger.class);
    private static final Map<String, RemotingServer> SERVERS = new ConcurrentHashMap<>();
    private static final Map<String, AbstractConnectionClient> CLIENTS = new ConcurrentHashMap<>(16);
    private static final ChannelHandler HANDLER = new ChannelHandlerAdapter();

    private Http3Exchanger() {}

    public static RemotingServer bind(URL url) {
        return SERVERS.computeIfAbsent(url.getAddress(), addr -> {
            try {
                return new NettyHttp3Server(url, HANDLER);
            } catch (RemotingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static AbstractConnectionClient connect(URL url) {
        return CLIENTS.compute(url.getAddress(), (address, client) -> {
            try {
                if (client == null) {
                    AbstractConnectionClient connectionClient = new NettyHttp3ConnectionClient(url, HANDLER);
                    connectionClient.addCloseListener(() -> CLIENTS.remove(address, connectionClient));
                    client = connectionClient;
                } else {
                    client.retain();
                }
                return client;
            } catch (RemotingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void close() {
        if (SERVERS.isEmpty()) {
            return;
        }
        ArrayList<RemotingServer> toClose = new ArrayList<>(SERVERS.values());
        SERVERS.clear();
        for (RemotingServer server : toClose) {
            try {
                server.close();
            } catch (Throwable t) {
                LOG.error(LoggerCodeConstants.PROTOCOL_ERROR_CLOSE_SERVER, "", "", "Close Http3 server failed", t);
            }
        }
    }
}
