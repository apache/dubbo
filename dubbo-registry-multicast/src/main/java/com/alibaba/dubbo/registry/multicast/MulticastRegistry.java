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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
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
    
    private static final String REGISTER = "register";

    private static final String UNREGISTER = "unregister";

    private static final String SUBSCRIBE = "subscribe";

    private static final String UNSUBSCRIBE = "unsubscribe";
    
    private final InetAddress mutilcastAddress;
    
    private final MulticastSocket mutilcastSocket;

    private final ConcurrentMap<String, Set<String>> notified = new ConcurrentHashMap<String, Set<String>>();

    public MulticastRegistry(URL url) {
        super(url);
        if (! isMulticastAddress(url.getHost())) {
            throw new IllegalArgumentException("Invalid multicast address " + url.getHost() + ", scope: 224.0.0.0 - 239.255.255.255");
        }
        try {
            mutilcastAddress = InetAddress.getByName(url.getHost());
            mutilcastSocket = new MulticastSocket(url.getPort());
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
                        } catch (IOException e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
            }, "MulticastRegistryReceiver");
            thread.setDaemon(true);
            thread.start();
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
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

    private void receive(String msg, InetSocketAddress remoteAddress) {
        if (logger.isInfoEnabled()) {
            logger.info("Receive multicast message: " + msg + " from " + remoteAddress);
        }
        if (msg.startsWith(REGISTER)) {
            URL url = URL.valueOf(msg.substring(REGISTER.length()).trim());
            registered(url);
        } else if (msg.startsWith(UNREGISTER)) {
            URL url = URL.valueOf(msg.substring(UNREGISTER.length()).trim());
            unregistered(url);
        } else if (msg.startsWith(SUBSCRIBE)) {
            URL url = URL.valueOf(msg.substring(SUBSCRIBE.length()).trim());
            List<URL> urls = lookup(url);
            if (urls != null && urls.size() > 0) {
                for (URL u : urls) {
                    String host = remoteAddress != null && remoteAddress.getAddress() != null 
                            ? remoteAddress.getAddress().getHostAddress() : url.getHost();
                    if (url.getParameter("uni", true) // 消费者的机器是否只有一个进程
                            && ! NetUtils.getLocalHost().equals(host)) { // 同机器多进程不能用unicast单播信息，否则只会有一个进程收到信息
                        unicast(REGISTER + " " + u.toFullString(), host);
                    } else {
                        broadcast(REGISTER + " " + u.toFullString());
                    }
                }
            }
        } else if (msg.startsWith(UNSUBSCRIBE)) {
        }
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
        broadcast(REGISTER + " " + url.toFullString());
    }

    protected void doUnregister(URL url) {
        broadcast(UNREGISTER + " " + url.toFullString());
    }

    protected void doSubscribe(URL url, NotifyListener listener) {
        if (! Constants.ANY_VALUE.equals(url.getServiceName())
                && url.getParameter(Constants.REGISTER_KEY, true)) {
            register(url);
        }
        broadcast(SUBSCRIBE + " " + url.toFullString());
        synchronized (listener) {
            try {
                listener.wait(url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT));
            } catch (InterruptedException e) {
            }
        }
    }

    protected void doUnsubscribe(URL url, NotifyListener listener) {
        if (! Constants.ANY_VALUE.equals(url.getServiceName())
                && url.getParameter(Constants.REGISTER_KEY, true)) {
            unregister(url);
        }
        broadcast(UNSUBSCRIBE + " " + url.toFullString());
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
                if (list != null && list.size() > 0) {
                    for (NotifyListener listener : entry.getValue()) {
                        notify(subscribe, listener, list);
                        synchronized (listener) {
                            listener.notify();
                        }
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
                if (list != null && list.size() > 0) {
                    for (NotifyListener listener : entry.getValue()) {
                        notify(subscribe, listener, list);
                    }
                }
            }
        }
    }

    protected void subscribed(URL url, NotifyListener listener) {
        List<URL> urls = lookup(url);
        if (urls != null && urls.size() > 0) {
            notify(url, listener, urls);
        }
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

    public void register(URL url) {
        super.register(url);
        registered(url);
    }

    public void unregister(URL url) {
        super.unregister(url);
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

}