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
package org.apache.dubbo.rpc.cluster.loadbalance.statistics;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.protocol.AbstractProtocol;
import org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import javax.management.monitor.Monitor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.MockitoAnnotations.initMocks;

class CpuUsageServiceFactoryTest {

    @Mock
    private ProxyFactory proxyFactory;
    private CpuUsageServiceFactory cpuUsageServiceFactory;
    final private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

    @BeforeEach
    public void setUp() throws Exception {
        initMocks(this);
        this.cpuUsageServiceFactory = new CpuUsageServiceFactory();
        this.cpuUsageServiceFactory.setProtocol(new DubboProtocol());
        this.cpuUsageServiceFactory.setProxyFactory(proxyFactory);
    }

    @Test
    void createCpuUsageService() {
        URL url = URL.valueOf("http://10.10.10.11");
        CpuUsageService cpuUsage1 = cpuUsageServiceFactory.createCpuUsageService(url);
        CpuUsageService cpuUsage2 = cpuUsageServiceFactory.createCpuUsageService(url);

        Assertions.assertEquals(cpuUsage1, cpuUsage2);
    }
}