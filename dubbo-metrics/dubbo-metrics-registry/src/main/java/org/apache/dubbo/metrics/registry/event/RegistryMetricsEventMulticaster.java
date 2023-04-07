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
import org.apache.dubbo.metrics.registry.RegistryConstants;
import org.apache.dubbo.metrics.registry.collector.RegistryMetricsCollector;
import org.apache.dubbo.metrics.registry.event.type.ApplicationType;
import org.apache.dubbo.metrics.registry.event.type.ServiceType;

import static org.apache.dubbo.metrics.registry.RegistryConstants.*;

public final class RegistryMetricsEventMulticaster extends SimpleMetricsEventMulticaster {

    private final RegistryMetricsCollector collector;

    public RegistryMetricsEventMulticaster(RegistryMetricsCollector collector) {
        this.collector = collector;
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
                (event, type) -> {
                    collector.setNum(type, event.getSource().getApplicationName(), event.getAttachmentValue(ATTACHMENT_KEY_LAST_NUM_MAP));
                    collector.updateAppRt(event.getSource().getApplicationName(), OP_TYPE_NOTIFY, event.getTimePair().calc());
                }
            ));


        // MetricsDirectoryListener
        addIncrListener(MetricsKey.DIRECTORY_METRIC_NUM_VALID);
        addIncrListener(MetricsKey.DIRECTORY_METRIC_NUM_UN_VALID);
        addIncrListener(MetricsKey.DIRECTORY_METRIC_NUM_DISABLE);
        addIncrListener(MetricsKey.DIRECTORY_METRIC_NUM_RECOVER_DISABLE);
        super.addListener(RegistryListener.onEvent(MetricsKey.DIRECTORY_METRIC_NUM_CURRENT,
            (event, type) ->
                collector.setNum(type, event.getSource().getApplicationName(), event.getAttachmentValue(ATTACHMENT_KEY_DIR_NUM))
        ));

        // MetricsServiceRegisterListener
        super.addListener(RegistryListener.onEvent(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS,
            (event, metricsKey) -> collector.incrementServiceKey(event.getSource().getApplicationName(), event.getAttachmentValue(ATTACHMENT_KEY_SERVICE), metricsKey, ((RegistryEvent) event).getAttachmentValue(RegistryConstants.ATTACHMENT_KEY_SIZE))
        ));
        super.addListener(RegistryListener.onFinish(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_SUCCEED,
            (event, metricsKey) ->
                this.onRegisterRtEvent((RegistryEvent)event,metricsKey)));

        super.addListener(RegistryListener.onError(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_FAILED,
            (event, metricsKey) ->
                this.onRegisterRtEvent((RegistryEvent)event,metricsKey)));

        // MetricsServiceSubscribeListener
        super.addListener(RegistryListener.onEvent(MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM,
            (event, metricsKey) ->
                event.incrementServiceKey(metricsKey, ATTACHMENT_KEY_SERVICE, 1)));
        super.addListener(RegistryListener.onFinish(MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM_SUCCEED,
            (event, metricsKey) ->
                this.onRtEvent((RegistryEvent)event,metricsKey)));

        super.addListener(RegistryListener.onError(MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM_FAILED,
            (event, metricsKey) ->
                this.onRtEvent((RegistryEvent)event,metricsKey)));
    }


    private void addIncrListener(MetricsKey metricsKey) {
        super.addListener(onPostEventBuild(metricsKey));
    }

    private RegistryListener onPostEventBuild(MetricsKey metricsKey) {
        return RegistryListener.onEvent(metricsKey,
            (event, key) -> collector.incrAppNum(event.getSource().getApplicationName(), key)
        );
    }

    private RegistryListener onFinishEventBuild(MetricsKey metricsKey, String registryOpType) {
        return RegistryListener.onFinish(metricsKey,
            (event, type) -> {
                collector.incrAppNum(event.getSource().getApplicationName(), metricsKey);
                collector.updateAppRt(event.getSource().getApplicationName(), registryOpType, event.getTimePair().calc());
            }
        );
    }

    private RegistryListener onErrorEventBuild(MetricsKey metricsKey, String registryOpType) {
        return RegistryListener.onError(metricsKey,
            (event, type) -> {
                collector.incrAppNum(event.getSource().getApplicationName(), metricsKey);
                collector.updateAppRt(event.getSource().getApplicationName(), registryOpType, event.getTimePair().calc());
            }
        );
    }


    private void incrSk(RegistryEvent event, MetricsKey metricsKey) {
        event.incrementServiceKey(metricsKey, ATTACHMENT_KEY_SERVICE, 1);
    }

    private void onRtEvent(RegistryEvent event, MetricsKey metricsKey) {
        event.incrementServiceKey(metricsKey, ATTACHMENT_KEY_SERVICE, 1);
        event.addServiceKeyRT(ATTACHMENT_KEY_SERVICE, OP_TYPE_SUBSCRIBE_SERVICE);
    }

    private void onRegisterRtEvent(RegistryEvent event, MetricsKey metricsKey) {
        event.incrementServiceKey(metricsKey, ATTACHMENT_KEY_SERVICE, RegistryConstants.ATTACHMENT_KEY_SIZE);
        event.addServiceKeyRT(ATTACHMENT_KEY_SERVICE, OP_TYPE_REGISTER_SERVICE);
    }
}
