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
import java.util.List;
import java.util.Map;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.support.AbstractRegistry;

/**
 * MulticastRegistry
 * 
 * @author william.liangf
 */
public class MulticastRegistry extends AbstractRegistry {

    // 日志输出
    private static final Logger logger = LoggerFactory.getLogger(MulticastRegistry.class);
    
    private InetAddress mutilcastAddress;
    
    private MulticastSocket mutilcastSocket;

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
                    byte[] buf = new byte[1024];
                    DatagramPacket recv = new DatagramPacket(buf, buf.length);
                    while (true) {
                        try {
                            mutilcastSocket.receive(recv);
                            MulticastRegistry.this.receive(new String(recv.getData()).trim(), (InetSocketAddress) recv.getSocketAddress());
                        } catch (IOException e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
            }, "MulticastRegistry");
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
        if (msg.startsWith("register")) {
            String[] parts = msg.split("\\s+");
            String service = parts[1];
            if (getSubscribed().containsKey(service)) {
                URL url = URL.valueOf(parts[2]);
                List<URL> urls = new ArrayList<URL>();
                List<URL> notified = getNotified().get(service);
                if (notified != null) {
                    urls.addAll(notified);
                }
                if (! urls.contains(url)) {
                    urls.add(url);
                }
                notify(service, urls);
            }
        } else if (msg.startsWith("subscribe")) {
            String[] parts = msg.split("\\s+");
            String service = parts[1];
            if (getRegistered().containsKey(service)) {
                for (URL url : getRegistered().get(service)) {
                    send("register " + service + " " + url.toString());
                }
            }
        }
    }
    
    private void send(String msg) {
        DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(), mutilcastAddress, mutilcastSocket.getLocalPort());
        try {
            mutilcastSocket.send(hi);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public void register(String service, URL url) {
        super.register(service, url);
        send("register " + service + " " + url);
    }

    public void unregister(String service, URL url) {
        send("unregister " + service + " " + url);
    }

    public void subscribe(String service, Map<String, String> parameters, NotifyListener listener) {
        super.subscribe(service, parameters, listener);
        send("subscribe " + service + " " + StringUtils.toQueryString(parameters));
        synchronized (this) {
            try {
                this.wait(5000);
            } catch (InterruptedException e) {
            }
        }
    }

    public void unsubscribe(String service, Map<String, String> query, NotifyListener listener) {
        send("unsubscribe " + service + " " + StringUtils.toQueryString(query));
    }

    public boolean isAvailable() {
        return mutilcastSocket.isConnected();
    }

    @Override
    public void destroy() {
        mutilcastSocket.close();
    }

}