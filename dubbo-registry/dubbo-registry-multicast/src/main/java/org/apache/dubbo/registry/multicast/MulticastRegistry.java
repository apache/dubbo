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

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.support.FailbackRegistry;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * MulticastRegistry
 */
public class MulticastRegistry extends FailbackRegistry {

    // logging output
    private static final Logger logger = LoggerFactory.getLogger(MulticastRegistry.class);

    private static final int DEFAULT_MULTICAST_PORT = 1234;

    private final InetAddress mutilcastAddress;

    private final MulticastSocket mutilcastSocket;

    private final int mutilcastPort;

    private final ConcurrentMap<URL, Set<URL>> received = new ConcurrentHashMap<URL, Set<URL>>();

    private final ScheduledExecutorService cleanExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("DubboMulticastRegistryCleanTimer", true));

    private final ScheduledFuture<?> cleanFuture;

    private final int cleanPeriod;

    private volatile boolean admin = false;

    public MulticastRegistry(URL url) {
        super(url);
        if (url.isAnyHost()) {
            throw new IllegalStateException("registry address == null");
        }
        if (!isMulticastAddress(url.getHost())) {
            throw new IllegalArgumentException("Invalid multicast address " + url.getHost() + ", scope: 224.0.0.0 - 239.255.255.255");
        }
        try {
            mutilcastAddress = InetAddress.getByName(url.getHost());
            mutilcastPort = url.getPort() <= 0 ? DEFAULT_MULTICAST_PORT : url.getPort();
            mutilcastSocket = new MulticastSocket(mutilcastPort);
            mutilcastSocket.setLoopbackMode(false);
            mutilcastSocket.joinGroup(mutilcastAddress);
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buf = new byte[2048];
                    DatagramPacket recv = new DatagramPacket(buf, buf.length);
                    while (!mutilcastSocket.isClosed()) {
                        try {
                            mutilcastSocket.receive(recv);
                            String msg = new String(recv.getData()).trim();
                            int i = msg.indexOf('\n');
                            if (i > 0) {
                                msg = msg.substring(0, i).trim();
                            }
                            MulticastRegistry.this.receive(msg, (InetSocketAddress) recv.getSocketAddress());
                            Arrays.fill(buf, (byte) 0);
                        } catch (Throwable e) {
                            if (!mutilcastSocket.isClosed()) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                    }
                }
            }, "DubboMulticastRegistryReceiver");
            thread.setDaemon(true);
            thread.start();
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        this.cleanPeriod = url.getParameter(Constants.SESSION_TIMEOUT_KEY, Constants.DEFAULT_SESSION_TIMEOUT);
        if (url.getParameter("clean", true)) {
            this.cleanFuture = cleanExecutor.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    try {
                        clean(); // Remove the expired
                    } catch (Throwable t) { // Defensive fault tolerance
                        logger.error("Unexpected exception occur at clean expired provider, cause: " + t.getMessage(), t);
                    }
                }
            }, cleanPeriod, cleanPeriod, TimeUnit.MILLISECONDS);
        } else {
            this.cleanFuture = null;
        }
    }

    private static boolean isMulticastAddress(String ip) {
        int i = ip.indexOf('.');
        if (i > 0) {
            String prefix = ip.substring(0, i);
            if (StringUtils.isInteger(prefix)) {
                int p = Integer.parseInt(prefix);
                return p >= 224 && p <= 239;
            }
        }
        return false;
    }

    /**
     * Remove the expired providers, only when "clean" parameter is true.
     */
    private void clean() {
        if (admin) {
            for (Set<URL> providers : new HashSet<Set<URL>>(received.values())) {
                for (URL url : new HashSet<URL>(providers)) {
                    if (isExpired(url)) {
                        if (logger.isWarnEnabled()) {
                            logger.warn("Clean expired provider " + url);
                        }
                        doUnregister(url);
                    }
                }
            }
        }
    }

    private boolean isExpired(URL url) {
        if (!url.getParameter(Constants.DYNAMIC_KEY, true)
                || url.getPort() <= 0
                || Constants.CONSUMER_PROTOCOL.equals(url.getProtocol())
                || Constants.ROUTE_PROTOCOL.equals(url.getProtocol())
                || Constants.OVERRIDE_PROTOCOL.equals(url.getProtocol())) {
            return false;
        }
        Socket socket = null;
        try {
            socket = new Socket(url.getHost(), url.getPort());
        } catch (Throwable e) {
            try {
                Thread.sleep(100);
            } catch (Throwable e2) {
            }
            Socket socket2 = null;
            try {
                socket2 = new Socket(url.getHost(), url.getPort());
            } catch (Throwable e2) {
                return true;
            } finally {
                if (socket2 != null) {
                    try {
                        socket2.close();
                    } catch (Throwable e2) {
                    }
                }
            }
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Throwable e) {
                }
            }
        }
        return false;
    }

    private void receive(String msg, InetSocketAddress remoteAddress) {
        if (logger.isInfoEnabled()) {
            logger.info("Receive multicast message: " + msg + " from " + remoteAddress);
        }
        if (msg.startsWith(Constants.REGISTER)) {
            URL url = URL.valueOf(msg.substring(Constants.REGISTER.length()).trim());
            registered(url);
        } else if (msg.startsWith(Constants.UNREGISTER)) {
            URL url = URL.valueOf(msg.substring(Constants.UNREGISTER.length()).trim());
            unregistered(url);
        } else if (msg.startsWith(Constants.SUBSCRIBE)) {
            URL url = URL.valueOf(msg.substring(Constants.SUBSCRIBE.length()).trim());
            Set<URL> urls = getRegistered();
            if (urls != null && !urls.isEmpty()) {
                for (URL u : urls) {
                    if (UrlUtils.isMatch(url, u)) {
                        String host = remoteAddress != null && remoteAddress.getAddress() != null
                                ? remoteAddress.getAddress().getHostAddress() : url.getIp();
                        if (url.getParameter("unicast", true) // Whether the consumer's machine has only one process
                                && !NetUtils.getLocalHost().equals(host)) { // Multiple processes in the same machine cannot be unicast with unicast or there will be only one process receiving information
                            unicast(Constants.REGISTER + " " + u.toFullString(), host);
                        } else {
                            broadcast(Constants.REGISTER + " " + u.toFullString());
                        }
                    }
                }
            }
        }/* else if (msg.startsWith(UNSUBSCRIBE)) {
        }*/
    }

    private void broadcast(String msg) {
        if (logger.isInfoEnabled()) {
            logger.info("Send broadcast message: " + msg + " to " + mutilcastAddress + ":" + mutilcastPort);
        }
        try {
            byte[] data = (msg + "\n").getBytes();
            DatagramPacket hi = new DatagramPacket(data, data.length, mutilcastAddress, mutilcastPort);
            mutilcastSocket.send(hi);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private void unicast(String msg, String host) {
        if (logger.isInfoEnabled()) {
            logger.info("Send unicast message: " + msg + " to " + host + ":" + mutilcastPort);
        }
        try {
            byte[] data = (msg + "\n").getBytes();
            DatagramPacket hi = new DatagramPacket(data, data.length, InetAddress.getByName(host), mutilcastPort);
            mutilcastSocket.send(hi);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    protected void doRegister(URL url) {
        broadcast(Constants.REGISTER + " " + url.toFullString());
    }

    @Override
    protected void doUnregister(URL url) {
        broadcast(Constants.UNREGISTER + " " + url.toFullString());
    }

    @Override
    protected void doSubscribe(URL url, NotifyListener listener) {
        if (Constants.ANY_VALUE.equals(url.getServiceInterface())) {
            admin = true;
        }
        broadcast(Constants.SUBSCRIBE + " " + url.toFullString());
        synchronized (listener) {
            try {
                listener.wait(url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT));
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    protected void doUnsubscribe(URL url, NotifyListener listener) {
        if (!Constants.ANY_VALUE.equals(url.getServiceInterface())
                && url.getParameter(Constants.REGISTER_KEY, true)) {
            unregister(url);
        }
        broadcast(Constants.UNSUBSCRIBE + " " + url.toFullString());
    }

    @Override
    public boolean isAvailable() {
        try {
            return mutilcastSocket != null;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Remove the expired providers(if clean is true), leave the multicast group and close the multicast socket.
     */
    @Override
    public void destroy() {
        super.destroy();
        try {
            if (cleanFuture != null) {
                cleanFuture.cancel(true);
            }
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
        try {
            mutilcastSocket.leaveGroup(mutilcastAddress);
            mutilcastSocket.close();
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
        ExecutorUtil.gracefulShutdown(cleanExecutor, cleanPeriod);
    }

    protected void registered(URL url) {
        for (Map.Entry<URL, Set<NotifyListener>> entry : getSubscribed().entrySet()) {
            URL key = entry.getKey();
            if (UrlUtils.isMatch(key, url)) {
                Set<URL> urls = received.get(key);
                if (urls == null) {
                    received.putIfAbsent(key, new ConcurrentHashSet<URL>());
                    urls = received.get(key);
                }
                urls.add(url);
                List<URL> list = toList(urls);
                for (NotifyListener listener : entry.getValue()) {
                    notify(key, listener, list);
                    synchronized (listener) {
                        listener.notify();
                    }
                }
            }
        }
    }

    protected void unregistered(URL url) {
        for (Map.Entry<URL, Set<NotifyListener>> entry : getSubscribed().entrySet()) {
            URL key = entry.getKey();
            if (UrlUtils.isMatch(key, url)) {
                Set<URL> urls = received.get(key);
                if (urls != null) {
                    urls.remove(url);
                }
                if (urls == null || urls.isEmpty()){
                    if (urls == null){
                        urls = new ConcurrentHashSet<URL>();
                    }
                    URL empty = url.setProtocol(Constants.EMPTY_PROTOCOL);
                    urls.add(empty);
                }
                List<URL> list = toList(urls);
                for (NotifyListener listener : entry.getValue()) {
                    notify(key, listener, list);
                }
            }
        }
    }

    protected void subscribed(URL url, NotifyListener listener) {
        List<URL> urls = lookup(url);
        notify(url, listener, urls);
    }

    private List<URL> toList(Set<URL> urls) {
        List<URL> list = new ArrayList<URL>();
        if (urls != null && !urls.isEmpty()) {
            for (URL url : urls) {
                list.add(url);
            }
        }
        return list;
    }

    @Override
    public void register(URL url) {
        super.register(url);
        registered(url);
    }

    @Override
    public void unregister(URL url) {
        super.unregister(url);
        unregistered(url);
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        super.subscribe(url, listener);
        subscribed(url, listener);
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        super.unsubscribe(url, listener);
        received.remove(url);
    }

    @Override
    public List<URL> lookup(URL url) {
        List<URL> urls = new ArrayList<URL>();
        Map<String, List<URL>> notifiedUrls = getNotified().get(url);
        if (notifiedUrls != null && notifiedUrls.size() > 0) {
            for (List<URL> values : notifiedUrls.values()) {
                urls.addAll(values);
            }
        }
        if (urls.isEmpty()) {
            List<URL> cacheUrls = getCacheUrls(url);
            if (cacheUrls != null && !cacheUrls.isEmpty()) {
                urls.addAll(cacheUrls);
            }
        }
        if (urls.isEmpty()) {
            for (URL u : getRegistered()) {
                if (UrlUtils.isMatch(url, u)) {
                    urls.add(u);
                }
            }
        }
        if (Constants.ANY_VALUE.equals(url.getServiceInterface())) {
            for (URL u : getSubscribed().keySet()) {
                if (UrlUtils.isMatch(url, u)) {
                    urls.add(u);
                }
            }
        }
        return urls;
    }

    public MulticastSocket getMutilcastSocket() {
        return mutilcastSocket;
    }

    public Map<URL, Set<URL>> getReceived() {
        return received;
    }

}
