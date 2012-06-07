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
package com.alibaba.dubbo.registry.zookeeper;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.alibaba.dubbo.common.URL;

/**
 * ZookeeperRegistryTest
 * 
 * @author tony.chenl
 */
public class ZookeeperRegistryTest {

    String            service     = "com.alibaba.dubbo.test.injvmServie";
    URL               registryUrl = URL.valueOf("zookeeper://239.255.255.255/");
    URL               serviceUrl  = URL.valueOf("zookeeper://zookeeper/" + service
                                                + "?notify=false&methods=test1,test2");
    URL               consumerUrl = URL.valueOf("zookeeper://consumer/" + service + "?notify=false&methods=test1,test2");
    // ZookeeperRegistry registry    = new ZookeeperRegistry(registryUrl);

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        //registry.register(service, serviceUrl);
    }

    /*@Test(expected = IllegalStateException.class)
    public void testUrlerror() {
        URL errorUrl = URL.valueOf("zookeeper://zookeeper/");
        new ZookeeperRegistry(errorUrl);
    }*/
    
    @Test
    public void testDefaultPort() {
        Assert.assertEquals("10.20.153.10:2181", ZookeeperRegistry.appendDefaultPort("10.20.153.10:0"));
        Assert.assertEquals("10.20.153.10:2181", ZookeeperRegistry.appendDefaultPort("10.20.153.10"));
    }

    /**
     * Test method for {@link com.alibaba.dubbo.registry.support.injvm.InjvmRegistry#register(java.util.Map)}.
     */
    @Test
    public void testRegister() {
        /*List<URL> registered = null;
        // clear first
        registered = registry.getRegistered(service);

        for (int i = 0; i < 2; i++) {
            registry.register(service, serviceUrl);
            registered = registry.getRegistered(service);
            assertTrue(registered.contains(serviceUrl));
        }
        // confirm only 1 regist success;
        registered = registry.getRegistered(service);
        assertEquals(1, registered.size());*/
    }

    /**
     * Test method for
     * {@link com.alibaba.dubbo.registry.support.injvm.InjvmRegistry#subscribe(java.util.Map, com.alibaba.dubbo.registry.support.NotifyListener)}
     * .
     */
    @Test
    public void testSubscribe() {
        /*final String subscribearg = "arg1=1&arg2=2";
        // verify lisener.
        final AtomicReference<Map<String, String>> args = new AtomicReference<Map<String, String>>();
        registry.subscribe(service, new URL("dubbo", NetUtils.getLocalHost(), 0, StringUtils.parseQueryString(subscribearg)), new NotifyListener() {

            public void notify(List<URL> urls) {
                // FIXME assertEquals(ZookeeperRegistry.this.service, service);
                args.set(urls.get(0).getParameters());
            }
        });
        assertEquals(serviceUrl.toParameterString(), StringUtils.toQueryString(args.get()));
        Map<String, String> arg = registry.getSubscribed(service);
        assertEquals(subscribearg, StringUtils.toQueryString(arg));*/

    }

}