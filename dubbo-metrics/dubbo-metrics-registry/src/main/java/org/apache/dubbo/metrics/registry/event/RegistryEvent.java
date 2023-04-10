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
import org.apache.dubbo.metrics.exception.MetricsNeverHappenException;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsLevel;
import org.apache.dubbo.metrics.model.key.MetricsPlaceType;
import org.apache.dubbo.metrics.model.key.TypeWrapper;
import org.apache.dubbo.metrics.registry.collector.RegistryMetricsCollector;
import org.apache.dubbo.metrics.registry.event.type.ApplicationType;
import org.apache.dubbo.metrics.registry.event.type.ServiceType;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_LAST_NUM_MAP;
import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_SERVICE;
import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_SIZE;

/**
 * Registry related events
 */
public class RegistryEvent extends TimeCounterEvent {
    private final RegistryMetricsCollector collector;
    private final Map<String, Object> attachment = new HashMap<>(8);

    public RegistryEvent(ApplicationModel applicationModel, TypeWrapper typeWrapper) {
        super(applicationModel);
        super.typeWrapper = typeWrapper;
        ScopeBeanFactory beanFactory = getSource().getBeanFactory();
        if (beanFactory.isDestroyed()) {
            this.collector = null;
        } else {
            this.collector = beanFactory.getBean(RegistryMetricsCollector.class);
            super.setAvailable(this.collector != null && collector.isCollectEnabled());
        }
    }


    public ApplicationModel getSource() {
        return source;
    }

    public RegistryMetricsCollector getCollector() {
        return collector;
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttachmentValue(String key) {
        if (!attachment.containsKey(key)) {
            throw new MetricsNeverHappenException("Attachment key [" + key + "] not found");
        }
        return (T) attachment.get(key);
    }

    public void putAttachment(String key, Object value) {
        attachment.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public void setLastNum(MetricsKey metricsKey) {
        getCollector().setNum(metricsKey, getSource().getApplicationName(), (Map<String, Integer>) getAttachmentValue(ATTACHMENT_KEY_LAST_NUM_MAP));
    }

    public void addApplicationRT(MetricsPlaceType placeType) {
        getCollector().addApplicationRT(getSource().getApplicationName(), placeType.getType(), getTimePair().calc());

    }

    public void setNum(MetricsKey metricsKey, String attachmentKey) {
        getCollector().setNum(metricsKey, getSource().getApplicationName(), (int) getAttachmentValue(attachmentKey));
    }

    public void incrementServiceKey(MetricsKey metricsKey, String attServiceKey, String attSize) {
        incrementServiceKey(metricsKey, attServiceKey, (int) getAttachmentValue(attSize));
    }

    public void incrementServiceKey(MetricsKey metricsKey, String attServiceKey, int size) {
        getCollector().incrementServiceKey(getSource().getApplicationName(), getAttachmentValue(attServiceKey), metricsKey, size);
    }

    public void addServiceKeyRT(String attServiceKey, String attSize) {
        getCollector().addServiceKeyRT(getSource().getApplicationName(), getAttachmentValue(attServiceKey), attSize, getTimePair().calc());
    }

    public void increment(MetricsKey metricsKey) {
        getCollector().increment(getSource().getApplicationName(), metricsKey);
    }


    public static RegistryEvent toRegisterEvent(ApplicationModel applicationModel) {
        return new RegistryEvent(applicationModel, new TypeWrapper(MetricsLevel.APP, ApplicationType.R_TOTAL, ApplicationType.R_SUCCEED, ApplicationType.R_FAILED));
    }


    public static RegistryEvent toSubscribeEvent(ApplicationModel applicationModel) {
        return new RegistryEvent(applicationModel, new TypeWrapper(MetricsLevel.APP, ApplicationType.S_TOTAL, ApplicationType.S_SUCCEED, ApplicationType.S_FAILED));
    }


    public static RegistryEvent toNotifyEvent(ApplicationModel applicationModel) {
        return new RegistryEvent(applicationModel, new TypeWrapper(MetricsLevel.APP, ApplicationType.N_TOTAL, ServiceType.N_LAST_NUM, null)) {
            @Override
            public void customAfterPost(Object postResult) {
                super.putAttachment(ATTACHMENT_KEY_LAST_NUM_MAP, postResult);
            }
        };
    }

    public static RegistryEvent toRsEvent(ApplicationModel applicationModel, String serviceKey, int size) {
        RegistryEvent ddEvent = new RegistryEvent(applicationModel, new TypeWrapper(MetricsLevel.SERVICE, ServiceType.R_SERVICE_TOTAL, ServiceType.R_SERVICE_SUCCEED, ServiceType.R_SERVICE_FAILED));
        ddEvent.putAttachment(ATTACHMENT_KEY_SERVICE, serviceKey);
        ddEvent.putAttachment(ATTACHMENT_KEY_SIZE, size);
        return ddEvent;
    }

    public static RegistryEvent toSsEvent(ApplicationModel applicationModel, String serviceKey) {
        RegistryEvent ddEvent = new RegistryEvent(applicationModel, new TypeWrapper(MetricsLevel.SERVICE, ServiceType.S_SERVICE_TOTAL, ServiceType.S_SERVICE_SUCCEED, ServiceType.S_SERVICE_FAILED));
        ddEvent.putAttachment(ATTACHMENT_KEY_SERVICE, serviceKey);
        return ddEvent;
    }


}
