package org.apache.dubbo.rpc.protocol.grpc;/*
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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.AbstractProxyProtocol;

import io.grpc.BindableService;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.rpc.Constants.INTERFACES;

/**
 *
 */
public class GrpcProtocol extends AbstractProxyProtocol {

    private static final Logger logger = LoggerFactory.getLogger(GrpcProtocol.class);

    public final static int DEFAULT_PORT = 50051;

    /* <address, gRPC channel> */
    private final ConcurrentMap<String, ManagedChannel> channelMap = new ConcurrentHashMap<>();

    @Override
    protected <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException {
        String key = url.getAddress();
        ProtocolServer protocolServer = serverMap.computeIfAbsent(key, k -> {
            DubboHandlerRegistry registry = new DubboHandlerRegistry();

            NettyServerBuilder builder =
                    NettyServerBuilder
                    .forPort(url.getPort())
                            .fallbackHandlerRegistry(registry);

            Server originalServer = GrpcOptionsUtils.buildServerBuilder(url, builder).build();
            GrpcRemotingServer remotingServer = new GrpcRemotingServer(originalServer, registry);
            return new ProxyProtocolServer(remotingServer);
        });

        GrpcRemotingServer grpcServer = (GrpcRemotingServer) protocolServer.getRemotingServer();
        grpcServer.getRegistry().addService((BindableService) impl, url.getServiceKey());

        grpcServer.start();

        return () -> grpcServer.getRegistry().removeService(url.getServiceKey());
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        return super.export(new GrpcServerProxyInvoker<>(invoker));
    }

    @Override
    protected <T> Invoker<T> protocolBindingRefer(final Class<T> type, final URL url) throws RpcException {
        Class<?> enclosingClass = type.getEnclosingClass();

        if (enclosingClass == null) {
            throw new IllegalArgumentException(type.getName() + " must be declared inside protobuf generated classes, " +
                    "should be something like ServiceNameGrpc.IServiceName.");
        }

        final Method dubboStubMethod;
        try {
            dubboStubMethod = enclosingClass.getDeclaredMethod("getDubboStub", Channel.class, CallOptions.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Does not find getDubboStub in " + enclosingClass.getName() + ", please use the customized protoc-gen-dubbo-java to update the generated classes.");
        }

        // Channel
        ManagedChannel channel = channelMap.computeIfAbsent(url.getAddress(),
                k -> GrpcOptionsUtils.buildManagedChannel(url)
        );

        // CallOptions
        try {
            @SuppressWarnings("unchecked") final T stub = (T) dubboStubMethod.invoke(null, channel, GrpcOptionsUtils.buildCallOptions(url));
            final Invoker<T> target = proxyFactory.getInvoker(stub, type, url);
            GrpcInvoker<T> grpcInvoker = new GrpcInvoker<>(type, url, target, channel);
            invokers.add(grpcInvoker);
            return grpcInvoker;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Could not create stub through reflection.", e);
        }
    }

    /**
     * not used
     *
     * @param type
     * @param url
     * @param <T>
     * @return
     * @throws RpcException
     */
    @Override
    protected <T> T doRefer(Class<T> type, URL url) throws RpcException {
        throw new UnsupportedOperationException("not used");
    }

    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    public void destroy() {
        serverMap.values().forEach(ProtocolServer::close);
        channelMap.values().forEach(ManagedChannel::shutdown);
        serverMap.clear();
        channelMap.clear();
    }

    public class GrpcRemotingServer extends RemotingServerAdapter {

        private Server originalServer;
        private DubboHandlerRegistry handlerRegistry;

        public GrpcRemotingServer(Server server, DubboHandlerRegistry handlerRegistry) {
            this.originalServer = server;
            this.handlerRegistry = handlerRegistry;
        }

        public void start() throws RpcException {
            try {
                originalServer.start();
            } catch (IOException e) {
                throw new RpcException("Starting gRPC server failed. ", e);
            }
        }

        public DubboHandlerRegistry getRegistry() {
            return handlerRegistry;
        }

        @Override
        public Object getDelegateServer() {
            return originalServer;
        }

        @Override
        public void close() {
            originalServer.shutdown();
        }
    }

    /**
     * TODO, If IGreeter extends BindableService we can avoid the existence of this wrapper invoker.
     *
     * @param <T>
     */
    private class GrpcServerProxyInvoker<T> implements Invoker<T> {

        private Invoker<T> invoker;

        public GrpcServerProxyInvoker(Invoker<T> invoker) {
            this.invoker = invoker;
        }

        @Override
        public Class<T> getInterface() {
            return invoker.getInterface();
        }

        @Override
        public Result invoke(Invocation invocation) throws RpcException {
            return invoker.invoke(invocation);
        }

        @Override
        public URL getUrl() {
            URL url = invoker.getUrl();
            String interfaces = url.getParameter(INTERFACES);
            if (StringUtils.isNotEmpty(interfaces)) {
                interfaces += ("," + BindableService.class.getName());
            } else {
                interfaces = BindableService.class.getName();
            }
            return url.addParameter(INTERFACES, interfaces);
        }

        @Override
        public boolean isAvailable() {
            return invoker.isAvailable();
        }

        @Override
        public void destroy() {
            invoker.destroy();
        }
    }

}
