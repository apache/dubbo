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
package org.apache.dubbo.metrics.registry.event;

import org.apache.dubbo.common.event.DubboEvent;
import org.apache.dubbo.common.event.DubboLifecycleListener;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.metrics.event.TimeCounterEventMulticaster;
import org.apache.dubbo.metrics.listener.MetricsApplicationListener;
import org.apache.dubbo.metrics.model.key.CategoryOverall;
import org.apache.dubbo.metrics.model.key.MetricsCat;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.registry.collector.RegistryMetricsCollector;
import org.apache.dubbo.registry.client.event.RegistryEvent;
import org.apache.dubbo.rpc.cluster.directory.DirectoryRefreshEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_DIRECTORY;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_NOTIFY;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_REGISTER;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_REGISTER_SERVICE;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_SUBSCRIBE;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_SUBSCRIBE_SERVICE;

public final class RegistrySubDispatcher implements DubboLifecycleListener<DubboEvent> {

    ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RegistrySubDispatcher.class);

    private final RegistryMetricsCollector collector;

    private final TimeCounterEventMulticaster multicaster;

    private final Map<Class<?>, Boolean> eventMatchCache = new ConcurrentHashMap<>();

    public RegistrySubDispatcher(RegistryMetricsCollector collector) {
        this.multicaster = new TimeCounterEventMulticaster();
        this.collector = collector;
        CategorySet.ALL.forEach(categorySet -> {
            this.multicaster.addListener(categorySet.getPost().getEventFunc().apply(collector));
            if (categorySet.getFinish() != null) {
                this.multicaster.addListener(
                        categorySet.getFinish().getEventFunc().apply(collector));
            }
            if (categorySet.getError() != null) {
                this.multicaster.addListener(
                        categorySet.getError().getEventFunc().apply(collector));
            }
        });
    }

    @Override
    public boolean support(Class<? extends DubboEvent> eventClass) {
        return eventMatchCache.computeIfAbsent(
                eventClass,
                clazz -> RegistryEvent.class.isAssignableFrom(eventClass)
                        || DirectoryRefreshEvent.class.isAssignableFrom(eventClass));
    }

    @Override
    public void onEventBefore(DubboEvent event) {
        RegistryMetricsEvent metricsEvent = RegistryMetricsEvent.convertEvent(event);
        if (metricsEvent == null) {
            logger.debug("Unsupported event type: {}", event.getClass().getName());
            return;
        }
        this.multicaster.publishEvent(metricsEvent);
    }

    @Override
    public void onEventFinish(DubboEvent event) {
        RegistryMetricsEvent metricsEvent = RegistryMetricsEvent.convertEvent(event);
        if (metricsEvent == null) {
            logger.debug("Unsupported event type: {}", event.getClass().getName());
            return;
        }
        this.multicaster.publishFinishEvent(metricsEvent);
    }

    @Override
    public void onEventError(DubboEvent event) {
        RegistryMetricsEvent metricsEvent = RegistryMetricsEvent.convertEvent(event);
        if (metricsEvent == null) {
            logger.debug("Unsupported event type: {}", event.getClass().getName());
            return;
        }
        this.multicaster.publishErrorEvent(metricsEvent);
    }

    /**
     * A closer aggregation of MetricsCat, a summary collection of certain types of events
     */
    interface CategorySet {
        CategoryOverall APPLICATION_REGISTER = new CategoryOverall(
                OP_TYPE_REGISTER,
                MCat.APPLICATION_REGISTER_POST,
                MCat.APPLICATION_REGISTER_FINISH,
                MCat.APPLICATION_REGISTER_ERROR);
        CategoryOverall APPLICATION_SUBSCRIBE = new CategoryOverall(
                OP_TYPE_SUBSCRIBE,
                MCat.APPLICATION_SUBSCRIBE_POST,
                MCat.APPLICATION_SUBSCRIBE_FINISH,
                MCat.APPLICATION_SUBSCRIBE_ERROR);
        CategoryOverall APPLICATION_NOTIFY =
                new CategoryOverall(OP_TYPE_NOTIFY, MCat.APPLICATION_NOTIFY_POST, MCat.APPLICATION_NOTIFY_FINISH, null);
        CategoryOverall SERVICE_DIRECTORY =
                new CategoryOverall(OP_TYPE_DIRECTORY, MCat.APPLICATION_DIRECTORY_POST, null, null);
        CategoryOverall SERVICE_REGISTER = new CategoryOverall(
                OP_TYPE_REGISTER_SERVICE,
                MCat.SERVICE_REGISTER_POST,
                MCat.SERVICE_REGISTER_FINISH,
                MCat.SERVICE_REGISTER_ERROR);
        CategoryOverall SERVICE_SUBSCRIBE = new CategoryOverall(
                OP_TYPE_SUBSCRIBE_SERVICE,
                MCat.SERVICE_SUBSCRIBE_POST,
                MCat.SERVICE_SUBSCRIBE_FINISH,
                MCat.SERVICE_SUBSCRIBE_ERROR);

        List<CategoryOverall> ALL = Arrays.asList(
                APPLICATION_REGISTER,
                APPLICATION_SUBSCRIBE,
                APPLICATION_NOTIFY,
                SERVICE_DIRECTORY,
                SERVICE_REGISTER,
                SERVICE_SUBSCRIBE);
    }

    /**
     * {@link MetricsCat} MetricsCat collection, for better classification processing
     * Except for a few custom functions, most of them can build standard event listening functions through the static methods of MetricsApplicationListener
     */
    interface MCat {
        // MetricsRegisterListener
        MetricsCat APPLICATION_REGISTER_POST =
                new MetricsCat(MetricsKey.REGISTER_METRIC_REQUESTS, RegistrySpecListener::onPost);
        MetricsCat APPLICATION_REGISTER_FINISH =
                new MetricsCat(MetricsKey.REGISTER_METRIC_REQUESTS_SUCCEED, RegistrySpecListener::onFinish);
        MetricsCat APPLICATION_REGISTER_ERROR =
                new MetricsCat(MetricsKey.REGISTER_METRIC_REQUESTS_FAILED, RegistrySpecListener::onError);

        // MetricsSubscribeListener
        MetricsCat APPLICATION_SUBSCRIBE_POST =
                new MetricsCat(MetricsKey.SUBSCRIBE_METRIC_NUM, RegistrySpecListener::onPost);
        MetricsCat APPLICATION_SUBSCRIBE_FINISH =
                new MetricsCat(MetricsKey.SUBSCRIBE_METRIC_NUM_SUCCEED, RegistrySpecListener::onFinish);
        MetricsCat APPLICATION_SUBSCRIBE_ERROR =
                new MetricsCat(MetricsKey.SUBSCRIBE_METRIC_NUM_FAILED, RegistrySpecListener::onError);

        // MetricsNotifyListener
        MetricsCat APPLICATION_NOTIFY_POST =
                new MetricsCat(MetricsKey.NOTIFY_METRIC_REQUESTS, MetricsApplicationListener::onPostEventBuild);
        MetricsCat APPLICATION_NOTIFY_FINISH =
                new MetricsCat(MetricsKey.NOTIFY_METRIC_NUM_LAST, RegistrySpecListener::onFinishOfNotify);

        MetricsCat APPLICATION_DIRECTORY_POST =
                new MetricsCat(MetricsKey.DIRECTORY_METRIC_NUM_VALID, RegistrySpecListener::onPostOfDirectory);

        // MetricsServiceRegisterListener
        MetricsCat SERVICE_REGISTER_POST =
                new MetricsCat(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS, RegistrySpecListener::onPostOfService);
        MetricsCat SERVICE_REGISTER_FINISH = new MetricsCat(
                MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_SUCCEED, RegistrySpecListener::onFinishOfService);
        MetricsCat SERVICE_REGISTER_ERROR = new MetricsCat(
                MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_FAILED, RegistrySpecListener::onErrorOfService);

        // MetricsServiceSubscribeListener
        MetricsCat SERVICE_SUBSCRIBE_POST =
                new MetricsCat(MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM, RegistrySpecListener::onPostOfService);
        MetricsCat SERVICE_SUBSCRIBE_FINISH = new MetricsCat(
                MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM_SUCCEED, RegistrySpecListener::onFinishOfService);
        MetricsCat SERVICE_SUBSCRIBE_ERROR =
                new MetricsCat(MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM_FAILED, RegistrySpecListener::onErrorOfService);
    }
}
