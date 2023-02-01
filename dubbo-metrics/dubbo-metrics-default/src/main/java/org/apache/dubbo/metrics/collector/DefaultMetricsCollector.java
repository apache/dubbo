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

package org.apache.dubbo.metrics.collector;

import org.apache.dubbo.metrics.collector.stat.MetricsStatComposite;
import org.apache.dubbo.metrics.collector.stat.MetricsStatHandler;
import org.apache.dubbo.metrics.event.RequestEvent;
import org.apache.dubbo.metrics.listener.MetricsListener;
import org.apache.dubbo.metrics.model.MetricsKey;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;
import org.apache.dubbo.metrics.model.sample.MetricSample;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.apache.dubbo.metrics.model.MetricsCategory.REQUESTS;
import static org.apache.dubbo.metrics.model.MetricsCategory.RT;


/**
 * Default implementation of {@link MetricsCollector}
 */
public class DefaultMetricsCollector implements MetricsCollector {

    private AtomicBoolean collectEnabled = new AtomicBoolean(false);
    private final List<MetricsListener> listeners = new ArrayList<>();
    private final MetricsStatComposite stats;

    public DefaultMetricsCollector() {
        this.stats = new MetricsStatComposite( this);
    }

    public void setCollectEnabled(Boolean collectEnabled) {
        this.collectEnabled.compareAndSet(isCollectEnabled(), collectEnabled);
    }

    public Boolean isCollectEnabled() {
        return collectEnabled.get();
    }

    public void addListener(MetricsListener listener) {
        listeners.add(listener);
    }

    public List<MetricsListener> getListener() {
        return this.listeners;
    }

    public void increaseTotalRequests(String applicationName, String interfaceName, String methodName,
                                      String group, String version) {
        doExecute(RequestEvent.Type.TOTAL,statHandler-> {
            statHandler.increase(applicationName, interfaceName, methodName, group, version);
        });
    }

    public void increaseSucceedRequests(String applicationName, String interfaceName, String methodName,
                                        String group, String version) {
        doExecute(RequestEvent.Type.SUCCEED,statHandler->{
            statHandler.increase(applicationName, interfaceName, methodName, group, version);
        });
    }

    public void increaseUnknownFailedRequests(String applicationName, String interfaceName,
                                              String methodName,
                                              String group,
                                              String version) {
        doExecute(RequestEvent.Type.UNKNOWN_FAILED, statHandler->{
            statHandler.increase(applicationName, interfaceName, methodName, group, version);
        });
    }

    public void businessFailedRequests(String applicationName, String interfaceName, String methodName,
                                       String group, String version) {
        doExecute(RequestEvent.Type.BUSINESS_FAILED,statHandler->{
            statHandler.increase(applicationName, interfaceName, methodName, group, version);
        });
    }

    public void timeoutRequests(String applicationName, String interfaceName, String methodName, String group,
                                String version) {
        doExecute(RequestEvent.Type.REQUEST_TIMEOUT,statHandler->{
            statHandler.increase(applicationName, interfaceName, methodName, group, version);
        });
    }

    public void limitRequests(String applicationName, String interfaceName, String methodName, String group, String version) {
        doExecute(RequestEvent.Type.REQUEST_LIMIT,statHandler->{
            statHandler.increase(applicationName, interfaceName, methodName, group, version);
        });
    }

    public void increaseProcessingRequests(String applicationName, String interfaceName, String methodName,
                                           String group, String version) {
        doExecute(RequestEvent.Type.PROCESSING,statHandler-> {
            statHandler.increase(applicationName, interfaceName, methodName, group, version);
        });
    }

    public void decreaseProcessingRequests(String applicationName, String interfaceName, String methodName,
                                           String group, String version) {
        doExecute(RequestEvent.Type.PROCESSING,statHandler-> {
            statHandler.decrease(applicationName, interfaceName, methodName, group, version);
        });
    }

    public void totalFailedRequests(String applicationName, String interfaceName, String methodName, String group,
                                    String version) {
        doExecute(RequestEvent.Type.TOTAL_FAILED,statHandler-> {
            statHandler.increase(applicationName, interfaceName, methodName, group, version);
        });
    }

    public void addRT(String applicationName, String interfaceName, String methodName, String group, String version, Long responseTime) {
        stats.addRT(applicationName, interfaceName, methodName, group, version, responseTime);
    }

    @Override
    public List<MetricSample> collect() {
        List<MetricSample> list = new ArrayList<>();
        collectRequests(list);
        collectRT(list);

        return list;
    }

    private void collectRequests(List<MetricSample> list) {
        doExecute(RequestEvent.Type.TOTAL, MetricsStatHandler::get).filter(e->!e.isEmpty())
            .ifPresent(map-> map.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_REQUESTS, k.getTags(), REQUESTS, v::get))));

        doExecute(RequestEvent.Type.SUCCEED, MetricsStatHandler::get).filter(e->!e.isEmpty())
            .ifPresent(map-> map.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_REQUESTS_SUCCEED, k.getTags(), REQUESTS, v::get))));

        doExecute(RequestEvent.Type.UNKNOWN_FAILED, MetricsStatHandler::get).filter(e->!e.isEmpty())
            .ifPresent(map-> map.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_REQUESTS_FAILED, k.getTags(), REQUESTS, v::get))));

        doExecute(RequestEvent.Type.PROCESSING, MetricsStatHandler::get).filter(e->!e.isEmpty())
            .ifPresent(map-> map.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_REQUESTS_PROCESSING, k.getTags(), REQUESTS, v::get))));

        doExecute(RequestEvent.Type.BUSINESS_FAILED, MetricsStatHandler::get).filter(e->!e.isEmpty())
            .ifPresent(map-> map.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_REQUEST_BUSINESS_FAILED, k.getTags(), REQUESTS, v::get))));

        doExecute(RequestEvent.Type.REQUEST_TIMEOUT, MetricsStatHandler::get).filter(e->!e.isEmpty())
            .ifPresent(map-> map.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_REQUESTS_TIMEOUT, k.getTags(), REQUESTS, v::get))));

        doExecute(RequestEvent.Type.REQUEST_LIMIT, MetricsStatHandler::get).filter(e->!e.isEmpty())
            .ifPresent(map-> map.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_REQUESTS_LIMIT, k.getTags(), REQUESTS, v::get))));

        doExecute(RequestEvent.Type.TOTAL_FAILED, MetricsStatHandler::get).filter(e->!e.isEmpty())
            .ifPresent(map-> map.forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_REQUESTS_TOTAL_FAILED, k.getTags(), REQUESTS, v::get))));

    }

    private void collectRT(List<MetricSample> list) {
        this.stats.getLastRT().forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_RT_LAST, k.getTags(), RT, v::get)));
        this.stats.getMinRT().forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_RT_MIN, k.getTags(), RT, v::get)));
        this.stats.getMaxRT().forEach((k, v) -> list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_RT_MAX, k.getTags(), RT, v::get)));

        this.stats.getTotalRT().forEach((k, v) -> {
            list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_RT_SUM, k.getTags(), RT, v::get));

            AtomicLong avg = this.stats.getAvgRT().get(k);
            AtomicLong count = this.stats.getRtCount().get(k);
            avg.set(v.get() / count.get());
            list.add(new GaugeMetricSample(MetricsKey.PROVIDER_METRIC_RT_AVG, k.getTags(), RT, avg::get));
        });
    }
    private <T> Optional<T> doExecute(RequestEvent.Type requestType, Function<MetricsStatHandler,T> statExecutor) {
        if (isCollectEnabled()) {
            MetricsStatHandler handler = stats.getHandler(requestType);
            T result =  statExecutor.apply(handler);
            return Optional.ofNullable(result);
        }
        return Optional.empty();
    }

    private void doExecute(RequestEvent.Type requestType, Consumer<MetricsStatHandler> statExecutor) {
        if (isCollectEnabled()) {
            MetricsStatHandler handler = stats.getHandler(requestType);
             statExecutor.accept(handler);
        }
    }
}
