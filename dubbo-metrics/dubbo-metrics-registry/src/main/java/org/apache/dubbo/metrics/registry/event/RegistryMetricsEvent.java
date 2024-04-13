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
import org.apache.dubbo.common.event.DubboEvent;
import org.apache.dubbo.common.utils.TimePair;
import org.apache.dubbo.metrics.event.TimeCounterEvent;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsLevel;
import org.apache.dubbo.metrics.model.key.TypeWrapper;
import org.apache.dubbo.metrics.registry.RegistryMetricsConstants;
import org.apache.dubbo.metrics.registry.collector.RegistryMetricsCollector;
import org.apache.dubbo.registry.client.event.RegistryNotifyEvent;
import org.apache.dubbo.registry.client.event.RegistryRegisterEvent;
import org.apache.dubbo.registry.client.event.RegistryRsEvent;
import org.apache.dubbo.registry.client.event.RegistrySsEvent;
import org.apache.dubbo.registry.client.event.RegistrySubscribeEvent;
import org.apache.dubbo.rpc.cluster.directory.DirectoryRefreshEvent;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_DIRECTORY_MAP;
import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_LAST_NUM_MAP;
import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_SERVICE;
import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_SIZE;

/**
 * Registry related events
 */
public class RegistryMetricsEvent extends TimeCounterEvent {
    public RegistryMetricsEvent(ApplicationModel applicationModel, TypeWrapper typeWrapper, TimePair timePair) {
        super(applicationModel, typeWrapper, timePair);
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

    public static RegistryMetricsEvent toRegisterEvent(
            ApplicationModel applicationModel, List<String> registryClusterNames, TimePair timePair) {
        RegistryMetricsEvent registryMetricsEvent =
                new RegistryMetricsEvent(applicationModel, REGISTER_EVENT, timePair);
        registryMetricsEvent.putAttachment(RegistryMetricsConstants.ATTACHMENT_REGISTRY_KEY, registryClusterNames);
        return registryMetricsEvent;
    }

    private static final TypeWrapper SUBSCRIBE_EVENT = new TypeWrapper(
            MetricsLevel.APP,
            MetricsKey.SUBSCRIBE_METRIC_NUM,
            MetricsKey.SUBSCRIBE_METRIC_NUM_SUCCEED,
            MetricsKey.SUBSCRIBE_METRIC_NUM_FAILED);

    public static RegistryMetricsEvent toSubscribeEvent(
            ApplicationModel applicationModel, String registryClusterName, TimePair timePair) {
        RegistryMetricsEvent ddEvent = new RegistryMetricsEvent(applicationModel, SUBSCRIBE_EVENT, timePair);
        ddEvent.putAttachment(
                RegistryMetricsConstants.ATTACHMENT_REGISTRY_KEY, Collections.singletonList(registryClusterName));
        return ddEvent;
    }

    private static final TypeWrapper NOTIFY_EVENT = new TypeWrapper(
            MetricsLevel.APP, MetricsKey.NOTIFY_METRIC_REQUESTS, MetricsKey.NOTIFY_METRIC_NUM_LAST, (MetricsKey) null);

    public static RegistryMetricsEvent toNotifyEvent(
            ApplicationModel applicationModel, Map<String, Integer> postResult, TimePair timePair) {
        RegistryMetricsEvent registryMetricsEvent = new RegistryMetricsEvent(applicationModel, NOTIFY_EVENT, timePair);
        registryMetricsEvent.putAttachment(ATTACHMENT_KEY_LAST_NUM_MAP, postResult);
        return registryMetricsEvent;
    }

    private static final TypeWrapper RS_EVENT = new TypeWrapper(
            MetricsLevel.SERVICE,
            MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS,
            MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_SUCCEED,
            MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_FAILED);

    public static RegistryMetricsEvent toRsEvent(
            ApplicationModel applicationModel,
            String serviceKey,
            int size,
            List<String> serviceDiscoveryNames,
            TimePair timePair) {
        RegistryMetricsEvent ddEvent = new RegistryMetricsEvent(applicationModel, RS_EVENT, timePair);
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

    public static RegistryMetricsEvent toSsEvent(
            ApplicationModel applicationModel,
            String serviceKey,
            List<String> serviceDiscoveryNames,
            TimePair timePair) {
        RegistryMetricsEvent ddEvent = new RegistryMetricsEvent(applicationModel, SS_EVENT, timePair);
        ddEvent.putAttachment(ATTACHMENT_KEY_SERVICE, serviceKey);
        ddEvent.putAttachment(ATTACHMENT_KEY_SIZE, 1);
        ddEvent.putAttachment(RegistryMetricsConstants.ATTACHMENT_REGISTRY_KEY, serviceDiscoveryNames);
        return ddEvent;
    }

    private static final TypeWrapper DIRECTORY_EVENT =
            new TypeWrapper(MetricsLevel.APP, MetricsKey.DIRECTORY_METRIC_NUM_VALID, null, null);

    public static RegistryMetricsEvent refreshDirectoryEvent(
            ApplicationModel applicationModel,
            DirectoryRefreshEvent.Summary summary,
            Map<String, String> attachments,
            TimePair timePair) {
        Map<MetricsKey, Map<String, Integer>> summaryMap = new HashMap<>();
        summaryMap.put(MetricsKey.DIRECTORY_METRIC_NUM_VALID, summary.directoryNumValidMap);
        summaryMap.put(MetricsKey.DIRECTORY_METRIC_NUM_DISABLE, summary.directoryNumDisableMap);
        summaryMap.put(MetricsKey.DIRECTORY_METRIC_NUM_TO_RECONNECT, summary.directoryNumToReConnectMap);
        summaryMap.put(MetricsKey.DIRECTORY_METRIC_NUM_ALL, summary.directoryNumAllMap);

        RegistryMetricsEvent registryMetricsEvent =
                new RegistryMetricsEvent(applicationModel, DIRECTORY_EVENT, timePair);
        registryMetricsEvent.putAttachment(ATTACHMENT_DIRECTORY_MAP, summaryMap);
        registryMetricsEvent.putAttachments(attachments);
        return registryMetricsEvent;
    }

    public static RegistryMetricsEvent convertEvent(DubboEvent event) {
        if (event instanceof RegistryRegisterEvent) {
            RegistryRegisterEvent registerEvent = (RegistryRegisterEvent) event;
            return RegistryMetricsEvent.toRegisterEvent(
                    registerEvent.getApplicationModel(),
                    registerEvent.getRegistryClusterNames(),
                    registerEvent.getTimePair());
        } else if (event instanceof RegistrySubscribeEvent) {
            RegistrySubscribeEvent subscribeEvent = (RegistrySubscribeEvent) event;
            return RegistryMetricsEvent.toSubscribeEvent(
                    subscribeEvent.getApplicationModel(),
                    subscribeEvent.getRegistryClusterName(),
                    subscribeEvent.getTimePair());
        } else if (event instanceof RegistryNotifyEvent) {
            RegistryNotifyEvent notifyEvent = (RegistryNotifyEvent) event;
            return RegistryMetricsEvent.toNotifyEvent(
                    notifyEvent.getApplicationModel(), notifyEvent.getPostResult(), notifyEvent.getTimePair());
        } else if (event instanceof RegistryRsEvent) {
            RegistryRsEvent rsEvent = (RegistryRsEvent) event;
            return RegistryMetricsEvent.toRsEvent(
                    rsEvent.getApplicationModel(),
                    rsEvent.getServiceKey(),
                    rsEvent.getSize(),
                    rsEvent.getRegistryClusterNames(),
                    rsEvent.getTimePair());
        } else if (event instanceof RegistrySsEvent) {
            RegistrySsEvent ssEvent = (RegistrySsEvent) event;
            return RegistryMetricsEvent.toSsEvent(
                    ssEvent.getApplicationModel(),
                    ssEvent.getServiceKey(),
                    ssEvent.getRegistryClusterNames(),
                    ssEvent.getTimePair());
        } else if (event instanceof DirectoryRefreshEvent) {
            DirectoryRefreshEvent refreshEvent = (DirectoryRefreshEvent) event;
            return RegistryMetricsEvent.refreshDirectoryEvent(
                    refreshEvent.getApplicationModel(),
                    refreshEvent.getSummary(),
                    refreshEvent.getAttachments(),
                    refreshEvent.getTimePair());
        }
        return null;
    }
}
