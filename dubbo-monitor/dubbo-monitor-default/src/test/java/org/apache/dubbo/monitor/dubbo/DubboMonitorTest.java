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
package org.apache.dubbo.monitor.dubbo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.monitor.Monitor;
import org.apache.dubbo.monitor.MonitorFactory;
import org.apache.dubbo.monitor.MonitorService;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import org.hamcrest.CustomMatcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PROTOCOL;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * DubboMonitorTest
 */
public class DubboMonitorTest {

    private final Invoker<MonitorService> monitorInvoker = new Invoker<MonitorService>() {
        @Override
        public Class<MonitorService> getInterface() {
            return MonitorService.class;
        }

        public URL getUrl() {
            return URL.valueOf("dubbo://127.0.0.1:7070?interval=1000");
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public Result invoke(Invocation invocation) throws RpcException {
            return null;
        }

        @Override
        public void destroy() {
        }
    };
    private volatile URL lastStatistics;
    private final MonitorService monitorService = new MonitorService() {

        public void collect(URL statistics) {
            DubboMonitorTest.this.lastStatistics = statistics;
        }

        public List<URL> lookup(URL query) {
            return Arrays.asList(DubboMonitorTest.this.lastStatistics);
        }

    };

    @Test
    public void testCount() throws Exception {
        DubboMonitor monitor = new DubboMonitor(monitorInvoker, monitorService);
        URL statistics = new URLBuilder(DUBBO_PROTOCOL, "10.20.153.10", 0)
                .addParameter(MonitorService.APPLICATION, "morgan")
                .addParameter(MonitorService.INTERFACE, "MemberService")
                .addParameter(MonitorService.METHOD, "findPerson")
                .addParameter(MonitorService.CONSUMER, "10.20.153.11")
                .addParameter(MonitorService.SUCCESS, 1)
                .addParameter(MonitorService.FAILURE, 0)
                .addParameter(MonitorService.ELAPSED, 3)
                .addParameter(MonitorService.MAX_ELAPSED, 3)
                .addParameter(MonitorService.CONCURRENT, 1)
                .addParameter(MonitorService.MAX_CONCURRENT, 1)
                .build();
        monitor.collect(statistics);
        monitor.send();
        while (lastStatistics == null) {
            Thread.sleep(10);
        }
        Assertions.assertEquals("morgan", lastStatistics.getParameter(MonitorService.APPLICATION));
        Assertions.assertEquals("dubbo", lastStatistics.getProtocol());
        Assertions.assertEquals("10.20.153.10", lastStatistics.getHost());
        Assertions.assertEquals("morgan", lastStatistics.getParameter(MonitorService.APPLICATION));
        Assertions.assertEquals("MemberService", lastStatistics.getParameter(MonitorService.INTERFACE));
        Assertions.assertEquals("findPerson", lastStatistics.getParameter(MonitorService.METHOD));
        Assertions.assertEquals("10.20.153.11", lastStatistics.getParameter(MonitorService.CONSUMER));
        Assertions.assertEquals("1", lastStatistics.getParameter(MonitorService.SUCCESS));
        Assertions.assertEquals("0", lastStatistics.getParameter(MonitorService.FAILURE));
        Assertions.assertEquals("3", lastStatistics.getParameter(MonitorService.ELAPSED));
        Assertions.assertEquals("3", lastStatistics.getParameter(MonitorService.MAX_ELAPSED));
        Assertions.assertEquals("1", lastStatistics.getParameter(MonitorService.CONCURRENT));
        Assertions.assertEquals("1", lastStatistics.getParameter(MonitorService.MAX_CONCURRENT));
        monitor.destroy();
    }

    @Test
    public void testMonitorFactory() throws Exception {
        MockMonitorService monitorService = new MockMonitorService();
        URL statistics = new URLBuilder(DUBBO_PROTOCOL, "10.20.153.10", 0)
                .addParameter(MonitorService.APPLICATION, "morgan")
                .addParameter(MonitorService.INTERFACE, "MemberService")
                .addParameter(MonitorService.METHOD, "findPerson")
                .addParameter(MonitorService.CONSUMER, "10.20.153.11")
                .addParameter(MonitorService.SUCCESS, 1)
                .addParameter(MonitorService.FAILURE, 0)
                .addParameter(MonitorService.ELAPSED, 3)
                .addParameter(MonitorService.MAX_ELAPSED, 3)
                .addParameter(MonitorService.CONCURRENT, 1)
                .addParameter(MonitorService.MAX_CONCURRENT, 1)
                .build();

        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        MonitorFactory monitorFactory = ExtensionLoader.getExtensionLoader(MonitorFactory.class).getAdaptiveExtension();

        Exporter<MonitorService> exporter = protocol.export(proxyFactory.getInvoker(monitorService, MonitorService.class, URL.valueOf("dubbo://127.0.0.1:17979/" + MonitorService.class.getName())));
        try {
            Monitor monitor = null;
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 60000) {
                monitor = monitorFactory.getMonitor(URL.valueOf("dubbo://127.0.0.1:17979?interval=10"));
                if (monitor == null) {
                    continue;
                }
                try {
                    monitor.collect(statistics);
                    int i = 0;
                    while (monitorService.getStatistics() == null && i < 200) {
                        i++;
                        Thread.sleep(10);
                    }
                    URL result = monitorService.getStatistics();
                    Assertions.assertEquals(1, result.getParameter(MonitorService.SUCCESS, 0));
                    Assertions.assertEquals(3, result.getParameter(MonitorService.ELAPSED, 0));
                } finally {
                    monitor.destroy();
                }
                break;
            }
            Assertions.assertNotNull(monitor);
        } finally {
            exporter.unexport();
        }
    }

    @Test
    public void testAvailable() {
        Invoker invoker = mock(Invoker.class);
        MonitorService monitorService = mock(MonitorService.class);

        given(invoker.isAvailable()).willReturn(true);
        given(invoker.getUrl()).willReturn(URL.valueOf("dubbo://127.0.0.1:7070?interval=20"));
        DubboMonitor dubboMonitor = new DubboMonitor(invoker, monitorService);

        assertThat(dubboMonitor.isAvailable(), is(true));
        verify(invoker).isAvailable();
    }

    @Test
    public void testSum() {
        URL statistics = new URLBuilder(DUBBO_PROTOCOL, "10.20.153.11", 0)
                .addParameter(MonitorService.APPLICATION, "morgan")
                .addParameter(MonitorService.INTERFACE, "MemberService")
                .addParameter(MonitorService.METHOD, "findPerson")
                .addParameter(MonitorService.CONSUMER, "10.20.153.11")
                .addParameter(MonitorService.SUCCESS, 1)
                .addParameter(MonitorService.FAILURE, 0)
                .addParameter(MonitorService.ELAPSED, 3)
                .addParameter(MonitorService.MAX_ELAPSED, 3)
                .addParameter(MonitorService.CONCURRENT, 1)
                .addParameter(MonitorService.MAX_CONCURRENT, 1)
                .build();
        Invoker invoker = mock(Invoker.class);
        MonitorService monitorService = mock(MonitorService.class);

        given(invoker.getUrl()).willReturn(URL.valueOf("dubbo://127.0.0.1:7070?interval=20"));
        DubboMonitor dubboMonitor = new DubboMonitor(invoker, monitorService);

        dubboMonitor.collect(statistics);
        dubboMonitor.collect(statistics.addParameter(MonitorService.SUCCESS, 3).addParameter(MonitorService.CONCURRENT, 2)
                .addParameter(MonitorService.INPUT, 1).addParameter(MonitorService.OUTPUT, 2));
        dubboMonitor.collect(statistics.addParameter(MonitorService.SUCCESS, 6).addParameter(MonitorService.ELAPSED, 2));

        dubboMonitor.send();

        ArgumentCaptor<URL> summaryCaptor = ArgumentCaptor.forClass(URL.class);
        verify(monitorService, atLeastOnce()).collect(summaryCaptor.capture());

        List<URL> allValues = summaryCaptor.getAllValues();

        assertThat(allValues, not(nullValue()));
        assertThat(allValues, hasItem(new CustomMatcher<URL>("Monitor count should greater than 1") {
            @Override
            public boolean matches(Object item) {
                URL url = (URL) item;
                return Integer.valueOf(url.getParameter(MonitorService.SUCCESS)) > 1;
            }
        }));
    }

    @Test
    public void testLookUp() {
        Invoker invoker = mock(Invoker.class);
        MonitorService monitorService = mock(MonitorService.class);

        URL queryUrl = URL.valueOf("dubbo://127.0.0.1:7070?interval=20");
        given(invoker.getUrl()).willReturn(queryUrl);
        DubboMonitor dubboMonitor = new DubboMonitor(invoker, monitorService);

        dubboMonitor.lookup(queryUrl);

        verify(monitorService).lookup(eq(queryUrl));
    }
}
