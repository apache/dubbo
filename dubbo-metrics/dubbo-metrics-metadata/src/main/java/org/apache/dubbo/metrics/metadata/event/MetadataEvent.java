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
import org.apache.dubbo.metrics.metadata.collector.MetadataMetricsCollector;
import org.apache.dubbo.metrics.model.key.MetricsLevel;
import org.apache.dubbo.metrics.model.key.TypeWrapper;
import org.apache.dubbo.rpc.model.ApplicationModel;

import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_SERVICE;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METADATA_PUSH_METRIC_NUM;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METADATA_PUSH_METRIC_NUM_FAILED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METADATA_PUSH_METRIC_NUM_SUCCEED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METADATA_SUBSCRIBE_METRIC_NUM;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METADATA_SUBSCRIBE_METRIC_NUM_FAILED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METADATA_SUBSCRIBE_METRIC_NUM_SUCCEED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.STORE_PROVIDER_METADATA;
import static org.apache.dubbo.metrics.model.key.MetricsKey.STORE_PROVIDER_METADATA_FAILED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.STORE_PROVIDER_METADATA_SUCCEED;

/**
 * Registry related events
 */
public class MetadataEvent extends TimeCounterEvent {
    public MetadataEvent(ApplicationModel applicationModel, TypeWrapper typeWrapper) {
        super(applicationModel, typeWrapper);
        ScopeBeanFactory beanFactory = applicationModel.getBeanFactory();
        MetadataMetricsCollector collector;
        if (!beanFactory.isDestroyed()) {
            collector = beanFactory.getBean(MetadataMetricsCollector.class);
            super.setAvailable(collector != null && collector.isCollectEnabled());
        }
    }

    public static MetadataEvent toPushEvent(ApplicationModel applicationModel) {
        return new MetadataEvent(
                applicationModel,
                new TypeWrapper(
                        MetricsLevel.APP,
                        METADATA_PUSH_METRIC_NUM,
                        METADATA_PUSH_METRIC_NUM_SUCCEED,
                        METADATA_PUSH_METRIC_NUM_FAILED));
    }

    public static MetadataEvent toSubscribeEvent(ApplicationModel applicationModel) {
        return new MetadataEvent(
                applicationModel,
                new TypeWrapper(
                        MetricsLevel.APP,
                        METADATA_SUBSCRIBE_METRIC_NUM,
                        METADATA_SUBSCRIBE_METRIC_NUM_SUCCEED,
                        METADATA_SUBSCRIBE_METRIC_NUM_FAILED));
    }

    public static MetadataEvent toServiceSubscribeEvent(ApplicationModel applicationModel, String serviceKey) {
        MetadataEvent metadataEvent = new MetadataEvent(
                applicationModel,
                new TypeWrapper(
                        MetricsLevel.APP,
                        STORE_PROVIDER_METADATA,
                        STORE_PROVIDER_METADATA_SUCCEED,
                        STORE_PROVIDER_METADATA_FAILED));
        metadataEvent.putAttachment(ATTACHMENT_KEY_SERVICE, serviceKey);
        return metadataEvent;
    }
}
