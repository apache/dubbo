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
package org.apache.dubbo.registry.dns.util;

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.resolver.dns.SingletonDnsServerAddressStreamProvider;

public class DNSResolver {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    private final DnsNameResolver dnsResolver;
    private final Map<String, List<InetAddress>> cache = new ConcurrentHashMap<>();
    private final Map<String, List<Consumer<List<InetAddress>>>> listeners = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    private final ScheduledExecutorService scheduledExecutorService;
    private final NioEventLoopGroup group;

    public DNSResolver(String dnsServerHost, int dnsServerPort) {
        this.scheduledExecutorService =
                Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Dubbo-DNS-Registry-Refresh"));
        this.group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group).channel(NioDatagramChannel.class);

        this.dnsResolver = new DnsNameResolverBuilder(group.next())
                .nameServerProvider(new SingletonDnsServerAddressStreamProvider(
                        new InetSocketAddress(dnsServerHost, dnsServerPort)))
                .channelType(NioDatagramChannel.class)
                .queryTimeoutMillis(5000)
                .build();
        startRefreshTask();
    }

    public DNSResolver() {
        this.scheduledExecutorService =
                Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Dubbo-DNS-Registry-Refresh"));
        this.group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group).channel(NioDatagramChannel.class);

        this.dnsResolver = new DnsNameResolverBuilder(group.next())
                .channelType(NioDatagramChannel.class)
                .queryTimeoutMillis(5000)
                .build();
        startRefreshTask();
    }

    public void subscribe(String hostname, Consumer<List<InetAddress>> notifyListener) {
        readLock.lock();
        try {
            List<InetAddress> cachedAddresses = cache.get(hostname);
            if (cachedAddresses != null) {
                notifyListener.accept(Collections.unmodifiableList(cachedAddresses));
                listeners.computeIfAbsent(hostname, k -> new ArrayList<>()).add(notifyListener);
                return;
            }
        } finally {
            readLock.unlock();
        }

        // Acquire write lock to perform DNS resolution and update cache
        writeLock.lock();
        try {
            // Double-check to see if another thread has already updated the cache
            List<InetAddress> cachedAddresses = cache.get(hostname);
            if (cachedAddresses != null) {
                notifyListener.accept(Collections.unmodifiableList(cachedAddresses));
                listeners.computeIfAbsent(hostname, k -> new ArrayList<>()).add(notifyListener);
                return;
            }
            List<InetAddress> inetAddresses = dnsResolver
                    .resolveAll(hostname, Collections.emptySet())
                    .sync()
                    .getNow();
            if (inetAddresses.isEmpty()) {
                throw new RuntimeException("No DNS records found for " + hostname);
            }
            cache.put(hostname, inetAddresses);
            notifyListener.accept(Collections.unmodifiableList(inetAddresses));
            listeners.computeIfAbsent(hostname, k -> new ArrayList<>()).add(notifyListener);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            writeLock.unlock();
        }
    }

    public void unsubscribe(String hostname, Consumer<List<InetAddress>> notifyListener) {
        writeLock.lock();
        try {
            List<Consumer<List<InetAddress>>> listenerList = listeners.get(hostname);
            if (listenerList != null) {
                listenerList.remove(notifyListener);
                if (listenerList.isEmpty()) {
                    listeners.remove(hostname);
                    cache.remove(hostname);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    public void stop() {
        scheduledExecutorService.shutdownNow();
        dnsResolver.close();
        group.shutdownGracefully();
    }

    private void startRefreshTask() {
        scheduledExecutorService.scheduleAtFixedRate(
                () -> {
                    readLock.lock();
                    try {
                        for (Map.Entry<String, List<InetAddress>> entry : cache.entrySet()) {
                            String hostname = entry.getKey();
                            List<InetAddress> originAddress = entry.getValue();
                            // Upgrade to write lock to update cache
                            readLock.unlock();
                            writeLock.lock();
                            try {
                                List<Consumer<List<InetAddress>>> listenerList = listeners.get(hostname);
                                if (listenerList == null || listenerList.isEmpty()) {
                                    // If there are no listeners, remove the hostname from the cache
                                    cache.remove(hostname);
                                    continue;
                                }
                                // Resolve via DNS and update cache with new TTL
                                List<InetAddress> inetAddresses = dnsResolver
                                        .resolveAll(hostname, Collections.emptySet())
                                        .sync()
                                        .getNow();
                                if (inetAddresses.isEmpty()) {
                                    throw new Exception("No DNS records found for " + hostname);
                                }
                                cache.put(hostname, inetAddresses);
                                if (CollectionUtils.equals(inetAddresses, originAddress)) {
                                    continue;
                                }
                                for (Consumer<List<InetAddress>> listener : listenerList) {
                                    listener.accept(Collections.unmodifiableList(inetAddresses));
                                }
                            } catch (Exception e) {
                                logger.warn(
                                        LoggerCodeConstants.REGISTRY_FAILED_REFRESH_ADDRESS,
                                        "",
                                        "",
                                        "Failed to refresh hostname: " + hostname,
                                        e);
                            } finally {
                                writeLock.unlock();
                                readLock.lock();
                            }
                        }
                    } finally {
                        readLock.unlock();
                    }
                },
                1000,
                1000,
                TimeUnit.SECONDS);
    }
}
