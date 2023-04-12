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
package org.apache.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.NetUtils;
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
import org.apache.dubbo.remoting.exchange.PortUnificationExchanger;
import org.apache.dubbo.remoting.exchange.support.ExchangeHandlerAdapter;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.protocol.AbstractProtocol;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.LAZY_CONNECT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.STUB_EVENT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_ERROR_CLOSE_SERVER;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_REFER_INVOKER;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_UNSUPPORTED;
import static org.apache.dubbo.remoting.Constants.CHANNEL_READONLYEVENT_SENT_KEY;
import static org.apache.dubbo.remoting.Constants.CLIENT_KEY;
import static org.apache.dubbo.remoting.Constants.CODEC_KEY;
import static org.apache.dubbo.remoting.Constants.CONNECTIONS_KEY;
import static org.apache.dubbo.remoting.Constants.DEFAULT_HEARTBEAT;
import static org.apache.dubbo.remoting.Constants.DEFAULT_REMOTING_CLIENT;
import static org.apache.dubbo.remoting.Constants.HEARTBEAT_KEY;
import static org.apache.dubbo.remoting.Constants.SERVER_KEY;
import static org.apache.dubbo.rpc.Constants.DEFAULT_REMOTING_SERVER;
import static org.apache.dubbo.rpc.Constants.DEFAULT_STUB_EVENT;
import static org.apache.dubbo.rpc.Constants.IS_SERVER_KEY;
import static org.apache.dubbo.rpc.Constants.STUB_EVENT_METHODS_KEY;
import static org.apache.dubbo.rpc.protocol.dubbo.Constants.CALLBACK_SERVICE_KEY;
import static org.apache.dubbo.rpc.protocol.dubbo.Constants.DEFAULT_SHARE_CONNECTIONS;
import static org.apache.dubbo.rpc.protocol.dubbo.Constants.IS_CALLBACK_SERVICE;
import static org.apache.dubbo.rpc.protocol.dubbo.Constants.ON_CONNECT_KEY;
import static org.apache.dubbo.rpc.protocol.dubbo.Constants.ON_DISCONNECT_KEY;
import static org.apache.dubbo.rpc.protocol.dubbo.Constants.SHARE_CONNECTIONS_KEY;


/**
 * dubbo protocol support.
 */
public class DubboProtocol extends AbstractProtocol {

    public static final String NAME = "dubbo";

    public static final int DEFAULT_PORT = 20880;

    private static final String IS_CALLBACK_SERVICE_INVOKE = "_isCallBackServiceInvoke";

    /**
     * <host:port,Exchanger>
     * Map<String, List<ReferenceCountExchangeClient>
     */
    private final Map<String, SharedClientsProvider> referenceClientMap = new ConcurrentHashMap<>();

    private final AtomicBoolean destroyed = new AtomicBoolean();

    private final ExchangeHandler requestHandler;

    public DubboProtocol(FrameworkModel frameworkModel) {
        requestHandler = new ExchangeHandlerAdapter(frameworkModel) {

            @Override
            public CompletableFuture<Object> reply(ExchangeChannel channel, Object message) throws RemotingException {

                if (!(message instanceof Invocation)) {
                    throw new RemotingException(channel, "Unsupported request: "
                        + (message == null ? null : (message.getClass().getName() + ": " + message))
                        + ", channel: consumer: " + channel.getRemoteAddress() + " --> provider: " + channel.getLocalAddress());
                }

                Invocation inv = (Invocation) message;
                Invoker<?> invoker = inv.getInvoker() == null ? getInvoker(channel, inv) : inv.getInvoker();
                // switch TCCL
                if (invoker.getUrl().getServiceModel() != null) {
                    Thread.currentThread().setContextClassLoader(invoker.getUrl().getServiceModel().getClassLoader());
                }
                // need to consider backward-compatibility if it's a callback
                if (Boolean.TRUE.toString().equals(inv.getObjectAttachmentWithoutConvert(IS_CALLBACK_SERVICE_INVOKE))) {
                    String methodsStr = invoker.getUrl().getParameters().get("methods");
                    boolean hasMethod = false;
                    if (methodsStr == null || !methodsStr.contains(",")) {
                        hasMethod = inv.getMethodName().equals(methodsStr);
                    } else {
                        String[] methods = methodsStr.split(",");
                        for (String method : methods) {
                            if (inv.getMethodName().equals(method)) {
                                hasMethod = true;
                                break;
                            }
                        }
                    }
                    if (!hasMethod) {
                        logger.warn(PROTOCOL_FAILED_REFER_INVOKER, "", "", new IllegalStateException("The methodName " + inv.getMethodName()
                            + " not found in callback service interface ,invoke will be ignored."
                            + " please update the api interface. url is:"
                            + invoker.getUrl()) + " ,invocation is :" + inv);
                        return null;
                    }
                }
                RpcContext.getServiceContext().setRemoteAddress(channel.getRemoteAddress());
                Result result = invoker.invoke(inv);
                return result.thenApply(Function.identity());
            }

            @Override
            public void received(Channel channel, Object message) throws RemotingException {
                if (message instanceof Invocation) {
                    reply((ExchangeChannel) channel, message);

                } else {
                    super.received(channel, message);
                }
            }

            @Override
            public void connected(Channel channel) throws RemotingException {
                invoke(channel, ON_CONNECT_KEY);
            }

            @Override
            public void disconnected(Channel channel) throws RemotingException {
                if (logger.isDebugEnabled()) {
                    logger.debug("disconnected from " + channel.getRemoteAddress() + ",url:" + channel.getUrl());
                }
                invoke(channel, ON_DISCONNECT_KEY);
            }

            private void invoke(Channel channel, String methodKey) {
                Invocation invocation = createInvocation(channel, channel.getUrl(), methodKey);
                if (invocation != null) {
                    try {
                        if (Boolean.TRUE.toString().equals(invocation.getAttachment(STUB_EVENT_KEY))) {
                            tryToGetStubService(channel, invocation);
                        }
                        received(channel, invocation);
                    } catch (Throwable t) {
                        logger.warn(PROTOCOL_FAILED_REFER_INVOKER, "", "", "Failed to invoke event method " + invocation.getMethodName() + "(), cause: " + t.getMessage(), t);
                    }
                }
            }

            private void tryToGetStubService(Channel channel, Invocation invocation) throws RemotingException {
                try {
                    Invoker<?> invoker = getInvoker(channel, invocation);
                } catch (RemotingException e) {
                    String serviceKey = serviceKey(
                        0,
                        (String) invocation.getObjectAttachmentWithoutConvert(PATH_KEY),
                        (String) invocation.getObjectAttachmentWithoutConvert(VERSION_KEY),
                        (String) invocation.getObjectAttachmentWithoutConvert(GROUP_KEY)
                    );
                    throw new RemotingException(channel, "The stub service[" + serviceKey + "] is not found, it may not be exported yet");
                }
            }

            /**
             * FIXME channel.getUrl() always binds to a fixed service, and this service is random.
             * we can choose to use a common service to carry onConnect event if there's no easy way to get the specific
             * service this connection is binding to.
             * @param channel
             * @param url
             * @param methodKey
             * @return
             */
            private Invocation createInvocation(Channel channel, URL url, String methodKey) {
                String method = url.getParameter(methodKey);
                if (method == null || method.length() == 0) {
                    return null;
                }

                RpcInvocation invocation = new RpcInvocation(url.getServiceModel(), method, url.getParameter(INTERFACE_KEY), "", new Class<?>[0], new Object[0]);
                invocation.setAttachment(PATH_KEY, url.getPath());
                invocation.setAttachment(GROUP_KEY, url.getGroup());
                invocation.setAttachment(INTERFACE_KEY, url.getParameter(INTERFACE_KEY));
                invocation.setAttachment(VERSION_KEY, url.getVersion());
                if (url.getParameter(STUB_EVENT_KEY, false)) {
                    invocation.setAttachment(STUB_EVENT_KEY, Boolean.TRUE.toString());
                }

                return invocation;
            }
        };
        this.frameworkModel = frameworkModel;
    }

    /**
     * @deprecated Use {@link DubboProtocol#getDubboProtocol(ScopeModel)} instead
     */
    @Deprecated
    public static DubboProtocol getDubboProtocol() {
        return (DubboProtocol) FrameworkModel.defaultModel().getExtensionLoader(Protocol.class).getExtension(DubboProtocol.NAME, false);
    }

    public static DubboProtocol getDubboProtocol(ScopeModel scopeModel) {
        return (DubboProtocol) scopeModel.getExtensionLoader(Protocol.class).getExtension(DubboProtocol.NAME, false);
    }

    private boolean isClientSide(Channel channel) {
        InetSocketAddress address = channel.getRemoteAddress();
        URL url = channel.getUrl();
        return url.getPort() == address.getPort() &&
            NetUtils.filterLocalHost(channel.getUrl().getIp())
                .equals(NetUtils.filterLocalHost(address.getAddress().getHostAddress()));
    }

    Invoker<?> getInvoker(Channel channel, Invocation inv) throws RemotingException {
        boolean isCallBackServiceInvoke;
        boolean isStubServiceInvoke;
        int port = channel.getLocalAddress().getPort();
        String path = (String) inv.getObjectAttachmentWithoutConvert(PATH_KEY);

        //if it's stub service on client side(after enable stubevent, usually is set up onconnect or ondisconnect method)
        isStubServiceInvoke = Boolean.TRUE.toString().equals(inv.getObjectAttachmentWithoutConvert(STUB_EVENT_KEY));
        if (isStubServiceInvoke) {
            //when a stub service export to local, it usually can't be exposed to port
            port = 0;
        }

        // if it's callback service on client side
        isCallBackServiceInvoke = isClientSide(channel) && !isStubServiceInvoke;
        if (isCallBackServiceInvoke) {
            path += "." + inv.getObjectAttachmentWithoutConvert(CALLBACK_SERVICE_KEY);
            inv.setObjectAttachment(IS_CALLBACK_SERVICE_INVOKE, Boolean.TRUE.toString());
        }

        String serviceKey = serviceKey(
            port,
            path,
            (String) inv.getObjectAttachmentWithoutConvert(VERSION_KEY),
            (String) inv.getObjectAttachmentWithoutConvert(GROUP_KEY)
        );
        DubboExporter<?> exporter = (DubboExporter<?>) exporterMap.get(serviceKey);

        if (exporter == null) {
            throw new RemotingException(channel, "Not found exported service: " + serviceKey + " in " + exporterMap.keySet() + ", may be version or group mismatch " +
                ", channel: consumer: " + channel.getRemoteAddress() + " --> provider: " + channel.getLocalAddress() + ", message:" + getInvocationWithoutData(inv));
        }

        Invoker<?> invoker = exporter.getInvoker();
        inv.setServiceModel(invoker.getUrl().getServiceModel());
        return invoker;
    }

    public Collection<Invoker<?>> getInvokers() {
        return Collections.unmodifiableCollection(invokers);
    }

    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        checkDestroyed();
        URL url = invoker.getUrl();

        // export service.
        String key = serviceKey(url);
        DubboExporter<T> exporter = new DubboExporter<T>(invoker, key, exporterMap);

        //export a stub service for dispatching event
        boolean isStubSupportEvent = url.getParameter(STUB_EVENT_KEY, DEFAULT_STUB_EVENT);
        boolean isCallbackService = url.getParameter(IS_CALLBACK_SERVICE, false);
        if (isStubSupportEvent && !isCallbackService) {
            String stubServiceMethods = url.getParameter(STUB_EVENT_METHODS_KEY);
            if (stubServiceMethods == null || stubServiceMethods.length() == 0) {
                if (logger.isWarnEnabled()) {
                    logger.warn(PROTOCOL_UNSUPPORTED, "", "", "consumer [" + url.getParameter(INTERFACE_KEY) +
                        "], has set stub proxy support event ,but no stub methods founded.");
                }

            }
        }

        openServer(url);
        optimizeSerialization(url);

        return exporter;
    }

    private void openServer(URL url) {
        checkDestroyed();
        // find server.
        String key = url.getAddress();
        // client can export a service which only for server to invoke
        boolean isServer = url.getParameter(IS_SERVER_KEY, true);

        if (isServer) {
            ProtocolServer server = serverMap.get(key);
            if (server == null) {
                synchronized (this) {
                    server = serverMap.get(key);
                    if (server == null) {
                        serverMap.put(key, createServer(url));
                        return;
                    }
                }
            }

            // server supports reset, use together with override
            server.reset(url);
        }
    }

    private void checkDestroyed() {
        if (destroyed.get()) {
            throw new IllegalStateException(getClass().getSimpleName() + " is destroyed");
        }
    }

    private ProtocolServer createServer(URL url) {
        url = URLBuilder.from(url)
            // send readonly event when server closes, it's enabled by default
            .addParameterIfAbsent(CHANNEL_READONLYEVENT_SENT_KEY, Boolean.TRUE.toString())
            // enable heartbeat by default
            .addParameterIfAbsent(HEARTBEAT_KEY, String.valueOf(DEFAULT_HEARTBEAT))
            .addParameter(CODEC_KEY, DubboCodec.NAME)
            .build();

        String transporter = url.getParameter(SERVER_KEY, DEFAULT_REMOTING_SERVER);
        if (StringUtils.isNotEmpty(transporter) && !url.getOrDefaultFrameworkModel().getExtensionLoader(Transporter.class).hasExtension(transporter)) {
            throw new RpcException("Unsupported server type: " + transporter + ", url: " + url);
        }

        ExchangeServer server;
        try {
            server = Exchangers.bind(url, requestHandler);
        } catch (RemotingException e) {
            throw new RpcException("Fail to start server(url: " + url + ") " + e.getMessage(), e);
        }

        transporter = url.getParameter(CLIENT_KEY);
        if (StringUtils.isNotEmpty(transporter) && !url.getOrDefaultFrameworkModel().getExtensionLoader(Transporter.class).hasExtension(transporter)) {
            throw new RpcException("Unsupported client type: " + transporter);
        }

        DubboProtocolServer protocolServer = new DubboProtocolServer(server);
        loadServerProperties(protocolServer);
        return protocolServer;
    }


    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        checkDestroyed();
        return protocolBindingRefer(type, url);
    }

    @Override
    public <T> Invoker<T> protocolBindingRefer(Class<T> serviceType, URL url) throws RpcException {
        checkDestroyed();
        optimizeSerialization(url);

        // create rpc invoker.
        DubboInvoker<T> invoker = new DubboInvoker<T>(serviceType, url, getClients(url), invokers);
        invokers.add(invoker);

        return invoker;
    }

    private ClientsProvider getClients(URL url) {
        int connections = url.getParameter(CONNECTIONS_KEY, 0);
        // whether to share connection
        // if not configured, connection is shared, otherwise, one connection for one service
        if (connections == 0) {
            /*
             * The xml configuration should have a higher priority than properties.
             */
            String shareConnectionsStr = StringUtils.isBlank(url.getParameter(SHARE_CONNECTIONS_KEY, (String) null))
                ? ConfigurationUtils.getProperty(url.getOrDefaultApplicationModel(), SHARE_CONNECTIONS_KEY, DEFAULT_SHARE_CONNECTIONS)
                : url.getParameter(SHARE_CONNECTIONS_KEY, (String) null);
            connections = Integer.parseInt(shareConnectionsStr);

            return getSharedClient(url, connections);
        }

        List<ExchangeClient> clients = IntStream.range(0, connections)
            .mapToObj((i) -> initClient(url))
            .collect(Collectors.toList());
        return new ExclusiveClientsProvider(clients);
    }

    /**
     * Get shared connection
     *
     * @param url
     * @param connectNum connectNum must be greater than or equal to 1
     */
    @SuppressWarnings("unchecked")
    private SharedClientsProvider getSharedClient(URL url, int connectNum) {
        String key = url.getAddress();

        // connectNum must be greater than or equal to 1
        int expectedConnectNum = Math.max(connectNum, 1);
        return referenceClientMap.compute(key, (originKey, originValue) -> {
            if (originValue != null && originValue.increaseCount()) {
                return originValue;
            } else {
                return new SharedClientsProvider(this, originKey, buildReferenceCountExchangeClientList(url, expectedConnectNum));
            }
        });
    }

    protected void scheduleRemoveSharedClient(String key, SharedClientsProvider sharedClient) {
        this.frameworkModel.getBeanFactory().getBean(FrameworkExecutorRepository.class)
            .getSharedExecutor()
            .submit(() -> referenceClientMap.remove(key, sharedClient));
    }

    /**
     * Bulk build client
     *
     * @param url
     * @param connectNum
     * @return
     */
    private List<ReferenceCountExchangeClient> buildReferenceCountExchangeClientList(URL url, int connectNum) {
        List<ReferenceCountExchangeClient> clients = new ArrayList<>();

        for (int i = 0; i < connectNum; i++) {
            clients.add(buildReferenceCountExchangeClient(url));
        }

        return clients;
    }

    /**
     * Build a single client
     *
     * @param url
     * @return
     */
    private ReferenceCountExchangeClient buildReferenceCountExchangeClient(URL url) {
        ExchangeClient exchangeClient = initClient(url);
        ReferenceCountExchangeClient client = new ReferenceCountExchangeClient(exchangeClient, DubboCodec.NAME);
        // read configs
        int shutdownTimeout = ConfigurationUtils.getServerShutdownTimeout(url.getScopeModel());
        client.setShutdownWaitTime(shutdownTimeout);
        return client;
    }

    /**
     * Create new connection
     *
     * @param url
     */
    private ExchangeClient initClient(URL url) {
        /*
         * Instance of url is InstanceAddressURL, so addParameter actually adds parameters into ServiceInstance,
         * which means params are shared among different services. Since client is shared among services this is currently not a problem.
         */
        String str = url.getParameter(CLIENT_KEY, url.getParameter(SERVER_KEY, DEFAULT_REMOTING_CLIENT));

        // BIO is not allowed since it has severe performance issue.
        if (StringUtils.isNotEmpty(str) && !url.getOrDefaultFrameworkModel().getExtensionLoader(Transporter.class).hasExtension(str)) {
            throw new RpcException("Unsupported client type: " + str + "," +
                " supported client type is " + StringUtils.join(url.getOrDefaultFrameworkModel().getExtensionLoader(Transporter.class).getSupportedExtensions(), " "));
        }

        try {
            // Replace InstanceAddressURL with ServiceConfigURL.
            url = new ServiceConfigURL(DubboCodec.NAME, url.getUsername(), url.getPassword(), url.getHost(), url.getPort(), url.getPath(), url.getAllParameters());
            url = url.addParameter(CODEC_KEY, DubboCodec.NAME);
            // enable heartbeat by default
            url = url.addParameterIfAbsent(HEARTBEAT_KEY, String.valueOf(DEFAULT_HEARTBEAT));

            // connection should be lazy
            return url.getParameter(LAZY_CONNECT_KEY, false)
                ? new LazyConnectExchangeClient(url, requestHandler)
                : Exchangers.connect(url, requestHandler);
        } catch (RemotingException e) {
            throw new RpcException("Fail to create remoting client for service(" + url + "): " + e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void destroy() {
        if (!destroyed.compareAndSet(false, true)) {
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("Destroying protocol [" + this.getClass().getSimpleName() + "] ...");
        }
        for (String key : new ArrayList<>(serverMap.keySet())) {
            ProtocolServer protocolServer = serverMap.remove(key);

            if (protocolServer == null) {
                continue;
            }

            RemotingServer server = protocolServer.getRemotingServer();

            try {
                if (logger.isInfoEnabled()) {
                    logger.info("Closing dubbo server: " + server.getLocalAddress());
                }

                server.close(getServerShutdownTimeout(protocolServer));

            } catch (Throwable t) {
                logger.warn(PROTOCOL_ERROR_CLOSE_SERVER, "", "", "Close dubbo server [" + server.getLocalAddress() + "] failed: " + t.getMessage(), t);
            }
        }
        serverMap.clear();

        for (String key : new ArrayList<>(referenceClientMap.keySet())) {
            SharedClientsProvider clients = referenceClientMap.remove(key);
            clients.forceClose();
        }

        PortUnificationExchanger.close();
        referenceClientMap.clear();

        super.destroy();
    }

    /**
     * only log body in debugger mode for size & security consideration.
     *
     * @param invocation
     * @return
     */
    private Invocation getInvocationWithoutData(Invocation invocation) {
        if (logger.isDebugEnabled()) {
            return invocation;
        }
        if (invocation instanceof RpcInvocation) {
            RpcInvocation rpcInvocation = (RpcInvocation) invocation;
            rpcInvocation.setArguments(null);
            return rpcInvocation;
        }
        return invocation;
    }
}
