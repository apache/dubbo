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
package org.apache.dubbo.rpc.protocol.grpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.AbstractProxyProtocol;

import io.grpc.BindableService;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.rpc.Constants.INTERFACES;

/**
 *
 */
public class gRPCProtocol extends AbstractProxyProtocol {

    public static final String NAME = "grpc";

    private static final Logger logger = LoggerFactory.getLogger(gRPCProtocol.class);

    public final static int DEFAULT_PORT = 50051;

    private final Map<String, gRPCServer> serverMap = new ConcurrentHashMap<>();

    private final Map<String, ManagedChannel> channelMap = new ConcurrentHashMap<>();

    /**
     * 传进来的impl implements DubboInterface, DubboInterface包含特定的3个通用方法就可以了
     *
     * @param impl
     * @param type
     * @param url
     * @param <T>
     * @return
     * @throws RpcException
     */
    @Override
    protected <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException {
        String key = url.getAddress();
        //若 address 对应的 gRPCServer 不存在，构造一个新的将其放入 map
        gRPCServer grpcServer = serverMap.computeIfAbsent(key, k -> {
            DubboHandlerRegistry registry = new DubboHandlerRegistry();
            Server originalServer = ServerBuilder
                    .forPort(url.getPort())
                    .fallbackHandlerRegistry(registry)
                    .build();
            return new gRPCServer(originalServer, registry);
        });
        grpcServer.getRegistry().addService((BindableService) impl, url.getServiceKey());
        return () -> grpcServer.getRegistry().removeService(url.getServiceKey());
//        Server server = ServerBuilder.forPort(url.getPort()).addService(((BindableService) impl)).build();
//        return server::shutdown;
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        return super.export(new gRPCInvoker<>(invoker));
    }

    /**
     * 这里返回的impl必须要有所有的方法, Stub BlockingStube FutureStub
     *
     * @param type
     * @param url
     * @param <T>
     * @return
     * @throws RpcException
     */
    @Override
    protected <T> T doRefer(Class<T> type, URL url) throws RpcException {
        //通过 type(IGreeter)，反射获得外部类（GreeterGrpc），调用静态方法（getDubboStub()），获得并返回（T）stub
        Class<?> enclosingClass = type.getEnclosingClass();
        if (enclosingClass == null) {
            throw new IllegalArgumentException(type.getName() + " must be declared inside protobuf generated classes, " +
                    "should be something like ServiceNameGrpc.IServiceName.");
        }

        final Method dubboStubMethod;
        try {
            dubboStubMethod = enclosingClass.getDeclaredMethod("getDubboStub", Channel.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Does not find getDubboStub in " + enclosingClass.getName() + ", please use the customized protoc-gen-grpc-dubbo-java to update the generated classes.");
        }

        Channel channel = channelMap.computeIfAbsent(url.getServiceKey(),
                k -> ManagedChannelBuilder.forAddress(url.getHost(), url.getPort()).usePlaintext().build()
        );

        try {
            @SuppressWarnings("unchecked") final T stub = (T) dubboStubMethod.invoke(null, channel);
            return stub;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Could not create stub through reflection.", e);
        }
    }

    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    public void destroy() {
        super.destroy();
        for (String key : serverMap.keySet()) {
            gRPCServer server = serverMap.remove(key);
            if (server != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Close gRPC server " + server.getServer().getServices());
                    }
                    server.stop();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }
        for (String key : channelMap.keySet()) {
            ManagedChannel channel = channelMap.remove(key);
            if (channel != null && !channel.isShutdown()) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Close gRPC channel");
                    }
                    channel.shutdown();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }
    }

    private class gRPCServer {
        private Server server;
        private DubboHandlerRegistry registry;

        public gRPCServer(Server server, DubboHandlerRegistry registry) {
            try {
                server.start();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to start gRPC server.", e);
            }
            this.server = server;
            this.registry = registry;
        }

        public void stop() {
            server.shutdown();
        }

        public Server getServer() {
            return server;
        }

        public DubboHandlerRegistry getRegistry() {
            return registry;
        }
    }

    private class gRPCInvoker<T> implements Invoker<T> {

        private Invoker<T> invoker;

        public gRPCInvoker(Invoker<T> invoker) {
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