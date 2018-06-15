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
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.MulticastSocket;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class MulticastRegistryTest {

    private String service = "org.apache.dubbo.test.injvmServie";
    private URL registryUrl = URL.valueOf("multicast://239.255.255.255/");
    private URL serviceUrl = URL.valueOf("dubbo://" + NetUtils.getLocalHost() + "/" + service
            + "?methods=test1,test2");
    private URL adminUrl = URL.valueOf("dubbo://" + NetUtils.getLocalHost() + "/*");
    private URL consumerUrl = URL.valueOf("subscribe://" + NetUtils.getLocalHost() + "/" + service + "?arg1=1&arg2=2");
    private MulticastRegistry registry = new MulticastRegistry(registryUrl);

    @Before
    public void setUp() {
        registry.register(serviceUrl);
    }

    /**
     * Test method for {@link org.apache.dubbo.registry.multicast.MulticastRegistry#MulticastRegistry(URL)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUrlError() {
        URL errorUrl = URL.valueOf("multicast://mullticast/");
        new MulticastRegistry(errorUrl);
    }

    /**
     * Test method for {@link org.apache.dubbo.registry.multicast.MulticastRegistry#MulticastRegistry(URL)}.
     */
    @Test(expected = IllegalStateException.class)
    public void testAnyHost() {
        URL errorUrl = URL.valueOf("multicast://0.0.0.0/");
        new MulticastRegistry(errorUrl);
    }

    /**
     * Test method for {@link org.apache.dubbo.registry.multicast.MulticastRegistry#MulticastRegistry(URL)}.
     */
    @Test
    public void testGetCustomPort() {
        int port = NetUtils.getAvailablePort();
        URL customPortUrl = URL.valueOf("multicast://239.255.255.255:" + port);
        MulticastRegistry multicastRegistry = new MulticastRegistry(customPortUrl);
        assertThat(multicastRegistry.getUrl().getPort(), is(port));
    }

    /**
     * Test method for {@link org.apache.dubbo.registry.multicast.MulticastRegistry#getRegistered()}.
     */
    @Test
    public void testRegister() {
        Set<URL> registered;
        // clear first
        registered = registry.getRegistered();
        registered.clear();

        for (int i = 0; i < 2; i++) {
            registry.register(serviceUrl);
            registered = registry.getRegistered();
            assertTrue(registered.contains(serviceUrl));
        }
        // confirm only 1 register success
        registered = registry.getRegistered();
        assertEquals(1, registered.size());
    }

    /**
     * Test method for {@link org.apache.dubbo.registry.multicast.MulticastRegistry#unregister(URL)}.
     */
    @Test
    public void testUnregister() {
        Set<URL> registered;

        // register first
        registry.register(serviceUrl);
        registered = registry.getRegistered();
        assertTrue(registered.contains(serviceUrl));

        // then unregister
        registered = registry.getRegistered();
        registry.unregister(serviceUrl);
        assertFalse(registered.contains(serviceUrl));
    }

    /**
     * Test method for
     * {@link org.apache.dubbo.registry.multicast.MulticastRegistry#subscribe(URL url, org.apache.dubbo.registry.NotifyListener)}
     * .
     */
    @Test
    public void testSubscribe() {
        // verify listener
        registry.subscribe(consumerUrl, new NotifyListener() {
            @Override
            public void notify(List<URL> urls) {
                assertEquals(serviceUrl.toFullString(), urls.get(0).toFullString());

                Map<URL, Set<NotifyListener>> subscribed = registry.getSubscribed();
                assertEquals(consumerUrl, subscribed.keySet().iterator().next());
            }
        });
    }

    /**
     * Test method for {@link org.apache.dubbo.registry.multicast.MulticastRegistry#unsubscribe(URL, NotifyListener)}
     */
    @Test
    public void testUnsubscribe() {
        // subscribe first
        registry.subscribe(consumerUrl, new NotifyListener() {
            @Override
            public void notify(List<URL> urls) {
                // do nothing
            }
        });

        // then unsubscribe
        registry.unsubscribe(consumerUrl, new NotifyListener() {
            @Override
            public void notify(List<URL> urls) {
                Map<URL, Set<NotifyListener>> subscribed = registry.getSubscribed();
                Set<NotifyListener> listeners = subscribed.get(consumerUrl);
                assertTrue(listeners.isEmpty());

                Map<URL, Set<URL>> received = registry.getReceived();
                assertTrue(received.get(consumerUrl).isEmpty());
            }
        });
    }

    /**
     * Test method for {@link MulticastRegistry#isAvailable()}
     */
    @Test
    public void testAvailability() {
        int port = NetUtils.getAvailablePort();
        MulticastRegistry registry = new MulticastRegistry(URL.valueOf("multicast://224.5.6.8:" + port));
        assertTrue(registry.isAvailable());
    }

    /**
     * Test method for {@link MulticastRegistry#destroy()}
     */
    @Test
    public void testDestroy() {
        MulticastSocket socket = registry.getMutilcastSocket();
        assertFalse(socket.isClosed());

        // then destroy, the multicast socket will be closed
        registry.destroy();
        socket = registry.getMutilcastSocket();
        assertTrue(socket.isClosed());
    }

    /**
     * Test method for {@link org.apache.dubbo.registry.multicast.MulticastRegistry#MulticastRegistry(URL)}
     */
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

    /**
     * Test method for {@link org.apache.dubbo.registry.multicast.MulticastRegistry#MulticastRegistry(URL)}
     */
    @Test
    public void testCustomedPort() {
        int port = NetUtils.getAvailablePort();
        MulticastRegistry multicastRegistry = new MulticastRegistry(URL.valueOf("multicast://224.5.6.7:" + port));
        try {
            MulticastSocket multicastSocket = multicastRegistry.getMutilcastSocket();
            assertEquals(port, multicastSocket.getLocalPort());
        } finally {
            multicastRegistry.destroy();
        }
    }

}
