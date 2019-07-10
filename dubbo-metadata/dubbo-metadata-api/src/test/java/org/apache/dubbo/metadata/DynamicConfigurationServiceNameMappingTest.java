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
package org.apache.dubbo.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.config.configcenter.DynamicConfigurationFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static java.util.Arrays.asList;
import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;
import static org.apache.dubbo.metadata.DynamicConfigurationServiceNameMapping.buildGroup;
import static org.apache.dubbo.metadata.ServiceNameMapping.getDefaultExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link DynamicConfigurationServiceNameMapping} Test
 *
 * @since 2.7.4
 */
public class DynamicConfigurationServiceNameMappingTest {

    private static CuratorFramework client;

    private static URL configUrl;
    private static int zkServerPort = NetUtils.getAvailablePort();
    private static TestingServer zkServer;

    private final ServiceNameMapping serviceNameMapping = getDefaultExtension();

    @BeforeAll
    public static void setUp() throws Exception {

        zkServer = new TestingServer(zkServerPort, true);

        client = CuratorFrameworkFactory.newClient("localhost:" + zkServerPort, 60 * 1000, 60 * 1000,
                new ExponentialBackoffRetry(1000, 3));

        client.start();

        configUrl = URL.valueOf("zookeeper://localhost:" + zkServerPort);

        DynamicConfiguration configuration = getExtensionLoader(DynamicConfigurationFactory.class)
                .getExtension(configUrl.getProtocol())
                .getDynamicConfiguration(configUrl);

        Environment.getInstance().setDynamicConfiguration(configuration);
    }

    @AfterAll
    public static void tearDown() throws Exception {
        zkServer.stop();
    }

    @Test
    public void testBuildGroup() {
        assertEquals("test:::", buildGroup("test", null, null, null));
        assertEquals("test:default::", buildGroup("test", "default", null, null));
        assertEquals("test:default:1.0.0:", buildGroup("test", "default", "1.0.0", null));
        assertEquals("test:default:1.0.0:dubbo", buildGroup("test", "default", "1.0.0", "dubbo"));
    }

    @Test
    public void testMapAndGet() {

        String serviceName = "test";
        String serviceName2 = "test2";

        ApplicationModel.setApplication(serviceName);

        String serviceInterface = "org.apache.dubbo.service.UserService";
        String group = null;
        String version = null;
        String protocol = null;

        serviceNameMapping.map(serviceInterface, group, version, protocol);

        ApplicationModel.setApplication(serviceName2);

        serviceNameMapping.map(serviceInterface, group, version, protocol);

        Set<String> serviceNames = serviceNameMapping.get(serviceInterface, group, version, protocol);

        assertEquals(new TreeSet(asList(serviceName, serviceName2)), serviceNames);

    }
}
