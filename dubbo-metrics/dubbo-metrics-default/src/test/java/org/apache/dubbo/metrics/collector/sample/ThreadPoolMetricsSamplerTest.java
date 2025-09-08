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
package org.apache.dubbo.metrics.collector.sample;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.store.DataStore;
import org.apache.dubbo.common.store.DataStoreUpdateListener;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.model.ThreadPoolMetric;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SHARED_EXECUTOR_SERVICE_COMPONENT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.EXECUTOR_SERVICE_COMPONENT_KEY;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("all")
public class ThreadPoolMetricsSamplerTest {

    ThreadPoolMetricsSampler sampler;

    @BeforeEach
    void setUp() {
        DefaultMetricsCollector collector = new DefaultMetricsCollector(applicationModel);
        sampler = new ThreadPoolMetricsSampler(collector);
    }

    @Test
    void testSample() {

        ExecutorService executorService = java.util.concurrent.Executors.newFixedThreadPool(5);
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executorService;
        sampler.addExecutors("testPool", executorService);

        List<MetricSample> metricSamples = sampler.sample();

        Assertions.assertEquals(6, metricSamples.size());

        boolean coreSizeFound = false;
        boolean maxSizeFound = false;
        boolean activeSizeFound = false;
        boolean threadCountFound = false;
        boolean queueSizeFound = false;
        boolean largestSizeFound = false;

        for (MetricSample sample : metricSamples) {
            ThreadPoolMetric threadPoolMetric = ((ThreadPoolMetric) ((GaugeMetricSample) sample).getValue());
            switch (sample.getName()) {
                case "dubbo.thread.pool.core.size":
                    coreSizeFound = true;
                    Assertions.assertEquals(5, threadPoolMetric.getCorePoolSize());
                    break;
                case "dubbo.thread.pool.largest.size":
                    largestSizeFound = true;
                    Assertions.assertEquals(0, threadPoolMetric.getLargestPoolSize());
                    break;
                case "dubbo.thread.pool.max.size":
                    maxSizeFound = true;
                    Assertions.assertEquals(5, threadPoolMetric.getMaximumPoolSize());
                    break;
                case "dubbo.thread.pool.active.size":
                    activeSizeFound = true;
                    Assertions.assertEquals(0, threadPoolMetric.getActiveCount());
                    break;
                case "dubbo.thread.pool.thread.count":
                    threadCountFound = true;
                    Assertions.assertEquals(0, threadPoolMetric.getPoolSize());
                    break;
                case "dubbo.thread.pool.queue.size":
                    queueSizeFound = true;
                    Assertions.assertEquals(0, threadPoolMetric.getQueueSize());
                    break;
            }
        }

        Assertions.assertTrue(coreSizeFound);
        Assertions.assertTrue(maxSizeFound);
        Assertions.assertTrue(activeSizeFound);
        Assertions.assertTrue(threadCountFound);
        Assertions.assertTrue(queueSizeFound);
        Assertions.assertTrue(largestSizeFound);

        executorService.shutdown();
    }

    private DefaultMetricsCollector collector;

    private ThreadPoolMetricsSampler sampler2;

    @Mock
    private ApplicationModel applicationModel;

    @Mock
    ScopeBeanFactory scopeBeanFactory;

    @Mock
    private DataStore dataStore;

    @Mock
    private FrameworkExecutorRepository frameworkExecutorRepository;

    @Mock
    private ExtensionLoader<DataStore> extensionLoader;

    @BeforeEach
    public void setUp2() {
        MockitoAnnotations.openMocks(this);

        collector = new DefaultMetricsCollector(applicationModel);
        sampler2 = new ThreadPoolMetricsSampler(collector);

        when(scopeBeanFactory.getBean(FrameworkExecutorRepository.class)).thenReturn(new FrameworkExecutorRepository());

        collector.collectApplication();
        when(applicationModel.getBeanFactory()).thenReturn(scopeBeanFactory);
        when(applicationModel.getExtensionLoader(DataStore.class)).thenReturn(extensionLoader);
        when(extensionLoader.getDefaultExtension()).thenReturn(dataStore);
    }

    @Test
    public void testRegistryDefaultSampleThreadPoolExecutor() throws NoSuchFieldException, IllegalAccessException {

        Map<String, Object> serverExecutors = new HashMap<>();
        Map<String, Object> clientExecutors = new HashMap<>();

        ExecutorService serverExecutor = Executors.newFixedThreadPool(5);
        ExecutorService clientExecutor = Executors.newFixedThreadPool(5);

        serverExecutors.put("server1", serverExecutor);
        clientExecutors.put("client1", clientExecutor);

        when(dataStore.get(EXECUTOR_SERVICE_COMPONENT_KEY)).thenReturn(serverExecutors);
        when(dataStore.get(CONSUMER_SHARED_EXECUTOR_SERVICE_COMPONENT_KEY)).thenReturn(clientExecutors);

        when(frameworkExecutorRepository.getSharedExecutor()).thenReturn(Executors.newFixedThreadPool(5));

        sampler2.registryDefaultSampleThreadPoolExecutor();

        Field f = ThreadPoolMetricsSampler.class.getDeclaredField("sampleThreadPoolExecutor");
        f.setAccessible(true);
        Map<String, ThreadPoolExecutor> executors = (Map<String, ThreadPoolExecutor>) f.get(sampler2);

        Assertions.assertEquals(3, executors.size());
        Assertions.assertTrue(executors.containsKey("DubboServerHandler-server1"));
        Assertions.assertTrue(executors.containsKey("DubboClientHandler-client1"));
        Assertions.assertTrue(executors.containsKey("sharedExecutor"));

        serverExecutor.shutdown();
        clientExecutor.shutdown();
    }

    @Test
    void testDataSourceNotify() throws Exception {
        ArgumentCaptor<DataStoreUpdateListener> captor = ArgumentCaptor.forClass(DataStoreUpdateListener.class);
        when(scopeBeanFactory.getBean(FrameworkExecutorRepository.class)).thenReturn(frameworkExecutorRepository);
        when(frameworkExecutorRepository.getSharedExecutor()).thenReturn(null);
        sampler2.registryDefaultSampleThreadPoolExecutor();

        Field f = ThreadPoolMetricsSampler.class.getDeclaredField("sampleThreadPoolExecutor");
        f.setAccessible(true);
        Map<String, ThreadPoolExecutor> executors = (Map<String, ThreadPoolExecutor>) f.get(sampler2);

        Assertions.assertEquals(0, executors.size());

        verify(dataStore).addListener(captor.capture());
        Assertions.assertEquals(sampler2, captor.getValue());

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        sampler2.onUpdate(EXECUTOR_SERVICE_COMPONENT_KEY, "20880", executorService);

        executors = (Map<String, ThreadPoolExecutor>) f.get(sampler2);
        Assertions.assertEquals(1, executors.size());
        Assertions.assertTrue(executors.containsKey("DubboServerHandler-20880"));

        sampler2.onUpdate(CONSUMER_SHARED_EXECUTOR_SERVICE_COMPONENT_KEY, "client", executorService);
        Assertions.assertEquals(2, executors.size());
        Assertions.assertTrue(executors.containsKey("DubboClientHandler-client"));
    }
}
