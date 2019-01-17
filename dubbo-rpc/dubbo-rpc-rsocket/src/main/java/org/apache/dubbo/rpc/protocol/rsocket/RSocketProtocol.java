package org.apache.dubbo.rpc.protocol.rsocket;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.exchange.ExchangeHandler;
import org.apache.dubbo.remoting.exchange.support.ExchangeHandlerAdapter;
import org.apache.dubbo.rpc.AsyncContextImpl;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.AbstractProtocol;

import io.rsocket.AbstractRSocket;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.SocketAcceptor;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author sixie.xyn on 2019/1/2.
 */
public class RSocketProtocol extends AbstractProtocol {
//    @Override
//    public int getDefaultPort() {
//        return 0;
//    }
//
//    @Override
//    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
//        return null;
//    }
//
//    @Override
//    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
//        return null;
//    }

    public static final String NAME = "rsocket";

    public static final int DEFAULT_PORT = 30880;

    private static RSocketProtocol INSTANCE;

    // <host:port,CloseableChannel>
    private final Map<String, CloseableChannel> serverMap = new ConcurrentHashMap<String, CloseableChannel>();

    // <host:port,RSocket>
    private final Map<String, RSocket> referenceClientMap = new ConcurrentHashMap<String, RSocket>();

    private final ConcurrentMap<String, Object> locks = new ConcurrentHashMap<String, Object>();

    private ExchangeHandler requestHandler = new ExchangeHandlerAdapter() {

        @Override
        public CompletableFuture<Object> reply(ExchangeChannel channel, Object message) throws RemotingException {
            if (message instanceof Invocation) {
                Invocation inv = (Invocation) message;
                Invoker<?> invoker = getInvoker(channel, inv);
                // need to consider backward-compatibility if it's a callback

                RpcContext rpcContext = RpcContext.getContext();
                boolean supportServerAsync = invoker.getUrl().getMethodParameter(inv.getMethodName(), Constants.ASYNC_KEY, false);
                if (supportServerAsync) {
                    CompletableFuture<Object> future = new CompletableFuture<>();
                    rpcContext.setAsyncContext(new AsyncContextImpl(future));
                }
                rpcContext.setRemoteAddress(channel.getRemoteAddress());
                Result result = invoker.invoke(inv);

                if (result instanceof AsyncRpcResult) {
                    return ((AsyncRpcResult) result).getResultFuture().thenApply(r -> (Object) r);
                } else {
                    return CompletableFuture.completedFuture(result);
                }
            }
            throw new RemotingException(channel, "Unsupported request: "
                    + (message == null ? null : (message.getClass().getName() + ": " + message))
                    + ", channel: consumer: " + channel.getRemoteAddress() + " --> provider: " + channel.getLocalAddress());
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
            invoke(channel, Constants.ON_CONNECT_KEY);
        }

        @Override
        public void disconnected(Channel channel) throws RemotingException {
            if (logger.isInfoEnabled()) {
                logger.info("disconnected from " + channel.getRemoteAddress() + ",url:" + channel.getUrl());
            }
            invoke(channel, Constants.ON_DISCONNECT_KEY);
        }

        private void invoke(Channel channel, String methodKey) {
            Invocation invocation = createInvocation(channel, channel.getUrl(), methodKey);
            if (invocation != null) {
                try {
                    received(channel, invocation);
                } catch (Throwable t) {
                    logger.warn("Failed to invoke event method " + invocation.getMethodName() + "(), cause: " + t.getMessage(), t);
                }
            }
        }

        private Invocation createInvocation(Channel channel, URL url, String methodKey) {
            String method = url.getParameter(methodKey);
            if (method == null || method.length() == 0) {
                return null;
            }
            RpcInvocation invocation = new RpcInvocation(method, new Class<?>[0], new Object[0]);
            invocation.setAttachment(Constants.PATH_KEY, url.getPath());
            invocation.setAttachment(Constants.GROUP_KEY, url.getParameter(Constants.GROUP_KEY));
            invocation.setAttachment(Constants.INTERFACE_KEY, url.getParameter(Constants.INTERFACE_KEY));
            invocation.setAttachment(Constants.VERSION_KEY, url.getParameter(Constants.VERSION_KEY));
            if (url.getParameter(Constants.STUB_EVENT_KEY, false)) {
                invocation.setAttachment(Constants.STUB_EVENT_KEY, Boolean.TRUE.toString());
            }
            return invocation;
        }
    };

    public RSocketProtocol() {
        INSTANCE = this;
    }

    public static RSocketProtocol getRSocketProtocol() {
        if (INSTANCE == null) {
            ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(RSocketProtocol.NAME); // load
        }
        return INSTANCE;
    }

    public Collection<Exporter<?>> getExporters() {
        return Collections.unmodifiableCollection(exporterMap.values());
    }

    Map<String, Exporter<?>> getExporterMap() {
        return exporterMap;
    }

    private boolean isClientSide(Channel channel) {
        InetSocketAddress address = channel.getRemoteAddress();
        URL url = channel.getUrl();
        return url.getPort() == address.getPort() &&
                NetUtils.filterLocalHost(channel.getUrl().getIp())
                        .equals(NetUtils.filterLocalHost(address.getAddress().getHostAddress()));
    }

    Invoker<?> getInvoker(Channel channel, Invocation inv) throws RemotingException {
        int port = channel.getLocalAddress().getPort();
        String path = inv.getAttachments().get(Constants.PATH_KEY);
        String serviceKey = serviceKey(port, path, inv.getAttachments().get(Constants.VERSION_KEY), inv.getAttachments().get(Constants.GROUP_KEY));
        RSocketExporter<?> exporter = (RSocketExporter<?>) exporterMap.get(serviceKey);
        if (exporter == null) {
            throw new RemotingException(channel, "Not found exported service: " + serviceKey + " in " + exporterMap.keySet() + ", may be version or group mismatch " + ", channel: consumer: " + channel.getRemoteAddress() + " --> provider: " + channel.getLocalAddress() + ", message:" + inv);
        }

        return exporter.getInvoker();
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
        URL url = invoker.getUrl();

        // export service.
        String key = serviceKey(url);
        RSocketExporter<T> exporter = new RSocketExporter<T>(invoker, key, exporterMap);
        exporterMap.put(key, exporter);

        //export an stub service for dispatching event
//        Boolean isStubSupportEvent = url.getParameter(Constants.STUB_EVENT_KEY, Constants.DEFAULT_STUB_EVENT);
//        Boolean isCallbackservice = url.getParameter(Constants.IS_CALLBACK_SERVICE, false);
//        if (isStubSupportEvent && !isCallbackservice) {
//            String stubServiceMethods = url.getParameter(Constants.STUB_EVENT_METHODS_KEY);
//            if (stubServiceMethods == null || stubServiceMethods.length() == 0) {
//                if (logger.isWarnEnabled()) {
//                    logger.warn(new IllegalStateException("consumer [" + url.getParameter(Constants.INTERFACE_KEY) +
//                            "], has set stubproxy support event ,but no stub methods founded."));
//                }
//            } else {
//                stubServiceMethodsMap.put(url.getServiceKey(), stubServiceMethods);
//            }
//        }

        openServer(url);
        return exporter;
    }

    private void openServer(URL url) {
        String key = url.getAddress();
        //client can export a service which's only for server to invoke
        boolean isServer = url.getParameter(Constants.IS_SERVER_KEY, true);
        if (isServer) {
            CloseableChannel server = serverMap.get(key);
            if (server == null) {
                synchronized (this) {
                    server = serverMap.get(key);
                    if (server == null) {
                        serverMap.put(key, createServer(url));
                    }
                }
            }
        }
    }

    private CloseableChannel createServer(URL url) {
        try {
            String bindIp = url.getParameter(Constants.BIND_IP_KEY, url.getHost());
            int bindPort = url.getParameter(Constants.BIND_PORT_KEY, url.getPort());
            if (url.getParameter(Constants.ANYHOST_KEY, false) || NetUtils.isInvalidLocalHost(bindIp)) {
                bindIp = NetUtils.ANYHOST;
            }
            return RSocketFactory.receive()
                    .acceptor(new SocketAcceptorImpl())
                    .transport(TcpServerTransport.create(bindIp, bindPort))
                    .start()
                    .block();
        } catch (Throwable e) {
            throw new RpcException("Fail to start server(url: " + url + ") " + e.getMessage(), e);
        }
    }


    @Override
    public <T> Invoker<T> refer(Class<T> serviceType, URL url) throws RpcException {
        // create rpc invoker.
        RSocketInvoker<T> invoker = new RSocketInvoker<T>(serviceType, url, getClients(url), invokers);
        invokers.add(invoker);
        return invoker;
    }

    private RSocket[] getClients(URL url) {
        // whether to share connection
        boolean service_share_connect = false;
        int connections = url.getParameter(Constants.CONNECTIONS_KEY, 0);
        // if not configured, connection is shared, otherwise, one connection for one service
        if (connections == 0) {
            service_share_connect = true;
            connections = 1;
        }

        RSocket[] clients = new RSocket[connections];
        for (int i = 0; i < clients.length; i++) {
            if (service_share_connect) {
                clients[i] = getSharedClient(url);
            } else {
                clients[i] = initClient(url);
            }
        }
        return clients;
    }

    /**
     * Get shared connection
     */
    private RSocket getSharedClient(URL url) {
        String key = url.getAddress();
        RSocket client = referenceClientMap.get(key);
        if (client != null) {
            return client;
        }

        locks.putIfAbsent(key, new Object());
        synchronized (locks.get(key)) {
            if (referenceClientMap.containsKey(key)) {
                return referenceClientMap.get(key);
            }

            client = initClient(url);
            referenceClientMap.put(key, client);
            locks.remove(key);
            return client;
        }
    }

    /**
     * Create new connection
     */
    private RSocket initClient(URL url) {
        try {
            InetSocketAddress serverAddress = new InetSocketAddress(NetUtils.filterLocalHost(url.getHost()),url.getPort());
            RSocket client = RSocketFactory.connect().acceptor(
                    rSocket ->
                            new AbstractRSocket() {
                                public Mono<Payload> requestResponse(Payload payload) {
                                    ByteBuffer metadata = payload.getMetadata();
                                    ByteBuffer data = payload.getData();
                                    payload.release();
                                    return Mono.error(new UnsupportedOperationException("Request-Response not implemented."));
                                }

                                @Override
                                public Flux<Payload> requestStream(Payload payload) {
                                    return Flux.interval(Duration.ofSeconds(1))
                                            .map(aLong -> DefaultPayload.create("Bi-di Response => " + aLong));
                                }
                            })
                    .transport(TcpClientTransport.create(serverAddress))
                    .start()
                    .block();
            return client;
        } catch (Throwable e) {
            throw new RpcException("Fail to create remoting client for service(" + url + "): " + e.getMessage(), e);
        }

    }

    @Override
    public void destroy() {
        for (String key : new ArrayList<String>(serverMap.keySet())) {
            CloseableChannel server = serverMap.remove(key);
            if (server != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Close dubbo server: " + server.address());
                    }
                    server.dispose();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }

        for (String key : new ArrayList<String>(referenceClientMap.keySet())) {
            RSocket client = referenceClientMap.remove(key);
            if (client != null) {
                try {
//                    if (logger.isInfoEnabled()) {
//                        logger.info("Close dubbo connect: " + client. + "-->" + client.getRemoteAddress());
//                    }
                    client.dispose();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }
        super.destroy();
    }


    //server process logic
    private static class SocketAcceptorImpl implements SocketAcceptor {
        @Override
        public Mono<RSocket> accept(ConnectionSetupPayload setupPayload, RSocket reactiveSocket) {
            return Mono.just(
                    new AbstractRSocket() {

                        public Mono<Payload> requestResponse(Payload payload) {
                            ByteBuffer metadata = payload.getMetadata();
                            ByteBuffer data = payload.getData();

                            payload.release();
                            return Mono.error(new UnsupportedOperationException("Request-Response not implemented."));
                        }

                        public Flux<Payload> requestStream(Payload payload) {
                            payload.release();
                            return Flux.error(new UnsupportedOperationException("Request-Stream not implemented."));
                        }


                        @Override
                        public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
                            return Flux.from(payloads)
                                    .map(Payload::getDataUtf8)
                                    .map(s -> "Echo: " + s)
                                    .map(DefaultPayload::create);
                        }

                    });
        }
    }
}
