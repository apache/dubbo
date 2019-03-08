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
package com.alibaba.dubbo.registry.multicast;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class MulticastRegistryTest {

    private String service = "com.alibaba.dubbo.test.injvmServie";
    private URL registryUrl = URL.valueOf("multicast://239.255.255.255/");
    private URL serviceUrl = URL.valueOf("dubbo://" + NetUtils.getLocalHost() + "/" + service
            + "?methods=test1,test2");
    private URL adminUrl = URL.valueOf("dubbo://" + NetUtils.getLocalHost() + "/*");
    private URL consumerUrl = URL.valueOf("subscribe://" + NetUtils.getLocalHost() + "/" + service + "?arg1=1&arg2=2");
    private MulticastRegistry registry = new MulticastRegistry(registryUrl);

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        registry.register(serviceUrl);
    }

    @Test(expected = IllegalStateException.class)
    public void testUrlError() {
        URL errorUrl = URL.valueOf("multicast://mullticast/");
        new MulticastRegistry(errorUrl);
    }

    @Test(expected = IllegalStateException.class)
    public void testAnyHost() {
        URL errorUrl = URL.valueOf("multicast://0.0.0.0/");
        new MulticastRegistry(errorUrl);
    }

    @Test
    public void testGetCustomPort() {
        URL customPortUrl = URL.valueOf("multicast://239.255.255.255:4321/");
        MulticastRegistry multicastRegistry = new MulticastRegistry(customPortUrl);
        assertThat(multicastRegistry.getUrl().getPort(), is(4321));
    }

    /**
     * Test method for {@link com.alibaba.dubbo.registry.multicast.MulticastRegistry#getRegistered()}.
     */
    @Test
    public void testRegister() {
        Set<URL> registered = null;
        // clear first
        registered = registry.getRegistered();

        for (int i = 0; i < 2; i++) {
            registry.register(serviceUrl);
            registered = registry.getRegistered();
            assertTrue(registered.contains(serviceUrl));
        }
        // confirm only 1 regist success;
        registered = registry.getRegistered();
        assertEquals(1, registered.size());
    }

    /**
     * Test method for
     * {@link com.alibaba.dubbo.registry.multicast.MulticastRegistry#subscribe(URL url, com.alibaba.dubbo.registry.NotifyListener)}
     * .
     */
    @Test
    public void testSubscribe() {
        // verify lisener.
        final AtomicReference<URL> args = new AtomicReference<URL>();
        registry.subscribe(consumerUrl, new NotifyListener() {

            @Override
            public void notify(List<URL> urls) {
                // FIXME assertEquals(MulticastRegistry.this.service, service);
                args.set(urls.get(0));
            }
        });
        assertEquals(serviceUrl.toFullString(), args.get().toFullString());
        Map<URL, Set<NotifyListener>> arg = registry.getSubscribed();
        assertEquals(consumerUrl, arg.keySet().iterator().next());

    }

    @Test
    public void testDefaultPort() {
        MulticastRegistry multicastRegistry = new MulticastRegistry(URL.valueOf("multicast://224.5.6.7"));
        try {
            MulticastSocket multicastSocket = multicastRegistry.getMutilcastSocket();
            Assert.assertEquals(1234, multicastSocket.getLocalPort());
        } finally {
            multicastRegistry.destroy();
        }
    }

    @Test
    public void testMulticastAddress() {
        InetAddress multicastAddress = null;
        MulticastSocket multicastSocket = null;
        try {
            // ipv4 multicast address
            multicastAddress = InetAddress.getByName("224.55.66.77");
            multicastSocket = new MulticastSocket(2345);
            multicastSocket.setLoopbackMode(false);
            NetUtils.setInterface(multicastSocket, false);
            multicastSocket.joinGroup(multicastAddress);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            if (multicastSocket != null) {
                multicastSocket.close();
            }
        }

        // multicast ipv6 address,
        try {
            multicastAddress = InetAddress.getByName("ff01::1");

            multicastSocket = new MulticastSocket();
            multicastSocket.setLoopbackMode(false);
            NetUtils.setInterface(multicastSocket, true);
            multicastSocket.joinGroup(multicastAddress);
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (multicastSocket != null) {
                multicastSocket.close();
            }
        }

    }


}
