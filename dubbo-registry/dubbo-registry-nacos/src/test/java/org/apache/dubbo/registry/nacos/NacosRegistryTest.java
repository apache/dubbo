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
package org.apache.dubbo.registry.nacos;


import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.alibaba.nacos.client.naming.NacosNamingService;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.nacos.util.NacosNamingServiceUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_CATEGORY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doNothing;

public class NacosRegistryTest {


    private NacosRegistryFactory nacosRegistryFactory;

    private NacosRegistry nacosRegistry;

    private static final String serviceInterface = "org.apache.dubbo.registry.nacos.NacosService";

    private final URL serviceUrl = URL.valueOf("nacos://127.0.0.1:3333/" + serviceInterface + "?interface=" +
            serviceInterface + "&notify=false&methods=test1,test2&category=providers&version=1.0.0&group=default");

    private URL registryUrl;


    @BeforeEach
    public void setUp() throws Exception {

        int nacosServerPort = NetUtils.getAvailablePort();

        this.registryUrl = URL.valueOf("nacos://localhost:" + nacosServerPort);

        this.nacosRegistryFactory = new NacosRegistryFactory();

        this.nacosRegistry = (NacosRegistry) nacosRegistryFactory.createRegistry(registryUrl);

    }


    @AfterEach
    public void tearDown() throws Exception {
    }

    @Test
    public void testRegister() {
        NamingService namingService = mock(NacosNamingService.class);
        try {

            String serviceName = "providers:org.apache.dubbo.registry.nacos.NacosService:1.0.0:default";
            String category = this.serviceUrl.getParameter(CATEGORY_KEY, DEFAULT_CATEGORY);
            URL newUrl = this.serviceUrl.addParameter(CATEGORY_KEY, category);
            newUrl = newUrl.addParameter(PROTOCOL_KEY, this.serviceUrl.getProtocol());
            newUrl = newUrl.addParameter(PATH_KEY, this.serviceUrl.getPath());
            newUrl = newUrl.addParameters(NacosNamingServiceUtils.getNacosPreservedParam(this.serviceUrl));
            String ip = newUrl.getHost();
            int port = newUrl.getPort();
            Instance instance = new Instance();
            instance.setIp(ip);
            instance.setPort(port);
            instance.setMetadata(new HashMap<>(newUrl.getParameters()));
            doNothing().when(namingService).registerInstance(serviceName,
                    Constants.DEFAULT_GROUP, instance);
        } catch (NacosException e) {
            // ignore
        }

        NacosNamingServiceWrapper nacosNamingServiceWrapper = new
                NacosNamingServiceWrapper(namingService);
        nacosRegistry = new NacosRegistry(this.registryUrl, nacosNamingServiceWrapper);

        Set<URL> registered;
        for (int i = 0; i < 2; i++) {
            nacosRegistry.register(serviceUrl);
            registered = nacosRegistry.getRegistered();
            assertThat(registered.contains(serviceUrl), is(true));
        }

        registered = nacosRegistry.getRegistered();
        assertThat(registered.size(), is(1));

    }

    @Test
    public void testUnRegister() {
        NamingService namingService = mock(NacosNamingService.class);

        try {

            String serviceName = "providers:org.apache.dubbo.registry.nacos.NacosService:1.0.0:default";
            String category = this.serviceUrl.getParameter(CATEGORY_KEY, DEFAULT_CATEGORY);
            URL newUrl = this.serviceUrl.addParameter(CATEGORY_KEY, category);
            newUrl = newUrl.addParameter(PROTOCOL_KEY, this.serviceUrl.getProtocol());
            newUrl = newUrl.addParameter(PATH_KEY, this.serviceUrl.getPath());
            newUrl = newUrl.addParameters(NacosNamingServiceUtils.getNacosPreservedParam(this.serviceUrl));
            String ip = newUrl.getHost();
            int port = newUrl.getPort();
            Instance instance = new Instance();
            instance.setIp(ip);
            instance.setPort(port);
            instance.setMetadata(new HashMap<>(newUrl.getParameters()));
            doNothing().when(namingService).registerInstance(serviceName,
                    Constants.DEFAULT_GROUP, instance);

            doNothing().when(namingService).deregisterInstance(serviceName,
                    Constants.DEFAULT_GROUP, ip, port);
        } catch (NacosException e) {
            // ignore
        }

        NacosNamingServiceWrapper nacosNamingServiceWrapper = new
                NacosNamingServiceWrapper(namingService);
        nacosRegistry = new NacosRegistry(this.registryUrl, nacosNamingServiceWrapper);

        nacosRegistry.register(serviceUrl);
        Set<URL> registered = nacosRegistry.getRegistered();

        assertThat(registered.contains(serviceUrl), is(true));
        assertThat(registered.size(), is(1));

        nacosRegistry.unregister(serviceUrl);
        assertThat(registered.contains(serviceUrl), is(false));
        assertThat(registered.size(), is(0));

    }

    @Test
    public void testSubscribe() {
        NamingService namingService = mock(NacosNamingService.class);

        try {

            String serviceName = "providers:org.apache.dubbo.registry.nacos.NacosService:1.0.0:default";
            String category = this.serviceUrl.getParameter(CATEGORY_KEY, DEFAULT_CATEGORY);
            URL newUrl = this.serviceUrl.addParameter(CATEGORY_KEY, category);
            newUrl = newUrl.addParameter(PROTOCOL_KEY, this.serviceUrl.getProtocol());
            newUrl = newUrl.addParameter(PATH_KEY, this.serviceUrl.getPath());
            newUrl = newUrl.addParameters(NacosNamingServiceUtils.getNacosPreservedParam(this.serviceUrl));
            String ip = newUrl.getHost();
            int port = newUrl.getPort();
            Instance instance = new Instance();
            instance.setIp(ip);
            instance.setPort(port);
            instance.setMetadata(new HashMap<>(newUrl.getParameters()));

            List<Instance> instances = new ArrayList<>();
            instances.add(instance);
            when(namingService.getAllInstances(serviceName,
                    this.registryUrl.getParameter(GROUP_KEY, Constants.DEFAULT_GROUP))).thenReturn(instances);
        } catch (NacosException e) {
            // ignore
        }

        NacosNamingServiceWrapper nacosNamingServiceWrapper = new
                NacosNamingServiceWrapper(namingService);
        nacosRegistry = new NacosRegistry(this.registryUrl, nacosNamingServiceWrapper);

        NotifyListener listener = mock(NotifyListener.class);
        nacosRegistry.subscribe(serviceUrl, listener);

        Map<URL, Set<NotifyListener>> subscribed = nacosRegistry.getSubscribed();
        assertThat(subscribed.size(), is(1));
        assertThat(subscribed.get(serviceUrl).size(), is(1));

    }

    @Test
    public void testUnSubscribe() {
        NamingService namingService = mock(NacosNamingService.class);

        try {

            String serviceName = "providers:org.apache.dubbo.registry.nacos.NacosService:1.0.0:default";
            String category = this.serviceUrl.getParameter(CATEGORY_KEY, DEFAULT_CATEGORY);
            URL newUrl = this.serviceUrl.addParameter(CATEGORY_KEY, category);
            newUrl = newUrl.addParameter(PROTOCOL_KEY, this.serviceUrl.getProtocol());
            newUrl = newUrl.addParameter(PATH_KEY, this.serviceUrl.getPath());
            newUrl = newUrl.addParameters(NacosNamingServiceUtils.getNacosPreservedParam(this.serviceUrl));
            String ip = newUrl.getHost();
            int port = newUrl.getPort();
            Instance instance = new Instance();
            instance.setIp(ip);
            instance.setPort(port);
            instance.setMetadata(new HashMap<>(newUrl.getParameters()));

            List<Instance> instances = new ArrayList<>();
            instances.add(instance);
            when(namingService.getAllInstances(serviceName,
                    this.registryUrl.getParameter(GROUP_KEY, Constants.DEFAULT_GROUP))).thenReturn(instances);

        } catch (NacosException e) {
            // ignore
        }

        NacosNamingServiceWrapper nacosNamingServiceWrapper = new
                NacosNamingServiceWrapper(namingService);
        nacosRegistry = new NacosRegistry(this.registryUrl, nacosNamingServiceWrapper);

        NotifyListener listener = mock(NotifyListener.class);
        nacosRegistry.subscribe(serviceUrl, listener);

        Map<URL, Set<NotifyListener>> subscribed = nacosRegistry.getSubscribed();
        assertThat(subscribed.size(), is(1));
        assertThat(subscribed.get(serviceUrl).size(), is(1));

        nacosRegistry.unsubscribe(serviceUrl, listener);
        subscribed = nacosRegistry.getSubscribed();
        assertThat(subscribed.size(), is(1));
        assertThat(subscribed.get(serviceUrl).size(), is(0));
    }


    @Test
    public void testIsConformRules() {
        NamingService namingService = mock(NacosNamingService.class);
        URL serviceUrlWithoutCategory = URL.valueOf("nacos://127.0.0.1:3333/" + serviceInterface + "?interface=" +
                serviceInterface + "&notify=false&methods=test1,test2&version=1.0.0&group=default");
        try {
            String serviceName = "providers:org.apache.dubbo.registry.nacos.NacosService:1.0.0:default";
            String category = this.serviceUrl.getParameter(CATEGORY_KEY, DEFAULT_CATEGORY);
            URL newUrl = this.serviceUrl.addParameter(CATEGORY_KEY, category);
            newUrl = newUrl.addParameter(PROTOCOL_KEY, this.serviceUrl.getProtocol());
            newUrl = newUrl.addParameter(PATH_KEY, this.serviceUrl.getPath());
            newUrl = newUrl.addParameters(NacosNamingServiceUtils.getNacosPreservedParam(this.serviceUrl));
            String ip = newUrl.getHost();
            int port = newUrl.getPort();
            Instance instance = new Instance();
            instance.setIp(ip);
            instance.setPort(port);
            instance.setMetadata(new HashMap<>(newUrl.getParameters()));

            List<Instance> instances = new ArrayList<>();
            instances.add(instance);
            when(namingService.getAllInstances(serviceName,
                    this.registryUrl.getParameter(GROUP_KEY, Constants.DEFAULT_GROUP))).thenReturn(instances);

            String serviceNameWithoutVersion = "providers:org.apache.dubbo.registry.nacos.NacosService:default";
            String serviceName1 = "providers:org.apache.dubbo.registry.nacos.NacosService:1.0.0:default";
            List<String> serviceNames = new ArrayList<>();
            serviceNames.add(serviceNameWithoutVersion);
            serviceNames.add(serviceName1);
            ListView<String> result = new ListView<>();
            result.setData(serviceNames);
            when(namingService.getServicesOfServer(1, Integer.MAX_VALUE,
                    registryUrl.getParameter(GROUP_KEY, Constants.DEFAULT_GROUP))).thenReturn(result);
        } catch (NacosException e) {
            // ignore
        }

        NacosNamingServiceWrapper nacosNamingServiceWrapper = new
                NacosNamingServiceWrapper(namingService);
        nacosRegistry = new NacosRegistry(this.registryUrl, nacosNamingServiceWrapper);

        Set<URL> registered;
        nacosRegistry.register(this.serviceUrl);
        nacosRegistry.register(serviceUrlWithoutCategory);
        registered = nacosRegistry.getRegistered();
        assertThat(registered.contains(serviceUrl), is(true));
        assertThat(registered.contains(serviceUrlWithoutCategory), is(true));
        assertThat(registered.size(), is(2));


        URL serviceUrlWithWildcard = URL.valueOf("nacos://127.0.0.1:3333/" +
                serviceInterface +
                "?interface=org.apache.dubbo.registry.nacos.NacosService" +
                "&notify=false&methods=test1,test2&category=providers&version=*&group=default");

        URL serviceUrlWithOutWildcard = URL.valueOf("nacos://127.0.0.1:3333/" +
                serviceInterface +
                "?interface=org.apache.dubbo.registry.nacos.NacosService" +
                "&notify=false&methods=test1,test2&category=providers&version=1.0.0&group=default");

        NotifyListener listener = mock(NotifyListener.class);
        nacosRegistry.subscribe(serviceUrlWithWildcard, listener);
        nacosRegistry.subscribe(serviceUrlWithOutWildcard, listener);

        Map<URL, Set<NotifyListener>> subscribed = nacosRegistry.getSubscribed();

        assertThat(subscribed.size(), is(2));
        assertThat(subscribed.get(serviceUrlWithOutWildcard).size(), is(1));

        assertThat(subscribed.size(), is(2));
        assertThat(subscribed.get(serviceUrlWithWildcard).size(), is(1));


    }
}
