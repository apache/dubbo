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
import org.apache.dubbo.metrics.DefaultConstants;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.listener.AbstractMetricsKeyListener;
import org.apache.dubbo.metrics.listener.MetricsListener;
import org.apache.dubbo.metrics.model.MetricsSupport;
import org.apache.dubbo.metrics.model.key.CategoryOverall;
import org.apache.dubbo.metrics.model.key.MetricsCat;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsLevel;
import org.apache.dubbo.metrics.model.key.MetricsPlaceValue;
import org.apache.dubbo.rpc.RpcException;

import static org.apache.dubbo.metrics.DefaultConstants.METRIC_THROWABLE;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_SERVICE_UNAVAILABLE_FAILED;

public final class DefaultSubDispatcher extends SimpleMetricsEventMulticaster {

    public DefaultSubDispatcher(DefaultMetricsCollector collector) {

        CategoryOverall categoryOverall = initServiceRequest();
        super.addListener(categoryOverall.getPost().getEventFunc().apply(collector));
        super.addListener(categoryOverall.getFinish().getEventFunc().apply(collector));
        super.addListener(categoryOverall.getError().getEventFunc().apply(collector));

        super.addListener(new MetricsListener<RequestBeforeEvent>() {

            @Override
            public boolean isSupport(MetricsEvent event) {
                return event instanceof RequestBeforeEvent;
            }

            @Override
            public void onEvent(RequestBeforeEvent event) {
                MetricsPlaceValue dynamicPlaceType = MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.SERVICE);
                MetricsSupport.increment(METRIC_REQUESTS_SERVICE_UNAVAILABLE_FAILED, dynamicPlaceType, collector, event);
            }
        });
    }

    private CategoryOverall initServiceRequest() {

        return new CategoryOverall(null,
            new MetricsCat(MetricsKey.METRIC_REQUESTS, (key, placeType, collector) -> AbstractMetricsKeyListener.onEvent(key,
                event ->
                {
                    MetricsPlaceValue dynamicPlaceType = MetricsPlaceValue.of(event.getAttachmentValue(DefaultConstants.INVOCATION_SIDE), MetricsLevel.SERVICE);
                    MetricsSupport.increment(key, dynamicPlaceType, collector, event);
                    MetricsSupport.increment(MetricsKey.METRIC_REQUESTS_PROCESSING, dynamicPlaceType, collector, event);

                })),
            new MetricsCat(MetricsKey.METRIC_REQUESTS_SUCCEED, (key, placeType, collector) -> AbstractMetricsKeyListener.onFinish(key,
                event ->
                {
                    MetricsPlaceValue dynamicPlaceType = MetricsPlaceValue.of(event.getAttachmentValue(DefaultConstants.INVOCATION_SIDE), MetricsLevel.SERVICE);
                    MetricsSupport.dec(MetricsKey.METRIC_REQUESTS_PROCESSING, dynamicPlaceType, collector, event);
                    Object throwableObj = event.getAttachmentValue(METRIC_THROWABLE);
                    MetricsKey targetKey;
                    if (throwableObj == null) {
                        targetKey = key;
                    } else {
                        targetKey = MetricsSupport.getMetricsKey((RpcException) throwableObj);
                    }
                    MetricsSupport.incrAndAddRt(targetKey, dynamicPlaceType, collector, event);
                })),
            new MetricsCat(MetricsKey.METRIC_REQUEST_BUSINESS_FAILED, (key, placeType, collector) -> AbstractMetricsKeyListener.onError(key,
                event ->
                {
                    Throwable throwable = event.getAttachmentValue(METRIC_THROWABLE);
                    MetricsKey targetKey = MetricsKey.METRIC_REQUESTS_FAILED_AGG;
                    if (throwable instanceof RpcException) {
                        targetKey = MetricsSupport.getMetricsKey((RpcException) throwable);
                    }
                    // Dynamic metricsKey
                    MetricsPlaceValue dynamicPlaceType = MetricsPlaceValue.of(event.getAttachmentValue(DefaultConstants.INVOCATION_SIDE), MetricsLevel.SERVICE);
                    MetricsSupport.dec(MetricsKey.METRIC_REQUESTS_PROCESSING, dynamicPlaceType, collector, event);
                    MetricsSupport.incrAndAddRt(targetKey, dynamicPlaceType, collector, event);
                }
            )));
    }

}
