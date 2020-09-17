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
package org.apache.dubbo.rpc.protocol.thrift;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.RemotingServer;
import org.apache.dubbo.remoting.Transporter;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.exchange.ExchangeClient;
import org.apache.dubbo.remoting.exchange.ExchangeHandler;
import org.apache.dubbo.remoting.exchange.ExchangeServer;
import org.apache.dubbo.remoting.exchange.Exchangers;
import org.apache.dubbo.remoting.exchange.support.ExchangeHandlerAdapter;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.AbstractProtocol;
import org.apache.dubbo.rpc.protocol.dubbo.DubboExporter;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.remoting.Constants.CHANNEL_READONLYEVENT_SENT_KEY;
import static org.apache.dubbo.remoting.Constants.CLIENT_KEY;
import static org.apache.dubbo.remoting.Constants.CODEC_KEY;
import static org.apache.dubbo.remoting.Constants.CONNECTIONS_KEY;
import static org.apache.dubbo.remoting.Constants.SERVER_KEY;
import static org.apache.dubbo.rpc.Constants.IS_SERVER_KEY;

/**
 * @since 2.7.0, use https://github.com/dubbo/dubbo-rpc-native-thrift instead
 */
@Deprecated
public class ThriftProtocol extends AbstractProtocol {

    public static final int DEFAULT_PORT = 40880;

    public static final String NAME = "thrift";

    private ExchangeHandler handler = new ExchangeHandlerAdapter() {

        @Override
        public CompletableFuture<Object> reply(ExchangeChannel channel, Object msg) throws RemotingException {

            if (msg instanceof Invocation) {
                Invocation inv = (Invocation) msg;
                String path = (String) inv.getObjectAttachments().get(PATH_KEY);
                String serviceKey = serviceKey(channel.getLocalAddress().getPort(),
                        path, null, null);
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

                Result result = exporter.getInvoker().invoke(inv);
                return result.thenApply(Function.identity());
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
        URL url = invoker.getUrl().addParameter(CODEC_KEY, ThriftCodec.NAME);
        // find server.
        String key = url.getAddress();
        // client can expose a service for server to invoke only.
        boolean isServer = url.getParameter(IS_SERVER_KEY, true);
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

            ProtocolServer protocolServer = serverMap.remove(key);

            if (protocolServer != null) {
                RemotingServer server = protocolServer.getRemotingServer();
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Close dubbo server: " + server.getLocalAddress());
                    }
                    server.close(ConfigurationUtils.getServerShutdownTimeout());
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            } // ~ end of if ( server != null )

        } // ~ end of loop serverMap

    } // ~ end of method destroy

    @Override
    protected <T> Invoker<T> protocolBindingRefer(Class<T> type, URL url) throws RpcException {

        ThriftInvoker<T> invoker = new ThriftInvoker<T>(type, url, getClients(url), invokers);

        invokers.add(invoker);

        return invoker;

    }

    private ExchangeClient[] getClients(URL url) {

        int connections = url.getParameter(CONNECTIONS_KEY, 1);

        ExchangeClient[] clients = new ExchangeClient[connections];

        for (int i = 0; i < clients.length; i++) {
            clients[i] = initClient(url);
        }
        return clients;
    }

    private ExchangeClient initClient(URL url) {

        ExchangeClient client;

//        url = url.addParameter(CODEC_KEY, ThriftCodec.NAME);

        try {
            client = Exchangers.connect(url);
        } catch (RemotingException e) {
            throw new RpcException("Fail to create remoting client for service(" + url
                    + "): " + e.getMessage(), e);
        }

        return client;

    }

    private ProtocolServer getServer(URL url) {
        // enable sending readonly event when server closes by default
        url = url.addParameterIfAbsent(CHANNEL_READONLYEVENT_SENT_KEY, Boolean.TRUE.toString());
        String str = url.getParameter(SERVER_KEY, org.apache.dubbo.rpc.Constants.DEFAULT_REMOTING_SERVER);

        if (str != null && str.length() > 0 && !ExtensionLoader.getExtensionLoader(Transporter.class).hasExtension(str)) {
            throw new RpcException("Unsupported server type: " + str + ", url: " + url);
        }

        ExchangeServer server;
        try {
            server = Exchangers.bind(url, handler);
        } catch (RemotingException e) {
            throw new RpcException("Fail to start server(url: " + url + ") " + e.getMessage(), e);
        }
        str = url.getParameter(CLIENT_KEY);
        if (str != null && str.length() > 0) {
            Set<String> supportedTypes = ExtensionLoader.getExtensionLoader(Transporter.class).getSupportedExtensions();
            if (!supportedTypes.contains(str)) {
                throw new RpcException("Unsupported client type: " + str);
            }
        }
        return new ThriftProtocolServer(server);
    }

    private class ThriftProtocolServer implements ProtocolServer {

        private ExchangeServer server;
        private String address;

        public ThriftProtocolServer(ExchangeServer server) {
            this.server = server;
        }

        @Override
        public RemotingServer getRemotingServer() {
            return server;
        }

        @Override
        public String getAddress() {
            return StringUtils.isNotEmpty(address) ? address : server.getUrl().getAddress();
        }

        @Override
        public void setAddress(String address) {
            this.address = address;
        }

        @Override
        public URL getUrl() {
            return server.getUrl();
        }

        @Override
        public void reset(URL url) {
            server.reset(url);
        }

        @Override
        public void close() {
            server.close();
        }
    }

}
