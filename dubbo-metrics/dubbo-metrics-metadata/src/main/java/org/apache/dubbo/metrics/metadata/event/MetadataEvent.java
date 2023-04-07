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

package org.apache.dubbo.metrics.metadata.event;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.metrics.event.TimeCounterEvent;
import org.apache.dubbo.metrics.exception.MetricsNeverHappenException;
import org.apache.dubbo.metrics.metadata.collector.MetadataMetricsCollector;
import org.apache.dubbo.metrics.metadata.type.ApplicationType;
import org.apache.dubbo.metrics.metadata.type.ServiceType;
import org.apache.dubbo.metrics.model.key.MetricsLevel;
import org.apache.dubbo.metrics.model.key.TypeWrapper;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_SERVICE;

/**
 * Registry related events
 */
public class MetadataEvent extends TimeCounterEvent {
    private final MetadataMetricsCollector collector;
    private final Map<String, Object> attachment = new HashMap<>(8);

    public MetadataEvent(ApplicationModel applicationModel, TypeWrapper typeWrapper) {
        super(applicationModel);
        super.typeWrapper = typeWrapper;
        ScopeBeanFactory beanFactory = applicationModel.getBeanFactory();
        if (beanFactory.isDestroyed()) {
            this.collector = null;
        } else {
            this.collector = beanFactory.getBean(MetadataMetricsCollector.class);
            super.setAvailable(this.collector != null && collector.isCollectEnabled());
        }
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

    public ApplicationModel getSource() {
        return source;
    }

    public MetadataMetricsCollector getCollector() {
        return collector;
    }

    public static MetadataEvent toPushEvent(ApplicationModel applicationModel) {
        return new MetadataEvent(applicationModel, new TypeWrapper(MetricsLevel.APP, ApplicationType.P_TOTAL, ApplicationType.P_SUCCEED, ApplicationType.P_FAILED));
    }

    public static MetadataEvent toSubscribeEvent(ApplicationModel applicationModel) {
        return new MetadataEvent(applicationModel, new TypeWrapper(MetricsLevel.APP, ApplicationType.S_TOTAL, ApplicationType.S_SUCCEED, ApplicationType.S_FAILED));
    }

    public static MetadataEvent toServiceSubscribeEvent(ApplicationModel applicationModel, String serviceKey) {
        MetadataEvent metadataEvent = new MetadataEvent(applicationModel, new TypeWrapper(MetricsLevel.APP, ServiceType.S_P_TOTAL, ServiceType.S_P_SUCCEED, ServiceType.S_P_FAILED));
        metadataEvent.putAttachment(ATTACHMENT_KEY_SERVICE, serviceKey);
        return metadataEvent;
    }

}
