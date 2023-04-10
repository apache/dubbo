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
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsPlaceType;
import org.apache.dubbo.metrics.registry.collector.RegistryMetricsCollector;

import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_DIR_NUM;
import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_SERVICE;
import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_SIZE;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_NOTIFY;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_REGISTER;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_REGISTER_SERVICE;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_SUBSCRIBE;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_SUBSCRIBE_SERVICE;

public final class RegistryMetricsEventMulticaster extends SimpleMetricsEventMulticaster {

    public RegistryMetricsEventMulticaster(RegistryMetricsCollector collector) {
        // MetricsRegisterListener
        super.addListener(onPostEventBuild(MetricsKey.REGISTER_METRIC_REQUESTS));
        super.addListener(onFinishEventBuild(MetricsKey.REGISTER_METRIC_REQUESTS_SUCCEED, OP_TYPE_REGISTER));
        super.addListener(onErrorEventBuild(MetricsKey.REGISTER_METRIC_REQUESTS_FAILED, OP_TYPE_REGISTER));

        // MetricsSubscribeListener
        super.addListener(onPostEventBuild(MetricsKey.SUBSCRIBE_METRIC_NUM));
        super.addListener(onFinishEventBuild(MetricsKey.SUBSCRIBE_METRIC_NUM_SUCCEED, OP_TYPE_SUBSCRIBE));
        super.addListener(onErrorEventBuild(MetricsKey.SUBSCRIBE_METRIC_NUM_FAILED, OP_TYPE_SUBSCRIBE));

        // MetricsNotifyListener
        super.addListener(onPostEventBuild(MetricsKey.NOTIFY_METRIC_REQUESTS));
        super.addListener(
            RegistryListener.onFinish(MetricsKey.NOTIFY_METRIC_NUM_LAST,
                event -> {
                    event.setLastNum(MetricsKey.NOTIFY_METRIC_NUM_LAST);
                    event.addApplicationRT(OP_TYPE_NOTIFY);
                }
            ));


        // MetricsDirectoryListener
        addIncrListener(MetricsKey.DIRECTORY_METRIC_NUM_VALID);
        addIncrListener(MetricsKey.DIRECTORY_METRIC_NUM_UN_VALID);
        addIncrListener(MetricsKey.DIRECTORY_METRIC_NUM_DISABLE);
        addIncrListener(MetricsKey.DIRECTORY_METRIC_NUM_RECOVER_DISABLE);
        super.addListener(RegistryListener.onEvent(MetricsKey.DIRECTORY_METRIC_NUM_CURRENT,
            event -> event.setNum(MetricsKey.DIRECTORY_METRIC_NUM_CURRENT, ATTACHMENT_KEY_DIR_NUM))
        );

        // MetricsServiceRegisterListener
        super.addListener(RegistryListener.onEvent(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS,
            event -> event.incrementServiceKey(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS, ATTACHMENT_KEY_SERVICE, ATTACHMENT_KEY_SIZE)
        ));
        super.addListener(RegistryListener.onFinish(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_SUCCEED,
            event ->
            {
                event.incrementServiceKey(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_SUCCEED, ATTACHMENT_KEY_SERVICE, ATTACHMENT_KEY_SIZE);
                event.addServiceKeyRT(ATTACHMENT_KEY_SERVICE, OP_TYPE_REGISTER_SERVICE.getType());
            }));
        super.addListener(RegistryListener.onError(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_FAILED,
            event ->
            {
                event.incrementServiceKey(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_FAILED, ATTACHMENT_KEY_SERVICE, ATTACHMENT_KEY_SIZE);
                event.addServiceKeyRT(ATTACHMENT_KEY_SERVICE, OP_TYPE_REGISTER_SERVICE.getType());
            }));

        // MetricsServiceSubscribeListener
        super.addListener(RegistryListener.onEvent(MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM,
            event ->
                event.incrementServiceKey(MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM, ATTACHMENT_KEY_SERVICE, 1)));
        super.addListener(RegistryListener.onFinish(MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM_SUCCEED,
            event ->
            {
                event.incrementServiceKey(MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM_SUCCEED, ATTACHMENT_KEY_SERVICE, 1);
                event.addServiceKeyRT(ATTACHMENT_KEY_SERVICE, OP_TYPE_SUBSCRIBE_SERVICE.getType());
            }));
        super.addListener(RegistryListener.onError(MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM_FAILED,
            event ->
            {
                event.incrementServiceKey(MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM_FAILED, ATTACHMENT_KEY_SERVICE, 1);
                event.addServiceKeyRT(ATTACHMENT_KEY_SERVICE, OP_TYPE_SUBSCRIBE_SERVICE.getType());
            }));
    }


    private void addIncrListener(MetricsKey metricsKey) {
        super.addListener(onPostEventBuild(metricsKey));
    }

    private RegistryListener onPostEventBuild(MetricsKey metricsKey) {
        return RegistryListener.onEvent(metricsKey,
            event -> event.getCollector().increment(event.getSource().getApplicationName(), metricsKey)
        );
    }

    private RegistryListener onFinishEventBuild(MetricsKey metricsKey, MetricsPlaceType placeType) {
        return RegistryListener.onFinish(metricsKey,
            event -> {
                event.increment(metricsKey);
                event.addApplicationRT(placeType);
            }
        );
    }

    private RegistryListener onErrorEventBuild(MetricsKey metricsKey, MetricsPlaceType placeType) {
        return RegistryListener.onError(metricsKey,
            event -> {
                event.increment(metricsKey);
                event.addApplicationRT(placeType);
            }
        );
    }


}
