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
package org.apache.dubbo.xds.transport;

import org.apache.dubbo.xds.bootstrap.Bootstrapper.ServerInfo;

import java.net.URI;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.channel.EventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.epoll.Epoll;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollDomainSocketChannel;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.unix.DomainSocketAddress;

/** Owns a control channel and its native event loop. Shaded Netty does not alter Dubbo's business transport. */
public final class XdsTransport implements AutoCloseable {
    private final ManagedChannel channel;
    private final EventLoopGroup eventLoop;

    private XdsTransport(ManagedChannel channel, EventLoopGroup eventLoop) {
        this.channel = channel;
        this.eventLoop = eventLoop;
    }

    public static XdsTransport connect(ServerInfo server) {
        EventLoopGroup group = null;
        try {
            NettyChannelBuilder builder;
            if (server.getUri().startsWith("unix:")) {
                URI uri = URI.create(server.getUri());
                if (uri.getAuthority() != null
                        || uri.getPath() == null
                        || !uri.getPath().startsWith("/")) {
                    throw new IllegalArgumentException("Expected an absolute Unix socket path in the xDS server URI");
                }
                if (!"insecure".equals(server.getCredential())) {
                    throw new IllegalArgumentException(
                            "The local Istio ADS socket requires insecure channel credentials");
                }
                if (!Epoll.isAvailable()) {
                    throw new IllegalStateException(
                            "Istio ADS requires a Linux epoll Unix socket transport", Epoll.unavailabilityCause());
                }
                group = new EpollEventLoopGroup(1);
                builder = NettyChannelBuilder.forAddress(new DomainSocketAddress(
                                Paths.get(uri.getPath()).normalize().toString()))
                        .eventLoopGroup(group)
                        .channelType(EpollDomainSocketChannel.class);
            } else {
                builder = NettyChannelBuilder.forTarget(server.getUri());
            }
            if ("insecure".equals(server.getCredential())) {
                builder.usePlaintext();
            } else if ("tls".equals(server.getCredential())) {
                builder.useTransportSecurity();
            } else {
                throw new IllegalArgumentException("Unsupported ADS channel credentials");
            }
            return new XdsTransport(
                    builder.maxInboundMessageSize(16 * 1024 * 1024).build(), group);
        } catch (RuntimeException | Error e) {
            if (group != null) {
                group.shutdownGracefully(0, 1, TimeUnit.SECONDS);
            }
            throw e;
        }
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    @Override
    public void close() {
        channel.shutdownNow();
        if (eventLoop != null) {
            eventLoop.shutdownGracefully(0, 1, TimeUnit.SECONDS);
        }
    }
}
