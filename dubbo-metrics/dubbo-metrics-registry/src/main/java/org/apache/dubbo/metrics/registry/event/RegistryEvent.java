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
import org.apache.dubbo.metrics.model.key.MetricsKeyDecorator;
import org.apache.dubbo.metrics.registry.RegistryConstants;
import org.apache.dubbo.metrics.registry.collector.RegistryMetricsCollector;
import org.apache.dubbo.metrics.registry.event.type.ApplicationType;
import org.apache.dubbo.metrics.registry.event.type.ServiceType;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.metrics.registry.RegistryConstants.ATTACHMENT_KEY_LAST_NUM_MAP;
import static org.apache.dubbo.metrics.registry.RegistryConstants.ATTACHMENT_KEY_SERVICE;
import static org.apache.dubbo.metrics.registry.RegistryConstants.ATTACHMENT_KEY_SIZE;

/**
 * Registry related events
 */
public class RegistryEvent extends TimeCounterEvent {
    private final RegistryMetricsCollector collector;
    protected Map<String, Object> attachment = new HashMap<>(8);

    public RegistryEvent(ApplicationModel applicationModel, MetricsKeyDecorator key) {
        super(applicationModel, key);
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


    public void setLastNum(ServiceType type) {
        getCollector().setNum(type, getSource().getApplicationName(), getAttachmentValue(ATTACHMENT_KEY_LAST_NUM_MAP));
    }

    public void addApplicationRT(String opType) {
        getCollector().updateAppRt(getSource().getApplicationName(), opType, getTimePair().calc());

    }

    public void setNum(ApplicationType type, String attachmentKey) {
        getCollector().setNum(type, getSource().getApplicationName(), getAttachmentValue(attachmentKey));
    }

    public void incrementServiceKey(ServiceType type, String attServiceKey, String attSize) {
        incrementServiceKey(type, attServiceKey, (int) getAttachmentValue(attSize));
    }

    public void incrementServiceKey(ServiceType type, String attServiceKey, int size) {
        getCollector().incrementServiceKey(getSource().getApplicationName(), getAttachmentValue(attServiceKey), type, size);
    }

    public void addServiceKeyRT(String attServiceKey, String attSize) {
        getCollector().addServiceKeyRT(getSource().getApplicationName(), getAttachmentValue(attServiceKey), attSize, getTimePair().calc());
    }

    public void increment(MetricsKey metricsKey) {
        getCollector().incrAppNum(getSource().getApplicationName(), metricsKey);
    }


    public static RegistryEvent toRegisterEvent(ApplicationModel applicationModel) {
        return new RegistryEvent(applicationModel, RegistryConstants.REGISTER_KEY);
    }


    public static RegistryEvent toSubscribeEvent(ApplicationModel applicationModel) {
        return new RegistryEvent(applicationModel, RegistryConstants.SUBSCRIBE_KEY);
    }


    public static RegistryEvent toNotifyEvent(ApplicationModel applicationModel) {
        return new RegistryEvent(applicationModel, RegistryConstants.NOTIFY_KEY) {
            @Override
            public void customAfterPost(Object postResult) {
                super.attachment.put(ATTACHMENT_KEY_LAST_NUM_MAP, postResult);
            }
        };
    }

    public static RegistryEvent toRsEvent(ApplicationModel applicationModel, String serviceKey, int size) {
        RegistryEvent ddEvent = new RegistryEvent(applicationModel, RegistryConstants.SERVICE_REGISTER_KEY);
        ddEvent.attachment.put(ATTACHMENT_KEY_SERVICE, serviceKey);
        ddEvent.attachment.put(ATTACHMENT_KEY_SIZE, size);
        return ddEvent;
    }

    public static RegistryEvent toSsEvent(ApplicationModel applicationModel, String serviceKey) {
        RegistryEvent ddEvent = new RegistryEvent(applicationModel, RegistryConstants.SERVICE_SUBSCRIBE_KEY);
        ddEvent.attachment.put(ATTACHMENT_KEY_SERVICE, serviceKey);
        return ddEvent;
    }


}
