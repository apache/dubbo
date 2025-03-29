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
package org.apache.dubbo.registry.multicast;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.support.FailbackRegistry;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MulticastRegistry extends FailbackRegistry {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(MulticastRegistry.class);
    private final MulticastCommunication multicastCommunication;
    private final RegistryCleaner registryCleaner;
    private final ScheduledExecutorService cleanExecutor;
    private final ScheduledFuture<?> cleanFuture;
    private final int cleanPeriod;
    private ApplicationModel applicationModel = null;
    private final ConcurrentMap<URL, Set<URL>> received = new ConcurrentHashMap<>();

    public MulticastRegistry(URL url) throws IOException {
        super(url);

        // Validate multicast address
        if ("0.0.0.0".equals(url.getHost())) {
            throw new IllegalStateException("Invalid multicast address: 0.0.0.0");
        }

        InetAddress multicastAddress = InetAddress.getByName(url.getHost());

        // Ensure it's a valid multicast address
        if (!multicastAddress.isMulticastAddress()) {
            throw new IllegalStateException("Invalid multicast address: " + url.getHost());
        }

        int multicastPort = url.getPort() <= 0 ? 1234 : url.getPort();
        MulticastSocket multicastSocket = new MulticastSocket(multicastPort);
        this.multicastCommunication = new MulticastCommunication(multicastAddress, multicastSocket, multicastPort);
        this.registryCleaner = new RegistryCleaner(this, received);
        this.cleanPeriod = url.getParameter("session.timeout", 60000);
        this.cleanExecutor = Executors.newScheduledThreadPool(1);
        this.cleanFuture = cleanExecutor.scheduleWithFixedDelay(
                () -> {
                    try {
                        registryCleaner.clean();
                    } catch (Throwable t) {
                        logger.error(
                                "Unexpected exception occur at clean expired provider, cause: " + t.getMessage(), t);
                    }
                },
                cleanPeriod,
                cleanPeriod,
                TimeUnit.MILLISECONDS);
    }

    // Add method to get received map
    public Map<URL, Set<URL>> getReceived() {
        return received;
    }

    // Add method to get MulticastSocket
    public MulticastSocket getMulticastSocket() {
        return multicastCommunication.getMulticastSocket();
    }

    @Override
    public void doRegister(URL url) {
        multicastCommunication.multicast("REGISTER " + url.toFullString());
    }

    @Override
    public void doUnregister(URL url) {
        multicastCommunication.multicast("UNREGISTER " + url.toFullString());
    }

    @Override
    public void doSubscribe(URL url, NotifyListener listener) {
        multicastCommunication.multicast("SUBSCRIBE " + url.toFullString());
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        multicastCommunication.multicast("UNSUBSCRIBE " + url.toFullString());
    }

    @Override
    public boolean isAvailable() {
        try {
            return multicastCommunication != null
                    && multicastCommunication.getMulticastSocket() != null
                    && !multicastCommunication.getMulticastSocket().isClosed();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void destroy() {
        try {
            // Explicitly close the multicast socket
            MulticastSocket socket = multicastCommunication.getMulticastSocket();
            socket.close();
        } catch (Exception e) {
            logger.warn("Error closing multicast socket during destroy", e);
        }
        super.destroy();
        cleanExecutor.shutdown();
    }

    @Override
    public List<URL> lookup(URL url) {
        List<URL> result = new ArrayList<>();

        // Look for matching URLs in the received map
        for (Entry<URL, Set<URL>> entry : received.entrySet()) {
            if (UrlUtils.isMatch(entry.getKey(), url)) {
                result.addAll(entry.getValue());
            }
        }

        return result;
    }

    public void registered(URL url) {
        for (Entry<URL, Set<NotifyListener>> entry : getSubscribed().entrySet()) {
            URL key = entry.getKey();
            if (UrlUtils.isMatch(key, url)) {
                Set<URL> urls = received.computeIfAbsent(key, k -> new ConcurrentHashSet<>());
                urls.add(url);
                List<URL> list = toList(urls);
                for (final NotifyListener listener : entry.getValue()) {
                    notify(key, listener, list);
                }
            }
        }
    }

    public void unregistered(URL url) {
        for (Entry<URL, Set<NotifyListener>> entry : getSubscribed().entrySet()) {
            URL key = entry.getKey();
            if (UrlUtils.isMatch(key, url)) {
                Set<URL> urls = received.get(key);
                if (urls != null) {
                    urls.remove(url);
                }
                if (urls == null || urls.isEmpty()) {
                    if (urls == null) {
                        urls = new ConcurrentHashSet<>();
                    }
                    URL empty = url.setProtocol("empty");
                    urls.add(empty);
                }
                List<URL> list = toList(urls);
                for (NotifyListener listener : entry.getValue()) {
                    notify(key, listener, list);
                }
            }
        }
    }

    public void subscribed(URL url, NotifyListener listener) {
        // Add detailed logging
        System.out.println("Subscribed method called with URL: " + url);

        List<URL> urls = lookup(url);
        System.out.println("Lookup results: " + urls);

        // Always notify if urls are not empty, even if listener is null
        if (urls != null && !urls.isEmpty()) {
            System.out.println("Found matching URLs: " + urls);

            if (listener != null) {
                notify(url, listener, urls);
            } else {
                // Notify using the default mechanism when listener is null
                for (Entry<URL, Set<NotifyListener>> entry : getSubscribed().entrySet()) {
                    URL key = entry.getKey();
                    if (UrlUtils.isMatch(key, url)) {
                        System.out.println("Matching subscription found: " + key);
                        for (NotifyListener subscribedListener : entry.getValue()) {
                            notify(key, subscribedListener, urls);
                        }
                    }
                }
            }
        } else {
            System.out.println("No matching URLs found for subscription");
        }
    }

    private List<URL> toList(Set<URL> urls) {
        List<URL> list = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(urls)) {
            list.addAll(urls);
        }
        return list;
    }
}
