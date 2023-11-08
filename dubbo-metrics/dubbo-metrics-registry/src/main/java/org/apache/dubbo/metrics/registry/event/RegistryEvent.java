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

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.metrics.event.TimeCounterEvent;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsLevel;
import org.apache.dubbo.metrics.model.key.TypeWrapper;
import org.apache.dubbo.metrics.registry.RegistryMetricsConstants;
import org.apache.dubbo.metrics.registry.collector.RegistryMetricsCollector;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_DIRECTORY_MAP;
import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_LAST_NUM_MAP;
import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_SERVICE;
import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_SIZE;

/**
 * Registry related events
 */
public class RegistryEvent extends TimeCounterEvent {
    public RegistryEvent(ApplicationModel applicationModel, TypeWrapper typeWrapper) {
        super(applicationModel, typeWrapper);
        ScopeBeanFactory beanFactory = getSource().getBeanFactory();
        RegistryMetricsCollector collector;
        if (!beanFactory.isDestroyed()) {
            collector = beanFactory.getBean(RegistryMetricsCollector.class);
            super.setAvailable(collector != null && collector.isCollectEnabled());
        }
    }

    private static final TypeWrapper REGISTER_EVENT = new TypeWrapper(
            MetricsLevel.APP,
            MetricsKey.REGISTER_METRIC_REQUESTS,
            MetricsKey.REGISTER_METRIC_REQUESTS_SUCCEED,
            MetricsKey.REGISTER_METRIC_REQUESTS_FAILED);

    public static RegistryEvent toRegisterEvent(ApplicationModel applicationModel, List<String> registryClusterNames) {
        RegistryEvent registryEvent = new RegistryEvent(applicationModel, REGISTER_EVENT);
        registryEvent.putAttachment(RegistryMetricsConstants.ATTACHMENT_REGISTRY_KEY, registryClusterNames);
        return registryEvent;
    }

    private static final TypeWrapper SUBSCRIBE_EVENT = new TypeWrapper(
            MetricsLevel.APP,
            MetricsKey.SUBSCRIBE_METRIC_NUM,
            MetricsKey.SUBSCRIBE_METRIC_NUM_SUCCEED,
            MetricsKey.SUBSCRIBE_METRIC_NUM_FAILED);

    public static RegistryEvent toSubscribeEvent(ApplicationModel applicationModel, String registryClusterName) {
        RegistryEvent ddEvent = new RegistryEvent(applicationModel, SUBSCRIBE_EVENT);
        ddEvent.putAttachment(
                RegistryMetricsConstants.ATTACHMENT_REGISTRY_KEY, Collections.singletonList(registryClusterName));
        return ddEvent;
    }

    private static final TypeWrapper NOTIFY_EVENT = new TypeWrapper(
            MetricsLevel.APP, MetricsKey.NOTIFY_METRIC_REQUESTS, MetricsKey.NOTIFY_METRIC_NUM_LAST, (MetricsKey) null);

    public static RegistryEvent toNotifyEvent(ApplicationModel applicationModel) {
        return new RegistryEvent(applicationModel, NOTIFY_EVENT) {
            @Override
            public void customAfterPost(Object postResult) {
                super.putAttachment(ATTACHMENT_KEY_LAST_NUM_MAP, postResult);
            }
        };
    }

    private static final TypeWrapper RS_EVENT = new TypeWrapper(
            MetricsLevel.SERVICE,
            MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS,
            MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_SUCCEED,
            MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_FAILED);

    public static RegistryEvent toRsEvent(
            ApplicationModel applicationModel, String serviceKey, int size, List<String> serviceDiscoveryNames) {
        RegistryEvent ddEvent = new RegistryEvent(applicationModel, RS_EVENT);
        ddEvent.putAttachment(ATTACHMENT_KEY_SERVICE, serviceKey);
        ddEvent.putAttachment(ATTACHMENT_KEY_SIZE, size);
        ddEvent.putAttachment(RegistryMetricsConstants.ATTACHMENT_REGISTRY_KEY, serviceDiscoveryNames);
        return ddEvent;
    }

    private static final TypeWrapper SS_EVENT = new TypeWrapper(
            MetricsLevel.SERVICE,
            MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM,
            MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM_SUCCEED,
            MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM_FAILED);

    public static RegistryEvent toSsEvent(
            ApplicationModel applicationModel, String serviceKey, List<String> serviceDiscoveryNames) {
        RegistryEvent ddEvent = new RegistryEvent(applicationModel, SS_EVENT);
        ddEvent.putAttachment(ATTACHMENT_KEY_SERVICE, serviceKey);
        ddEvent.putAttachment(ATTACHMENT_KEY_SIZE, 1);
        ddEvent.putAttachment(RegistryMetricsConstants.ATTACHMENT_REGISTRY_KEY, serviceDiscoveryNames);
        return ddEvent;
    }

    private static final TypeWrapper DIRECTORY_EVENT =
            new TypeWrapper(MetricsLevel.APP, MetricsKey.DIRECTORY_METRIC_NUM_VALID, null, null);

    public static RegistryEvent refreshDirectoryEvent(
            ApplicationModel applicationModel,
            Map<MetricsKey, Map<String, Integer>> summaryMap,
            Map<String, String> attachments) {
        RegistryEvent registryEvent = new RegistryEvent(applicationModel, DIRECTORY_EVENT);
        registryEvent.putAttachment(ATTACHMENT_DIRECTORY_MAP, summaryMap);
        registryEvent.putAttachments(attachments);
        return registryEvent;
    }
}
