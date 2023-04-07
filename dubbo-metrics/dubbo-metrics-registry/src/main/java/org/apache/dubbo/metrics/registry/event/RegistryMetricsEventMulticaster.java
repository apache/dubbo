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
                    collector.setNum(type, event.getSource().getApplicationName(), ((RegistryEvent) event).getAttachmentValue(ATTACHMENT_KEY_LAST_NUM_MAP));
                    collector.updateAppRt(event.getSource().getApplicationName(), OP_TYPE_NOTIFY, event.getTimePair().calc());
                }
            ));


        // MetricsDirectoryListener
        addIncrListener(ApplicationType.D_VALID);
        addIncrListener(ApplicationType.D_UN_VALID);
        addIncrListener(ApplicationType.D_DISABLE);
        addIncrListener(ApplicationType.D_RECOVER_DISABLE);
        super.addListener(RegistryListener.onEvent(MetricsKey.DIRECTORY_METRIC_NUM_CURRENT,
            (event, type) -> event.setNum(type, ATTACHMENT_KEY_DIR_NUM))
        );

        // MetricsServiceRegisterListener
        super.addListener(RegistryListener.onEvent(ServiceType.R_SERVICE_TOTAL,
            this::incrSkSize
        ));
        super.addListener(RegistryListener.onFinish(ServiceType.R_SERVICE_SUCCEED, this::onRegisterRtEvent));
        super.addListener(RegistryListener.onError(ServiceType.R_SERVICE_FAILED, this::onRegisterRtEvent));

        // MetricsServiceSubscribeListener
        super.addListener(RegistryListener.onEvent(ServiceType.S_SERVICE_TOTAL, this::incrSk));
        super.addListener(RegistryListener.onFinish(ServiceType.S_SERVICE_SUCCEED, this::onRtEvent));
        super.addListener(RegistryListener.onError(ServiceType.S_SERVICE_FAILED, this::onRtEvent));
    }


    private void addIncrListener(ApplicationType applicationType) {
        super.addListener(onPostEventBuild(applicationType));
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


    private void incrSk(RegistryEvent event, ServiceType type) {
        event.incrementServiceKey(type, ATTACHMENT_KEY_SERVICE, 1);
    }

    private void incrSkSize(RegistryEvent event, ServiceType type) {
        event.incrementServiceKey(type, ATTACHMENT_KEY_SERVICE, org.apache.dubbo.metrics.registry.RegistryConstants.ATTACHMENT_KEY_SIZE);
    }

    private void onRtEvent(RegistryEvent event, ServiceType type) {
        incrSk(event, type);
        event.addServiceKeyRT(ATTACHMENT_KEY_SERVICE, OP_TYPE_SUBSCRIBE_SERVICE);
    }

    private void onRegisterRtEvent(RegistryEvent event, ServiceType type) {
        incrSkSize(event, type);
        event.addServiceKeyRT(ATTACHMENT_KEY_SERVICE, OP_TYPE_REGISTER_SERVICE);
    }
}
