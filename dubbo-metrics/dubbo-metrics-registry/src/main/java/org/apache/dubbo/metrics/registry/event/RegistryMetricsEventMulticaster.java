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
import org.apache.dubbo.metrics.registry.event.type.ApplicationType;
import org.apache.dubbo.metrics.registry.event.type.ServiceType;

import java.util.Map;

import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_DIRECTORY_MAP;
import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_SERVICE;
import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_SIZE;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_NOTIFY;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_REGISTER;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_REGISTER_SERVICE;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_SUBSCRIBE;
import static org.apache.dubbo.metrics.registry.RegistryMetricsConstants.OP_TYPE_SUBSCRIBE_SERVICE;

public final class RegistryMetricsEventMulticaster extends SimpleMetricsEventMulticaster {

    public RegistryMetricsEventMulticaster() {
        // MetricsRegisterListener
        super.addListener(onPostEventBuild(ApplicationType.R_TOTAL));
        super.addListener(onFinishEventBuild(ApplicationType.R_SUCCEED, OP_TYPE_REGISTER.getType()));
        super.addListener(onErrorEventBuild(ApplicationType.R_FAILED, OP_TYPE_REGISTER.getType()));

        // MetricsSubscribeListener
        super.addListener(onPostEventBuild(ApplicationType.S_TOTAL));
        super.addListener(onFinishEventBuild(ApplicationType.S_SUCCEED, OP_TYPE_SUBSCRIBE.getType()));
        super.addListener(onErrorEventBuild(ApplicationType.S_FAILED, OP_TYPE_SUBSCRIBE.getType()));

        // MetricsNotifyListener
        super.addListener(onPostEventBuild(ApplicationType.N_TOTAL));
        super.addListener(
            RegistryListener.onFinish(ServiceType.N_LAST_NUM,
                (event, type) -> {
                    event.setLastNum(type);
                    event.addApplicationRT(OP_TYPE_NOTIFY.getType());
                }
            ));


        // MetricsDirectoryListener
        super.addListener(RegistryListener.onEvent(ServiceType.D_VALID,
            (event, type) ->
            {
                Map<ServiceType, Map<String, Integer>> summaryMap = event.getAttachmentValue(ATTACHMENT_DIRECTORY_MAP);
                summaryMap.forEach((serviceType, map) ->
                    event.getCollector().setNum(serviceType, event.getSource().getApplicationName(), map));
            }
        ));

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

    private RegistryListener onPostEventBuild(ApplicationType applicationType) {
        return RegistryListener.onEvent(applicationType,
            (event, type) -> event.getCollector().increment(event.getSource().getApplicationName(), type)
        );
    }

    private RegistryListener onFinishEventBuild(ApplicationType applicationType, String registryOpType) {
        return RegistryListener.onFinish(applicationType,
            (event, type) -> {
                event.increment(type);
                event.addApplicationRT(registryOpType);
            }
        );
    }

    private RegistryListener onErrorEventBuild(ApplicationType applicationType, String registryOpType) {
        return RegistryListener.onError(applicationType,
            (event, type) -> {
                event.increment(type);
                event.addApplicationRT(registryOpType);
            }
        );
    }


    private void incrSk(RegistryEvent event, ServiceType type) {
        event.incrementServiceKey(type, ATTACHMENT_KEY_SERVICE, 1);
    }

    private void incrSkSize(RegistryEvent event, ServiceType type) {
        event.incrementServiceKey(type, ATTACHMENT_KEY_SERVICE, ATTACHMENT_KEY_SIZE);
    }

    private void onRtEvent(RegistryEvent event, ServiceType type) {
        incrSk(event, type);
        event.addServiceKeyRT(ATTACHMENT_KEY_SERVICE, OP_TYPE_SUBSCRIBE_SERVICE.getType());
    }

    private void onRegisterRtEvent(RegistryEvent event, ServiceType type) {
        incrSkSize(event, type);
        event.addServiceKeyRT(ATTACHMENT_KEY_SERVICE, OP_TYPE_REGISTER_SERVICE.getType());
    }
}
