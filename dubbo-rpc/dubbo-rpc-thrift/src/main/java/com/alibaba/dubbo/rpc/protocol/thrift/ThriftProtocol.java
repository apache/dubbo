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
package com.alibaba.dubbo.rpc.protocol.thrift;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.Transporter;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.remoting.exchange.ExchangeHandler;
import com.alibaba.dubbo.remoting.exchange.ExchangeServer;
import com.alibaba.dubbo.remoting.exchange.Exchangers;
import com.alibaba.dubbo.remoting.exchange.support.ExchangeHandlerAdapter;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProtocol;
import com.alibaba.dubbo.rpc.protocol.dubbo.DubboExporter;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThriftProtocol extends AbstractProtocol {

    public static final int DEFAULT_PORT = 40880;

    public static final String NAME = "thrift";

    // ip:port -> ExchangeServer
    private final ConcurrentMap<String, ExchangeServer> serverMap =
            new ConcurrentHashMap<String, ExchangeServer>();

    private ExchangeHandler handler = new ExchangeHandlerAdapter() {

        @Override
        public Object reply(ExchangeChannel channel, Object msg) throws RemotingException {

            if (msg instanceof Invocation) {
                Invocation inv = (Invocation) msg;
                String serviceName = inv.getAttachments().get(Constants.INTERFACE_KEY);
                String serviceKey = serviceKey(channel.getLocalAddress().getPort(),
                        serviceName, null, null);
                DubboExporter<?> exporter = (DubboExporter<?>) exporterMap.get(serviceKey);
                if (exporter == null) {
                    throw new RemotingException(channel,
                            "Not found exported service: "
                                    + serviceKey
                                    + " in "
                                    + exporterMap.keySet()
                                    + ", may be version or group mismatch "
                                    + ", channel: consumer: "
                                    + channel.getRemoteAddress()
                                    + " --> provider: "
                                    + channel.getLocalAddress()
                                    + ", message:" + msg);
                }

                RpcContext.getContext().setRemoteAddress(channel.getRemoteAddress());
                return exporter.getInvoker().invoke(inv);

            }

            throw new RemotingException(channel,
                    "Unsupported request: "
                            + (msg.getClass().getName() + ": " + msg)
                            + ", channel: consumer: "
                            + channel.getRemoteAddress()
                            + " --> provider: "
                            + channel.getLocalAddress());
        }

        @Override
        public void received(Channel channel, Object message) throws RemotingException {
            if (message instanceof Invocation) {
                reply((ExchangeChannel) channel, message);
            } else {
                super.received(channel, message);
            }
        }

    };

    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {

        // can use thrift codec only
        URL url = invoker.getUrl().addParameter(Constants.CODEC_KEY, ThriftCodec.NAME);
        // find server.
        String key = url.getAddress();
        // client can expose a service for server to invoke only.
        boolean isServer = url.getParameter(Constants.IS_SERVER_KEY, true);
        if (isServer && !serverMap.containsKey(key)) {
            serverMap.put(key, getServer(url));
        }
        // export service.
        key = serviceKey(url);
        DubboExporter<T> exporter = new DubboExporter<T>(invoker, key, exporterMap);
        exporterMap.put(key, exporter);

        return exporter;
    }

    @Override
    public void destroy() {

        super.destroy();

        for (String key : new ArrayList<String>(serverMap.keySet())) {

            ExchangeServer server = serverMap.remove(key);

            if (server != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Close dubbo server: " + server.getLocalAddress());
                    }
                    server.close(ConfigUtils.getServerShutdownTimeout());
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            } // ~ end of if ( server != null )

        } // ~ end of loop serverMap

    } // ~ end of method destroy

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {

        ThriftInvoker<T> invoker = new ThriftInvoker<T>(type, url, getClients(url), invokers);

        invokers.add(invoker);

        return invoker;

    }

    private ExchangeClient[] getClients(URL url) {

        int connections = url.getParameter(Constants.CONNECTIONS_KEY, 1);

        ExchangeClient[] clients = new ExchangeClient[connections];

        for (int i = 0; i < clients.length; i++) {
            clients[i] = initClient(url);
        }
        return clients;
    }

    private ExchangeClient initClient(URL url) {

        ExchangeClient client;

        url = url.addParameter(Constants.CODEC_KEY, ThriftCodec.NAME);

        try {
            client = Exchangers.connect(url);
        } catch (RemotingException e) {
            throw new RpcException("Fail to create remoting client for service(" + url
                    + "): " + e.getMessage(), e);
        }

        return client;

    }

    private ExchangeServer getServer(URL url) {
        // enable sending readonly event when server closes by default
        url = url.addParameterIfAbsent(Constants.CHANNEL_READONLYEVENT_SENT_KEY, Boolean.TRUE.toString());
        String str = url.getParameter(Constants.SERVER_KEY, Constants.DEFAULT_REMOTING_SERVER);

        if (str != null && str.length() > 0 && !ExtensionLoader.getExtensionLoader(Transporter.class).hasExtension(str))
            throw new RpcException("Unsupported server type: " + str + ", url: " + url);

        ExchangeServer server;
        try {
            server = Exchangers.bind(url, handler);
        } catch (RemotingException e) {
            throw new RpcException("Fail to start server(url: " + url + ") " + e.getMessage(), e);
        }
        str = url.getParameter(Constants.CLIENT_KEY);
        if (str != null && str.length() > 0) {
            Set<String> supportedTypes = ExtensionLoader.getExtensionLoader(Transporter.class).getSupportedExtensions();
            if (!supportedTypes.contains(str)) {
                throw new RpcException("Unsupported client type: " + str);
            }
        }
        return server;
    }

}
