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
package org.apache.dubbo.registry.client.event.listener;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.RetryServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.EXPORTED_SERVICES_REVISION_PROPERTY_NAME;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceInstancesChangedListenerConcurrencyTest {
    private static final String SERVICE = "org.apache.dubbo.demo.DemoService";
    private static final URL CONSUMER_URL = URL.valueOf("dubbo://127.0.0.1/" + SERVICE);

    @Test
    void testProviderChangesWhileRetryIsFetchingMetadata() throws Exception {
        ServiceDiscovery discovery = discovery();
        MetadataInfo metadata = metadata();
        CountDownLatch retryStarted = new CountDownLatch(1);
        CountDownLatch releaseRetry = new CountDownLatch(1);
        AtomicInteger attempts = new AtomicInteger();
        when(discovery.getRemoteMetadata(eq("revision"), anyList())).thenAnswer(invocation -> {
            int attempt = attempts.incrementAndGet();
            if (attempt == 1) {
                return MetadataInfo.EMPTY;
            }
            if (attempt == 2) {
                retryStarted.countDown();
                assertTrue(releaseRetry.await(10, TimeUnit.SECONDS));
            }
            return metadata;
        });
        when(discovery.getRemoteMetadata(eq("other-revision"), anyList()))
                .thenReturn(metadata("other-app", "other-revision"));
        ServiceInstancesChangedListener listener =
                new ServiceInstancesChangedListener(new HashSet<>(Arrays.asList("app", "other-app")), discovery);
        NotifyListener notifyListener = subscribe(listener);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            listener.onEvent(event("127.0.0.1"));
            Future<?> retry = executor.submit(() -> listener.onEvent(new RetryServiceInstancesChangedEvent("app")));
            assertTrue(retryStarted.await(5, TimeUnit.SECONDS));

            Future<?> change = executor.submit(() -> listener.onEvent(event("127.0.0.2")));
            change.get(2, TimeUnit.SECONDS);
            assertEquals(
                    "127.0.0.2", listener.getAllInstances().get("app").get(0).getHost());
            Future<?> otherAppChange =
                    executor.submit(() -> listener.onEvent(event("other-app", "127.0.0.4", "other-revision")));
            otherAppChange.get(2, TimeUnit.SECONDS);
            Future<?> latestChange = executor.submit(() -> listener.onEvent(event("127.0.0.3")));
            latestChange.get(2, TimeUnit.SECONDS);
            assertEquals(
                    "127.0.0.3", listener.getAllInstances().get("app").get(0).getHost());

            releaseRetry.countDown();
            retry.get(5, TimeUnit.SECONDS);
            ArgumentCaptor<List<URL>> addresses = ArgumentCaptor.forClass(List.class);
            verify(notifyListener, times(1)).notify(addresses.capture());
            assertEquals(2, addresses.getValue().size());
            assertEquals(
                    new HashSet<>(Arrays.asList("127.0.0.3", "127.0.0.4")),
                    new HashSet<>(Arrays.asList(
                            addresses.getValue().get(0).getHost(),
                            addresses.getValue().get(1).getHost())));
            ArgumentCaptor<List<ServiceInstance>> fetchedInstances = ArgumentCaptor.forClass(List.class);
            verify(discovery, times(3)).getRemoteMetadata(eq("revision"), fetchedInstances.capture());
            assertEquals("127.0.0.3", fetchedInstances.getValue().get(0).getHost());
        } finally {
            releaseRetry.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
            listener.destroy();
        }
    }

    @Test
    void testPendingChangeIsProcessedAfterMetadataFailure() throws Exception {
        ServiceDiscovery discovery = discovery();
        CountDownLatch fetchStarted = new CountDownLatch(1);
        CountDownLatch releaseFetch = new CountDownLatch(1);
        IllegalStateException failure = new IllegalStateException("Metadata fetch failed");
        AtomicInteger attempts = new AtomicInteger();
        when(discovery.getRemoteMetadata(eq("revision"), anyList())).thenAnswer(invocation -> {
            if (attempts.incrementAndGet() == 1) {
                fetchStarted.countDown();
                assertTrue(releaseFetch.await(10, TimeUnit.SECONDS));
                throw failure;
            }
            return metadata();
        });
        ServiceInstancesChangedListener listener =
                new ServiceInstancesChangedListener(Collections.singleton("app"), discovery);
        NotifyListener notifyListener = subscribe(listener);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> failed = executor.submit(() -> listener.onEvent(event("127.0.0.1")));
            assertTrue(fetchStarted.await(5, TimeUnit.SECONDS));
            executor.submit(() -> listener.onEvent(event("127.0.0.2"))).get(2, TimeUnit.SECONDS);
            releaseFetch.countDown();
            ExecutionException exception =
                    assertThrows(ExecutionException.class, () -> failed.get(5, TimeUnit.SECONDS));
            assertSame(failure, exception.getCause());
            ArgumentCaptor<List<URL>> addresses = ArgumentCaptor.forClass(List.class);
            verify(notifyListener).notify(addresses.capture());
            assertEquals("127.0.0.2", addresses.getValue().get(0).getHost());

            listener.onEvent(event("127.0.0.3"));
            verify(notifyListener, times(2)).notify(addresses.capture());
            assertEquals("127.0.0.3", addresses.getValue().get(0).getHost());
        } finally {
            releaseFetch.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
            listener.destroy();
        }
    }

    @Test
    void testDestroyWhileFetchingMetadata() throws Exception {
        ServiceDiscovery discovery = discovery();
        CountDownLatch fetchStarted = new CountDownLatch(1);
        CountDownLatch releaseFetch = new CountDownLatch(1);
        when(discovery.getRemoteMetadata(eq("revision"), anyList())).thenAnswer(invocation -> {
            fetchStarted.countDown();
            assertTrue(releaseFetch.await(10, TimeUnit.SECONDS));
            return metadata();
        });
        ServiceInstancesChangedListener listener =
                new ServiceInstancesChangedListener(Collections.singleton("app"), discovery);
        NotifyListener notifyListener = subscribe(listener);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> refresh = executor.submit(() -> listener.onEvent(event("127.0.0.1")));
            assertTrue(fetchStarted.await(5, TimeUnit.SECONDS));
            executor.submit(() -> listener.onEvent(event("app", "127.0.0.2", "queued-revision")))
                    .get(2, TimeUnit.SECONDS);
            executor.submit(listener::destroy).get(2, TimeUnit.SECONDS);
            releaseFetch.countDown();
            refresh.get(5, TimeUnit.SECONDS);
            assertTrue(listener.isDestroyed());
            assertTrue(listener.getAllInstances().isEmpty());
            assertTrue(listener.serviceUrls.isEmpty());
            listener.onEvent(event("app", "127.0.0.3", "queued-revision"));
            verify(discovery).getRemoteMetadata(anyString(), anyList());
            verify(notifyListener, never()).notify(anyList());
        } finally {
            releaseFetch.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
            listener.destroy();
        }
    }

    @Test
    void testReentrantProviderChangeFromNotification() {
        ServiceDiscovery discovery = discovery();
        when(discovery.getRemoteMetadata(eq("revision"), anyList())).thenReturn(metadata());
        ServiceInstancesChangedListener listener =
                new ServiceInstancesChangedListener(Collections.singleton("app"), discovery);
        NotifyListener notifyListener = subscribe(listener);
        AtomicInteger notifications = new AtomicInteger();
        doAnswer(invocation -> {
                    if (notifications.incrementAndGet() == 1) {
                        listener.onEvent(event("127.0.0.2"));
                    }
                    return null;
                })
                .when(notifyListener)
                .notify(anyList());
        try {
            listener.onEvent(event("127.0.0.1"));
            ArgumentCaptor<List<URL>> addresses = ArgumentCaptor.forClass(List.class);
            verify(notifyListener, times(2)).notify(addresses.capture());
            assertEquals("127.0.0.1", addresses.getAllValues().get(0).get(0).getHost());
            assertEquals("127.0.0.2", addresses.getAllValues().get(1).get(0).getHost());
        } finally {
            listener.destroy();
        }
    }

    @Test
    void testObsoleteSnapshotStopsFetchingRemainingRevisions() throws Exception {
        ServiceDiscovery discovery = discovery();
        CountDownLatch fetchStarted = new CountDownLatch(1);
        CountDownLatch releaseFetch = new CountDownLatch(1);
        AtomicInteger attempts = new AtomicInteger();
        when(discovery.getRemoteMetadata(anyString(), anyList())).thenAnswer(invocation -> {
            if (attempts.incrementAndGet() == 1) {
                fetchStarted.countDown();
                assertTrue(releaseFetch.await(10, TimeUnit.SECONDS));
            }
            return metadata("app", invocation.getArgument(0));
        });
        ServiceInstancesChangedListener listener =
                new ServiceInstancesChangedListener(Collections.singleton("app"), discovery);
        NotifyListener notifyListener = subscribe(listener);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            ServiceInstancesChangedEvent old = new ServiceInstancesChangedEvent(
                    "app",
                    Arrays.asList(
                            event("app", "127.0.0.1", "old-revision-1")
                                    .getServiceInstances()
                                    .get(0),
                            event("app", "127.0.0.2", "old-revision-2")
                                    .getServiceInstances()
                                    .get(0)));
            Future<?> refresh = executor.submit(() -> listener.onEvent(old));
            assertTrue(fetchStarted.await(5, TimeUnit.SECONDS));
            executor.submit(() -> listener.onEvent(event("127.0.0.3"))).get(2, TimeUnit.SECONDS);
            releaseFetch.countDown();
            refresh.get(5, TimeUnit.SECONDS);

            ArgumentCaptor<String> revisions = ArgumentCaptor.forClass(String.class);
            verify(discovery, times(2)).getRemoteMetadata(revisions.capture(), anyList());
            assertEquals("revision", revisions.getAllValues().get(1));
            ArgumentCaptor<List<URL>> addresses = ArgumentCaptor.forClass(List.class);
            verify(notifyListener).notify(addresses.capture());
            assertEquals("127.0.0.3", addresses.getValue().get(0).getHost());
        } finally {
            releaseFetch.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
            listener.destroy();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testPendingChangesSurviveRepeatedNotificationFailures(boolean reuseFailure) {
        ServiceDiscovery discovery = discovery();
        when(discovery.getRemoteMetadata(eq("revision"), anyList())).thenReturn(metadata());
        ServiceInstancesChangedListener listener =
                new ServiceInstancesChangedListener(Collections.singleton("app"), discovery);
        NotifyListener notifyListener = subscribe(listener);
        AssertionError firstFailure = new AssertionError("First notification failed");
        Throwable secondFailure = reuseFailure ? firstFailure : new IllegalStateException("Second notification failed");
        AtomicInteger notifications = new AtomicInteger();
        doAnswer(invocation -> {
                    int notification = notifications.incrementAndGet();
                    if (notification == 1) {
                        listener.onEvent(event("127.0.0.2"));
                        throw firstFailure;
                    }
                    if (notification == 2) {
                        listener.onEvent(event("127.0.0.3"));
                        throw secondFailure;
                    }
                    return null;
                })
                .when(notifyListener)
                .notify(anyList());
        try {
            assertSame(firstFailure, assertThrows(AssertionError.class, () -> listener.onEvent(event("127.0.0.1"))));
            assertArrayEquals(
                    reuseFailure ? new Throwable[0] : new Throwable[] {secondFailure}, firstFailure.getSuppressed());
            ArgumentCaptor<List<URL>> addresses = ArgumentCaptor.forClass(List.class);
            verify(notifyListener, times(3)).notify(addresses.capture());
            assertEquals("127.0.0.3", addresses.getValue().get(0).getHost());
            listener.onEvent(event("127.0.0.4"));
            verify(notifyListener, times(4)).notify(addresses.capture());
            assertEquals("127.0.0.4", addresses.getValue().get(0).getHost());
        } finally {
            listener.destroy();
        }
    }

    @Test
    void testUnrelatedAndExpiredEventsAreIgnored() {
        ServiceDiscovery discovery = discovery();
        when(discovery.getRemoteMetadata(eq("revision"), anyList())).thenReturn(metadata());
        ServiceInstancesChangedListener listener =
                new ServiceInstancesChangedListener(Collections.singleton("app"), discovery);
        NotifyListener notifyListener = subscribe(listener);
        RetryServiceInstancesChangedEvent expiredRetry = mock(RetryServiceInstancesChangedEvent.class);
        when(expiredRetry.getServiceName()).thenReturn("app");
        when(expiredRetry.getFailureRecordTime()).thenReturn(0L);
        try {
            listener.onEvent(event("127.0.0.1"));
            listener.onEvent(event("other-app", "127.0.0.2", "other-revision"));
            listener.onEvent(expiredRetry);
            assertEquals(
                    Collections.singleton("app"), listener.getAllInstances().keySet());
            assertEquals(
                    "127.0.0.1", listener.getAllInstances().get("app").get(0).getHost());
            verify(discovery).getRemoteMetadata(anyString(), anyList());
            verify(notifyListener).notify(anyList());
        } finally {
            listener.destroy();
        }
    }

    @Test
    void testDestroyFromNotificationDoesNotScheduleRetry() {
        ApplicationModel applicationModel = FrameworkModel.defaultModel().newApplication();
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        FrameworkExecutorRepository executors = mock(FrameworkExecutorRepository.class);
        when(executors.getMetadataRetryExecutor()).thenReturn(scheduler);
        applicationModel.getBeanFactory().registerBean(executors);
        ServiceDiscovery discovery = discovery();
        when(discovery.getUrl())
                .thenReturn(URL.valueOf("dubbo://127.0.0.1:2181").setScopeModel(applicationModel));
        when(discovery.getRemoteMetadata(eq("revision"), anyList())).thenReturn(metadata());
        when(discovery.getRemoteMetadata(eq("missing-revision"), anyList())).thenReturn(MetadataInfo.EMPTY);
        ServiceInstancesChangedListener listener =
                new ServiceInstancesChangedListener(Collections.singleton("app"), discovery);
        NotifyListener notifyListener = subscribe(listener);
        doAnswer(invocation -> {
                    listener.onEvent(event("127.0.0.3"));
                    listener.destroy();
                    return null;
                })
                .when(notifyListener)
                .notify(anyList());
        try {
            listener.onEvent(new ServiceInstancesChangedEvent(
                    "app",
                    Arrays.asList(
                            event("127.0.0.1").getServiceInstances().get(0),
                            event("app", "127.0.0.2", "missing-revision")
                                    .getServiceInstances()
                                    .get(0))));
            assertTrue(listener.isDestroyed());
            assertTrue(listener.getAllInstances().isEmpty());
            assertTrue(listener.serviceUrls.isEmpty());
            verify(notifyListener).notify(anyList());
            verify(scheduler, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
        } finally {
            listener.destroy();
            applicationModel.destroy();
        }
    }

    private static ServiceDiscovery discovery() {
        ServiceDiscovery discovery = mock(ServiceDiscovery.class);
        when(discovery.getUrl()).thenReturn(URL.valueOf("dubbo://127.0.0.1:2181"));
        return discovery;
    }

    private static NotifyListener subscribe(ServiceInstancesChangedListener listener) {
        NotifyListener notifyListener = mock(NotifyListener.class);
        when(notifyListener.getConsumerUrl()).thenReturn(CONSUMER_URL);
        listener.addListenerAndNotify(CONSUMER_URL, notifyListener);
        return notifyListener;
    }

    private static ServiceInstancesChangedEvent event(String host) {
        return event("app", host, "revision");
    }

    private static ServiceInstancesChangedEvent event(String app, String host, String revision) {
        DefaultServiceInstance instance = new DefaultServiceInstance(app, host, 20880, ApplicationModel.defaultModel());
        instance.getMetadata().put(EXPORTED_SERVICES_REVISION_PROPERTY_NAME, revision);
        return new ServiceInstancesChangedEvent(app, Collections.singletonList(instance));
    }

    private static MetadataInfo metadata() {
        return metadata("app", "revision");
    }

    private static MetadataInfo metadata(String app, String revision) {
        MetadataInfo.ServiceInfo service =
                new MetadataInfo.ServiceInfo(SERVICE, null, null, "dubbo", 0, SERVICE, Collections.emptyMap());
        MetadataInfo metadata = new MetadataInfo(app, revision, Collections.singletonMap(SERVICE + ":dubbo", service));
        metadata.init();
        return metadata;
    }
}
