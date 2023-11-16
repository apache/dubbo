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
package org.apache.dubbo.metrics.config.event;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.metrics.config.collector.ConfigCenterMetricsCollector;
import org.apache.dubbo.metrics.event.TimeCounterEvent;
import org.apache.dubbo.metrics.model.key.MetricsLevel;
import org.apache.dubbo.metrics.model.key.TypeWrapper;
import org.apache.dubbo.rpc.model.ApplicationModel;

import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_SIZE;
import static org.apache.dubbo.metrics.config.ConfigCenterMetricsConstants.ATTACHMENT_KEY_CHANGE_TYPE;
import static org.apache.dubbo.metrics.config.ConfigCenterMetricsConstants.ATTACHMENT_KEY_CONFIG_FILE;
import static org.apache.dubbo.metrics.config.ConfigCenterMetricsConstants.ATTACHMENT_KEY_CONFIG_GROUP;
import static org.apache.dubbo.metrics.config.ConfigCenterMetricsConstants.ATTACHMENT_KEY_CONFIG_PROTOCOL;
import static org.apache.dubbo.metrics.model.key.MetricsKey.CONFIGCENTER_METRIC_TOTAL;

/**
 * Registry related events
 * Triggered in three types of configuration centers (apollo, zk, nacos)
 */
public class ConfigCenterEvent extends TimeCounterEvent {

    public static final String NACOS_PROTOCOL = "nacos";
    public static final String APOLLO_PROTOCOL = "apollo";
    public static final String ZK_PROTOCOL = "zookeeper";

    public ConfigCenterEvent(ApplicationModel applicationModel, TypeWrapper typeWrapper) {
        super(applicationModel, typeWrapper);
        ScopeBeanFactory beanFactory = applicationModel.getBeanFactory();
        ConfigCenterMetricsCollector collector;
        if (!beanFactory.isDestroyed()) {
            collector = beanFactory.getBean(ConfigCenterMetricsCollector.class);
            super.setAvailable(collector != null && collector.isCollectEnabled());
        }
    }

    public static ConfigCenterEvent toChangeEvent(
            ApplicationModel applicationModel,
            String key,
            String group,
            String protocol,
            String changeType,
            int count) {
        ConfigCenterEvent configCenterEvent = new ConfigCenterEvent(
                applicationModel, new TypeWrapper(MetricsLevel.CONFIG, CONFIGCENTER_METRIC_TOTAL));
        configCenterEvent.putAttachment(ATTACHMENT_KEY_CONFIG_FILE, key);
        configCenterEvent.putAttachment(ATTACHMENT_KEY_CONFIG_GROUP, group);
        configCenterEvent.putAttachment(ATTACHMENT_KEY_CONFIG_PROTOCOL, protocol);
        configCenterEvent.putAttachment(ATTACHMENT_KEY_CHANGE_TYPE, changeType);
        configCenterEvent.putAttachment(ATTACHMENT_KEY_SIZE, count);
        return configCenterEvent;
    }
}
