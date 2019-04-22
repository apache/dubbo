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
package org.apache.dubbo.registry.consul;

import com.pszymczyk.consul.ConsulProcess;
import com.pszymczyk.consul.ConsulStarterBuilder;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.status.Status;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.status.RegistryStatusChecker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class ConsulRegistryTest {

    private static ConsulProcess consul;
    private ConsulRegistry consulRegistry;
    private String service = "org.apache.dubbo.test.injvmServie";
    private URL serviceUrl = URL.valueOf("consul://127.0.0.1:8012/" + service + "?notify=false&methods=test1,test2");
    private URL registryUrl;
    private ConsulRegistryFactory consulRegistryFactory;

    @BeforeEach
    public void setUp() throws Exception {
        this.consul = ConsulStarterBuilder.consulStarter()
                .build()
                .start();
        this.registryUrl = URL.valueOf("consul://localhost:" + consul.getHttpPort());

        consulRegistryFactory = new ConsulRegistryFactory();
        this.consulRegistry = (ConsulRegistry) consulRegistryFactory.createRegistry(registryUrl);
    }

    @AfterEach
    public void tearDown() throws Exception {
        consul.close();
        this.consulRegistry.destroy();
    }

    @Test
    public void testRegister() {
        Set<URL> registered;

        for (int i = 0; i < 2; i++) {
            consulRegistry.register(serviceUrl);
            registered = consulRegistry.getRegistered();
            assertThat(registered.contains(serviceUrl), is(true));
        }

        registered = consulRegistry.getRegistered();

        assertThat(registered.size(), is(1));
    }

    @Test
    public void testSubscribe() {
        NotifyListener listener = mock(NotifyListener.class);
        consulRegistry.subscribe(serviceUrl, listener);

        Map<URL, Set<NotifyListener>> subscribed = consulRegistry.getSubscribed();
        assertThat(subscribed.size(), is(1));
        assertThat(subscribed.get(serviceUrl).size(), is(1));

        consulRegistry.unsubscribe(serviceUrl, listener);
        subscribed = consulRegistry.getSubscribed();
        assertThat(subscribed.size(), is(1));
        assertThat(subscribed.get(serviceUrl).size(), is(0));
    }

    @Test
    public void testAvailable() {
        consulRegistry.register(serviceUrl);
        assertThat(consulRegistry.isAvailable(), is(true));

//        consulRegistry.destroy();
//        assertThat(consulRegistry.isAvailable(), is(false));
    }

    @Test
    public void testLookup() throws InterruptedException {
        List<URL> lookup = consulRegistry.lookup(serviceUrl);
        assertThat(lookup.size(), is(0));

        consulRegistry.register(serviceUrl);
        Thread.sleep(5000);
        lookup = consulRegistry.lookup(serviceUrl);
        assertThat(lookup.size(), is(1));
    }

    @Test
    public void testStatusChecker() {
        RegistryStatusChecker registryStatusChecker = new RegistryStatusChecker();
        Status status = registryStatusChecker.check();
        assertThat(status.getLevel(), is(Status.Level.UNKNOWN));

        Registry registry = consulRegistryFactory.getRegistry(registryUrl);
        assertThat(registry, not(nullValue()));

        status = registryStatusChecker.check();
        assertThat(status.getLevel(), is(Status.Level.OK));

        registry.register(serviceUrl);
        status = registryStatusChecker.check();
        assertThat(status.getLevel(), is(Status.Level.OK));
    }

}
