/**
 * File Created at 2011-12-06
 * $Id$
 * <p>
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 * <p>
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.rpc.protocol.thrift;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
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

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">gang.lvg</a>
 */
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

    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {

        // 只能使用 thrift codec
        URL url = invoker.getUrl().addParameter(Constants.CODEC_KEY, ThriftCodec.NAME);
        // find server.
        String key = url.getAddress();
        //client 也可以暴露一个只有server可以调用的服务。
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

    public void destroy() {

        super.destroy();

        for (String key : new ArrayList<String>(serverMap.keySet())) {

            ExchangeServer server = serverMap.remove(key);

            if (server != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Close dubbo server: " + server.getLocalAddress());
                    }
                    server.close(getServerShutdownTimeout());
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            } // ~ end of if ( server != null )

        } // ~ end of loop serverMap

    } // ~ end of method destroy

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
        //默认开启server关闭时发送readonly事件
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
