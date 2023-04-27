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

import org.apache.dubbo.metrics.event.SimpleMetricsEventMulticaster;
import org.apache.dubbo.metrics.listener.AbstractMetricsListener;
import org.apache.dubbo.metrics.listener.MetricsApplicationListener;
import org.apache.dubbo.metrics.listener.MetricsServiceListener;
import org.apache.dubbo.metrics.model.key.CategoryOverall;
import org.apache.dubbo.metrics.model.key.MetricsCat;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.registry.collector.RegistryMetricsCollector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_DIRECTORY_MAP;
import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_LAST_NUM_MAP;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_DIRECTORY;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_NOTIFY;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_REGISTER;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_REGISTER_SERVICE;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_SUBSCRIBE;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_SUBSCRIBE_SERVICE;

public final class RegistrySubDispatcher extends SimpleMetricsEventMulticaster {


    public RegistrySubDispatcher(RegistryMetricsCollector collector) {

        CategorySet.ALL.forEach(categorySet ->
        {
            super.addListener(categorySet.getPost().getEventFunc().apply(collector));
            if (categorySet.getFinish() != null) {
                super.addListener(categorySet.getFinish().getEventFunc().apply(collector));
            }
            if (categorySet.getError() != null) {
                super.addListener(categorySet.getError().getEventFunc().apply(collector));
            }
        });
    }

    interface CategorySet {
        CategoryOverall APPLICATION_REGISTER = new CategoryOverall(OP_TYPE_REGISTER, MCat.APPLICATION_REGISTER_POST, MCat.APPLICATION_REGISTER_FINISH, MCat.APPLICATION_REGISTER_ERROR);
        CategoryOverall APPLICATION_SUBSCRIBE = new CategoryOverall(OP_TYPE_SUBSCRIBE, MCat.APPLICATION_SUBSCRIBE_POST, MCat.APPLICATION_SUBSCRIBE_FINISH, MCat.APPLICATION_SUBSCRIBE_ERROR);
        CategoryOverall APPLICATION_NOTIFY = new CategoryOverall(OP_TYPE_NOTIFY, MCat.APPLICATION_NOTIFY_POST, MCat.APPLICATION_NOTIFY_FINISH, null);
        CategoryOverall SERVICE_DIRECTORY = new CategoryOverall(OP_TYPE_DIRECTORY, MCat.APPLICATION_DIRECTORY_POST, null, null);
        CategoryOverall SERVICE_REGISTER = new CategoryOverall(OP_TYPE_REGISTER_SERVICE, MCat.SERVICE_REGISTER_POST, MCat.SERVICE_REGISTER_FINISH, MCat.SERVICE_REGISTER_ERROR);
        CategoryOverall SERVICE_SUBSCRIBE = new CategoryOverall(OP_TYPE_SUBSCRIBE_SERVICE, MCat.SERVICE_SUBSCRIBE_POST, MCat.SERVICE_SUBSCRIBE_FINISH, MCat.SERVICE_SUBSCRIBE_ERROR);

        List<CategoryOverall> ALL = Arrays.asList(APPLICATION_REGISTER, APPLICATION_SUBSCRIBE, APPLICATION_NOTIFY, SERVICE_DIRECTORY, SERVICE_REGISTER, SERVICE_SUBSCRIBE);
    }


    interface MCat {
        // MetricsRegisterListener
        MetricsCat APPLICATION_REGISTER_POST = new MetricsCat(MetricsKey.REGISTER_METRIC_REQUESTS, MetricsApplicationListener::onPostEventBuild);
        MetricsCat APPLICATION_REGISTER_FINISH = new MetricsCat(MetricsKey.REGISTER_METRIC_REQUESTS_SUCCEED, MetricsApplicationListener::onFinishEventBuild);
        MetricsCat APPLICATION_REGISTER_ERROR = new MetricsCat(MetricsKey.REGISTER_METRIC_REQUESTS_FAILED, MetricsApplicationListener::onErrorEventBuild);

        // MetricsSubscribeListener
        MetricsCat APPLICATION_SUBSCRIBE_POST = new MetricsCat(MetricsKey.SUBSCRIBE_METRIC_NUM, MetricsApplicationListener::onPostEventBuild);
        MetricsCat APPLICATION_SUBSCRIBE_FINISH = new MetricsCat(MetricsKey.SUBSCRIBE_METRIC_NUM_SUCCEED, MetricsApplicationListener::onFinishEventBuild);
        MetricsCat APPLICATION_SUBSCRIBE_ERROR = new MetricsCat(MetricsKey.SUBSCRIBE_METRIC_NUM_FAILED, MetricsApplicationListener::onErrorEventBuild);

        // MetricsNotifyListener
        MetricsCat APPLICATION_NOTIFY_POST = new MetricsCat(MetricsKey.NOTIFY_METRIC_REQUESTS, MetricsApplicationListener::onPostEventBuild);
        MetricsCat APPLICATION_NOTIFY_FINISH = new MetricsCat(MetricsKey.NOTIFY_METRIC_NUM_LAST,
            (key, placeType, collector) -> AbstractMetricsListener.onFinish(key,
                event -> {
                    collector.addRt(event.appName(), placeType.getType(), event.getTimePair().calc());
                    Map<String, Integer> lastNumMap = Collections.unmodifiableMap(event.getAttachmentValue(ATTACHMENT_KEY_LAST_NUM_MAP));
                    lastNumMap.forEach(
                        (k, v) -> collector.setNum(key, event.appName(), k, v));

                }
            ));


        MetricsCat APPLICATION_DIRECTORY_POST = new MetricsCat(MetricsKey.DIRECTORY_METRIC_NUM_VALID, (key, placeType, collector) -> AbstractMetricsListener.onEvent(key,
            event ->
            {
                Map<MetricsKey, Map<String, Integer>> summaryMap = event.getAttachmentValue(ATTACHMENT_DIRECTORY_MAP);
                summaryMap.forEach((metricsKey, map) ->
                    map.forEach(
                        (k, v) -> collector.setNum(metricsKey, event.appName(), k, v)));
            }
        ));


        // MetricsServiceRegisterListener
        MetricsCat SERVICE_REGISTER_POST = new MetricsCat(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS, MetricsServiceListener::onPostEventBuild);
        MetricsCat SERVICE_REGISTER_FINISH = new MetricsCat(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_SUCCEED, MetricsServiceListener::onFinishEventBuild);
        MetricsCat SERVICE_REGISTER_ERROR = new MetricsCat(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_FAILED, MetricsServiceListener::onErrorEventBuild);


        // MetricsServiceSubscribeListener
        MetricsCat SERVICE_SUBSCRIBE_POST = new MetricsCat(MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM, MetricsServiceListener::onPostEventBuild);
        MetricsCat SERVICE_SUBSCRIBE_FINISH = new MetricsCat(MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM_SUCCEED, MetricsServiceListener::onFinishEventBuild);
        MetricsCat SERVICE_SUBSCRIBE_ERROR = new MetricsCat(MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM_FAILED, MetricsServiceListener::onErrorEventBuild);


    }


}
