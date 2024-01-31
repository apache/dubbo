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
package org.apache.dubbo.metrics.event;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.metrics.MetricsConstants;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.collector.MethodMetricsCollector;
import org.apache.dubbo.metrics.listener.AbstractMetricsKeyListener;
import org.apache.dubbo.metrics.listener.MetricsListener;
import org.apache.dubbo.metrics.model.MetricsSupport;
import org.apache.dubbo.metrics.model.key.CategoryOverall;
import org.apache.dubbo.metrics.model.key.MetricsCat;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsLevel;
import org.apache.dubbo.metrics.model.key.MetricsPlaceValue;

import static org.apache.dubbo.metrics.DefaultConstants.METRIC_THROWABLE;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_SERVICE_UNAVAILABLE_FAILED;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class DefaultSubDispatcher extends SimpleMetricsEventMulticaster {

    public DefaultSubDispatcher(DefaultMetricsCollector collector) {

        CategoryOverall categoryOverall = initMethodRequest();
        super.addListener(categoryOverall.getPost().getEventFunc().apply(collector));
        super.addListener(categoryOverall.getFinish().getEventFunc().apply(collector));
        super.addListener(categoryOverall.getError().getEventFunc().apply(collector));

        super.addListener(new MetricsListener<RequestEvent>() {

            @Override
            public boolean isSupport(MetricsEvent event) {
                return event instanceof RequestEvent && ((RequestEvent) event).isRequestErrorEvent();
            }

            private final MetricsPlaceValue dynamicPlaceType =
                    MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.METHOD);

            @Override
            public void onEvent(RequestEvent event) {
                MetricsSupport.increment(
                        METRIC_REQUESTS_SERVICE_UNAVAILABLE_FAILED,
                        dynamicPlaceType,
                        (MethodMetricsCollector) collector,
                        event);
            }
        });
    }

    private CategoryOverall initMethodRequest() {

        return new CategoryOverall(
                null,
                new MetricsCat(
                        MetricsKey.METRIC_REQUESTS,
                        (key, placeType, collector) -> AbstractMetricsKeyListener.onEvent(key, event -> {
                            MetricsPlaceValue dynamicPlaceType = MetricsPlaceValue.of(
                                    event.getAttachmentValue(MetricsConstants.INVOCATION_SIDE), MetricsLevel.METHOD);
                            MetricsSupport.increment(key, dynamicPlaceType, (MethodMetricsCollector) collector, event);
                            MetricsSupport.increment(
                                    MetricsKey.METRIC_REQUESTS_PROCESSING,
                                    dynamicPlaceType,
                                    (MethodMetricsCollector) collector,
                                    event);
                        })),
                new MetricsCat(
                        MetricsKey.METRIC_REQUESTS_SUCCEED,
                        (key, placeType, collector) -> AbstractMetricsKeyListener.onFinish(key, event -> {
                            MetricsPlaceValue dynamicPlaceType = MetricsPlaceValue.of(
                                    event.getAttachmentValue(MetricsConstants.INVOCATION_SIDE), MetricsLevel.METHOD);
                            MetricsSupport.dec(
                                    MetricsKey.METRIC_REQUESTS_PROCESSING, dynamicPlaceType, collector, event);

                            Object throwableObj = event.getAttachmentValue(METRIC_THROWABLE);
                            MetricsKey targetKey;
                            if (throwableObj == null) {
                                targetKey = key;
                            } else {
                                targetKey = MetricsSupport.getMetricsKey((Throwable) throwableObj);
                                MetricsSupport.increment(
                                        MetricsKey.METRIC_REQUESTS_TOTAL_FAILED,
                                        dynamicPlaceType,
                                        (MethodMetricsCollector) collector,
                                        event);
                            }
                            MetricsSupport.incrAndAddRt(
                                    targetKey, dynamicPlaceType, (MethodMetricsCollector) collector, event);
                        })),
                new MetricsCat(
                        MetricsKey.METRIC_REQUEST_BUSINESS_FAILED,
                        (key, placeType, collector) -> AbstractMetricsKeyListener.onError(key, event -> {
                            MetricsKey targetKey =
                                    MetricsSupport.getMetricsKey(event.getAttachmentValue(METRIC_THROWABLE));
                            // Dynamic metricsKey && dynamicPlaceType
                            MetricsPlaceValue dynamicPlaceType = MetricsPlaceValue.of(
                                    event.getAttachmentValue(MetricsConstants.INVOCATION_SIDE), MetricsLevel.METHOD);
                            MetricsSupport.increment(
                                    MetricsKey.METRIC_REQUESTS_TOTAL_FAILED,
                                    dynamicPlaceType,
                                    (MethodMetricsCollector) collector,
                                    event);
                            MetricsSupport.dec(
                                    MetricsKey.METRIC_REQUESTS_PROCESSING, dynamicPlaceType, collector, event);
                            MetricsSupport.incrAndAddRt(
                                    targetKey, dynamicPlaceType, (MethodMetricsCollector) collector, event);
                        })));
    }
}
