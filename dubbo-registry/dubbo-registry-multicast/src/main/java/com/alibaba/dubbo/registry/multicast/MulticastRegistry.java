/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.registry.multicast;

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

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.support.FailbackRegistry;

/**
 * MulticastRegistry
 * 
 * @author william.liangf
 */
public class MulticastRegistry extends FailbackRegistry {

    // 日志输出
    private static final Logger logger = LoggerFactory.getLogger(MulticastRegistry.class);

    private static final int DEFAULT_MULTICAST_PORT = 1234;

    private final InetAddress mutilcastAddress;
    
    private final MulticastSocket mutilcastSocket;

    private final ConcurrentMap<String, Set<String>> notified = new ConcurrentHashMap<String, Set<String>>();

    private final ScheduledExecutorService cleanExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("DubboMulticastRegistryCleanTimer", true));

    private final ScheduledFuture<?> cleanFuture;

    private final int cleanPeriod;
    
    private volatile boolean admin = false;

    public MulticastRegistry(URL url) {
        super(url);
        if (! isMulticastAddress(url.getHost())) {
            throw new IllegalArgumentException("Invalid multicast address " + url.getHost() + ", scope: 224.0.0.0 - 239.255.255.255");
        }
        try {
            mutilcastAddress = InetAddress.getByName(url.getHost());
            mutilcastSocket = new MulticastSocket(url.getPort() == 0 ? DEFAULT_MULTICAST_PORT : url.getPort());
            mutilcastSocket.setLoopbackMode(false);
            mutilcastSocket.joinGroup(mutilcastAddress);
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    byte[] buf = new byte[2048];
                    DatagramPacket recv = new DatagramPacket(buf, buf.length);
                    while (! mutilcastSocket.isClosed()) {
                        try {
                            mutilcastSocket.receive(recv);
                            String msg = new String(recv.getData()).trim();
                            int i = msg.indexOf('\n');
                            if (i > 0) {
                                msg = msg.substring(0, i).trim();
                            }
                            MulticastRegistry.this.receive(msg, (InetSocketAddress) recv.getSocketAddress());
                            Arrays.fill(buf, (byte)0);
                        } catch (Throwable e) {
                            if (! mutilcastSocket.isClosed()) {
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
                public void run() {
                    try {
                        clean(); // 清除过期者
                    } catch (Throwable t) { // 防御性容错
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
    
    private void clean() {
        if (admin) {
            for (Set<String> providers : new HashSet<Set<String>>(notified.values())) {
                for (String provider : new HashSet<String>(providers)) {
                    URL url = URL.valueOf(provider);
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
        if (Constants.SUBSCRIBE_PROTOCOL.equals(url.getProtocol())
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
            List<URL> urls = lookup(url);
            if (urls != null && urls.size() > 0) {
                for (URL u : urls) {
                    String host = remoteAddress != null && remoteAddress.getAddress() != null 
                            ? remoteAddress.getAddress().getHostAddress() : url.getIp();
                    if (url.getParameter("unicast", true) // 消费者的机器是否只有一个进程
                            && ! NetUtils.getLocalHost().equals(host)) { // 同机器多进程不能用unicast单播信息，否则只会有一个进程收到信息
                        unicast(Constants.REGISTER + " " + u.toFullString(), host);
                    } else {
                        broadcast(Constants.REGISTER + " " + u.toFullString());
                    }
                }
            }
        }/* else if (msg.startsWith(UNSUBSCRIBE)) {
        }*/
    }
    
    private void broadcast(String msg) {
        if (logger.isInfoEnabled()) {
            logger.info("Send broadcast message: " + msg + " to " + mutilcastAddress + ":" + mutilcastSocket.getLocalPort());
        }
        try {
            byte[] data = (msg + "\n").getBytes();
            DatagramPacket hi = new DatagramPacket(data, data.length, mutilcastAddress, mutilcastSocket.getLocalPort());
            mutilcastSocket.send(hi);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
    
    private void unicast(String msg, String host) {
        if (logger.isInfoEnabled()) {
            logger.info("Send unicast message: " + msg + " to " + host + ":" + mutilcastSocket.getLocalPort());
        }
        try {
            byte[] data = (msg + "\n").getBytes();
            DatagramPacket hi = new DatagramPacket(data, data.length, InetAddress.getByName(host), mutilcastSocket.getLocalPort());
            mutilcastSocket.send(hi);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
    
    protected void doRegister(URL url) {
        broadcast(Constants.REGISTER + " " + url.toFullString());
    }

    protected void doUnregister(URL url) {
        broadcast(Constants.UNREGISTER + " " + url.toFullString());
    }

    protected void doSubscribe(URL url, NotifyListener listener) {
        if (Constants.ANY_VALUE.equals(url.getServiceInterface())) {
            admin = true;
        }
        if (! Constants.ANY_VALUE.equals(url.getServiceInterface())
                && url.getParameter(Constants.REGISTER_KEY, true)) {
            register(url, null);
        }
        broadcast(Constants.SUBSCRIBE + " " + url.toFullString());
        synchronized (listener) {
            try {
                listener.wait(url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT));
            } catch (InterruptedException e) {
            }
        }
    }

    protected void doUnsubscribe(URL url, NotifyListener listener) {
        if (! Constants.ANY_VALUE.equals(url.getServiceInterface())
                && url.getParameter(Constants.REGISTER_KEY, true)) {
            unregister(url, null);
        }
        broadcast(Constants.UNSUBSCRIBE + " " + url.toFullString());
    }

    public boolean isAvailable() {
        try {
            return mutilcastSocket != null;
        } catch (Throwable t) {
            return false;
        }
    }

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
    }

    protected void registered(URL url) {
        for (Map.Entry<String, Set<NotifyListener>> entry : getSubscribed().entrySet()) {
            String key = entry.getKey();
            URL subscribe = URL.valueOf(key);
            if (UrlUtils.isMatch(subscribe, url)) {
                Set<String> urls = notified.get(key);
                if (urls == null) {
                    notified.putIfAbsent(key, new ConcurrentHashSet<String>());
                    urls = notified.get(key);
                }
                urls.add(url.toFullString());
                List<URL> list = toList(urls);
                for (NotifyListener listener : entry.getValue()) {
                    notify(subscribe, listener, list);
                    synchronized (listener) {
                        listener.notify();
                    }
                }
            }
        }
    }

    protected void unregistered(URL url) {
        for (Map.Entry<String, Set<NotifyListener>> entry : getSubscribed().entrySet()) {
            String key = entry.getKey();
            URL subscribe = URL.valueOf(key);
            if (UrlUtils.isMatch(subscribe, url)) {
                Set<String> urls = notified.get(key);
                if (urls != null) {
                    urls.remove(url.toFullString());
                }
                List<URL> list = toList(urls);
                for (NotifyListener listener : entry.getValue()) {
                    notify(subscribe, listener, list);
                }
            }
        }
    }

    protected void subscribed(URL url, NotifyListener listener) {
        List<URL> urls = lookup(url);
        notify(url, listener, urls);
    }

    private List<URL> toList(Set<String> urls) {
        List<URL> list = new ArrayList<URL>();
        if (urls != null && urls.size() > 0) {
            for (String url : urls) {
                list.add(URL.valueOf(url));
            }
        }
        return list;
    }

    public void register(URL url, NotifyListener listener) {
        super.register(url, listener);
        registered(url);
    }

    public void unregister(URL url, NotifyListener listener) {
        super.unregister(url, listener);
        unregistered(url);
    }

    public void subscribe(URL url, NotifyListener listener) {
        super.subscribe(url, listener);
        subscribed(url, listener);
    }

    public void unsubscribe(URL url, NotifyListener listener) {
        super.unsubscribe(url, listener);
        notified.remove(url.toFullString());
    }

    public Map<String, Set<String>> getNotified() {
        return notified;
    }

    public MulticastSocket getMutilcastSocket() {
        return mutilcastSocket;
    }

}