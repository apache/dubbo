package org.apache.dubbo.rpc.protocol.rsocket;

import io.rsocket.*;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.transport.CodecSupport;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.protocol.AbstractProtocol;
import org.apache.dubbo.rpc.support.RpcUtils;
import org.reactivestreams.Publisher;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * @author sixie.xyn on 2019/1/2.
 */
public class RSocketProtocol extends AbstractProtocol {

    private static final Logger log = LoggerFactory.getLogger(RSocketProtocol.class);

    public static final String NAME = "rsocket";

    public static final int DEFAULT_PORT = 30880;

    private static RSocketProtocol INSTANCE;

    // <host:port,CloseableChannel>
    private final Map<String, CloseableChannel> serverMap = new ConcurrentHashMap<String, CloseableChannel>();

    // <host:port,RSocket>
    private final Map<String, RSocket> referenceClientMap = new ConcurrentHashMap<String, RSocket>();

    private final ConcurrentMap<String, Object> locks = new ConcurrentHashMap<String, Object>();

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

    Invoker<?> getInvoker(int port, Map<String, Object> metadataMap) throws RemotingException {
        String path = (String) metadataMap.get(RSocketConstants.SERVICE_NAME_KEY);
        String serviceKey = serviceKey(port, path, (String) metadataMap.get(RSocketConstants.SERVICE_VERSION_KEY), (String) metadataMap.get(Constants.GROUP_KEY));
        RSocketExporter<?> exporter = (RSocketExporter<?>) exporterMap.get(serviceKey);
        if (exporter == null) {
            //throw new Throwable("Not found exported service: " + serviceKey + " in " + exporterMap.keySet() + ", may be version or group mismatch " + ", channel: consumer: " + channel.getRemoteAddress() + " --> provider: " + channel.getLocalAddress() + ", message:" + inv);
            throw new RuntimeException("Not found exported service: " + serviceKey + " in " + exporterMap.keySet() + ", may be version or group mismatch ");
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
                    .acceptor(new SocketAcceptorImpl(bindPort))
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
            InetSocketAddress serverAddress = new InetSocketAddress(NetUtils.filterLocalHost(url.getHost()), url.getPort());
            RSocket client = RSocketFactory.connect().keepAliveTickPeriod(Duration.ZERO).keepAliveAckTimeout(Duration.ZERO).acceptor(
                    rSocket ->
                            new AbstractRSocket() {
                                public Mono<Payload> requestResponse(Payload payload) {
                                    //TODO support Mono arg
                                    throw new UnsupportedOperationException();
                                }

                                @Override
                                public Flux<Payload> requestStream(Payload payload) {
                                    //TODO support Flux arg
                                    throw new UnsupportedOperationException();
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
    private class SocketAcceptorImpl implements SocketAcceptor {

        private final int port;

        public SocketAcceptorImpl(int port) {
            this.port = port;
        }

        @Override
        public Mono<RSocket> accept(ConnectionSetupPayload setupPayload, RSocket reactiveSocket) {
            return Mono.just(
                    new AbstractRSocket() {
                        public Mono<Payload> requestResponse(Payload payload) {
                            try {
                                Map<String, Object> metadata = decodeMetadata(payload);
                                Byte serializeId = ((Integer) metadata.get(RSocketConstants.SERIALIZE_TYPE_KEY)).byteValue();
                                Invocation inv = decodeInvocation(payload, metadata, serializeId);

                                Result result = inv.getInvoker().invoke(inv);

                                Class<?> retType = RpcUtils.getReturnType(inv);
                                //ok
                                if (retType != null && Mono.class.isAssignableFrom(retType)) {
                                    Throwable th = result.getException();
                                    if (th == null) {
                                        Mono bizMono = (Mono) result.getValue();
                                        Mono<Payload> retMono = bizMono.map(new Function<Object, Payload>() {
                                            @Override
                                            public Payload apply(Object o) {
                                                try {
                                                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                                    ObjectOutput out = CodecSupport.getSerializationById(serializeId).serialize(null, bos);
                                                    out.writeByte((byte) 0);
                                                    out.writeObject(o);
                                                    out.flushBuffer();
                                                    bos.flush();
                                                    bos.close();
                                                    Payload responsePayload = DefaultPayload.create(bos.toByteArray());
                                                    return responsePayload;
                                                } catch (Throwable t) {
                                                    throw Exceptions.propagate(t);
                                                }
                                            }
                                        }).onErrorResume(new Function<Throwable, Publisher<Payload>>() {
                                            @Override
                                            public Publisher<Payload> apply(Throwable throwable) {
                                                try {
                                                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                                    ObjectOutput out = CodecSupport.getSerializationById(serializeId).serialize(null, bos);
                                                    out.writeByte((byte) RSocketConstants.FLAG_ERROR);
                                                    out.writeObject(throwable);
                                                    out.flushBuffer();
                                                    bos.flush();
                                                    bos.close();
                                                    Payload errorPayload = DefaultPayload.create(bos.toByteArray());
                                                    return Flux.just(errorPayload);
                                                } catch (Throwable t) {
                                                    throw Exceptions.propagate(t);
                                                }
                                            }
                                        });

                                        return retMono;
                                    } else {
                                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                        ObjectOutput out = CodecSupport.getSerializationById(serializeId).serialize(null, bos);
                                        out.writeByte((byte) RSocketConstants.FLAG_ERROR);
                                        out.writeObject(th);
                                        out.flushBuffer();
                                        bos.flush();
                                        bos.close();
                                        Payload errorPayload = DefaultPayload.create(bos.toByteArray());
                                        return Mono.just(errorPayload);
                                    }

                                } else {
                                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                    ObjectOutput out = CodecSupport.getSerializationById(serializeId).serialize(null, bos);
                                    int flag = RSocketConstants.FLAG_HAS_ATTACHMENT;

                                    Throwable th = result.getException();
                                    if (th == null) {
                                        Object ret = result.getValue();
                                        if (ret == null) {
                                            flag |= RSocketConstants.FLAG_NULL_VALUE;
                                            out.writeByte((byte) flag);
                                        } else {
                                            out.writeByte((byte) flag);
                                            out.writeObject(ret);
                                        }
                                    } else {
                                        flag |= RSocketConstants.FLAG_ERROR;
                                        out.writeByte((byte) flag);
                                        out.writeObject(th);
                                    }
                                    out.writeObject(result.getAttachments());
                                    out.flushBuffer();
                                    bos.flush();
                                    bos.close();

                                    Payload responsePayload = DefaultPayload.create(bos.toByteArray());
                                    return Mono.just(responsePayload);
                                }
                            } catch (Throwable t) {
                                //application error
                                return Mono.error(t);
                            } finally {
                                payload.release();
                            }
                        }

                        public Flux<Payload> requestStream(Payload payload) {
                            try {
                                Map<String, Object> metadata = decodeMetadata(payload);
                                Byte serializeId = ((Integer) metadata.get(RSocketConstants.SERIALIZE_TYPE_KEY)).byteValue();
                                Invocation inv = decodeInvocation(payload, metadata, serializeId);

                                Result result = inv.getInvoker().invoke(inv);
                                //Class<?> retType = RpcUtils.getReturnType(inv);

                                Throwable th = result.getException();
                                if (th != null) {
                                    Payload errorPayload = encodeError(th, serializeId);
                                    return Flux.just(errorPayload);
                                }

                                Flux flux = (Flux) result.getValue();
                                Flux<Payload> retFlux = flux.map(new Function<Object, Payload>() {
                                    @Override
                                    public Payload apply(Object o) {
                                        try {
                                            return encodeData(o, serializeId);
                                        } catch (Throwable t) {
                                            throw new RuntimeException(t);
                                        }
                                    }
                                }).onErrorResume(new Function<Throwable, Publisher<Payload>>() {
                                    @Override
                                    public Publisher<Payload> apply(Throwable throwable) {
                                        try {
                                            Payload errorPayload = encodeError(throwable,serializeId);
                                            return Flux.just(errorPayload);
                                        } catch (Throwable t) {
                                            throw new RuntimeException(t);
                                        }
                                    }
                                });
                                return retFlux;
                            } catch (Throwable t) {
                                return Flux.error(t);
                            } finally {
                                payload.release();
                            }
                        }

                        private Payload encodeData(Object data, byte serializeId) throws Throwable{
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            ObjectOutput out = CodecSupport.getSerializationById(serializeId).serialize(null, bos);
                            out.writeByte((byte) 0);
                            out.writeObject(data);
                            out.flushBuffer();
                            bos.flush();
                            bos.close();
                            return DefaultPayload.create(bos.toByteArray());
                        }

                        private Payload encodeError(Throwable throwable, byte serializeId) throws Throwable{
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            ObjectOutput out = CodecSupport.getSerializationById(serializeId).serialize(null, bos);
                            out.writeByte((byte) RSocketConstants.FLAG_ERROR);
                            out.writeObject(throwable);
                            out.flushBuffer();
                            bos.flush();
                            bos.close();
                            return DefaultPayload.create(bos.toByteArray());
                        }

                        private Map<String, Object> decodeMetadata(Payload payload) throws IOException {
                            ByteBuffer metadataBuffer = payload.getMetadata();
                            byte[] metadataBytes = new byte[metadataBuffer.remaining()];
                            metadataBuffer.get(metadataBytes, metadataBuffer.position(), metadataBuffer.remaining());
                            return MetadataCodec.decodeMetadata(metadataBytes);
                        }

                        private Invocation decodeInvocation(Payload payload, Map<String, Object> metadata, Byte serializeId) throws RemotingException, IOException, ClassNotFoundException {
                            Invoker<?> invoker = getInvoker(port, metadata);

                            String serviceName = (String) metadata.get(RSocketConstants.SERVICE_NAME_KEY);
                            String version = (String) metadata.get(RSocketConstants.SERVICE_VERSION_KEY);
                            String methodName = (String) metadata.get(RSocketConstants.METHOD_NAME_KEY);
                            String paramType = (String) metadata.get(RSocketConstants.PARAM_TYPE_KEY);

                            ByteBuffer dataBuffer = payload.getData();
                            byte[] dataBytes = new byte[dataBuffer.remaining()];
                            dataBuffer.get(dataBytes, dataBuffer.position(), dataBuffer.remaining());


                            //TODO how to get remote address
                            //RpcContext rpcContext = RpcContext.getContext();
                            //rpcContext.setRemoteAddress(channel.getRemoteAddress());


                            RpcInvocation inv = new RpcInvocation();
                            inv.setInvoker(invoker);
                            inv.setAttachment(Constants.PATH_KEY, serviceName);
                            inv.setAttachment(Constants.VERSION_KEY, version);
                            inv.setMethodName(methodName);


                            InputStream dataInputStream = new ByteArrayInputStream(dataBytes);
                            ObjectInput in = CodecSupport.getSerializationById(serializeId).deserialize(null, dataInputStream);

                            Object[] args;
                            Class<?>[] pts;
                            String desc = paramType;
                            if (desc.length() == 0) {
                                pts = new Class<?>[0];
                                args = new Object[0];
                            } else {
                                pts = ReflectUtils.desc2classArray(desc);
                                args = new Object[pts.length];
                                for (int i = 0; i < args.length; i++) {
                                    try {
                                        args[i] = in.readObject(pts[i]);
                                    } catch (Exception e) {
                                        if (log.isWarnEnabled()) {
                                            log.warn("Decode argument failed: " + e.getMessage(), e);
                                        }
                                    }
                                }
                            }
                            inv.setParameterTypes(pts);
                            inv.setArguments(args);
                            Map<String, String> map = (Map<String, String>) in.readObject(Map.class);
                            if (map != null && map.size() > 0) {
                                inv.addAttachments(map);
                            }
                            return inv;
                        }
                    });
        }
    }
}
