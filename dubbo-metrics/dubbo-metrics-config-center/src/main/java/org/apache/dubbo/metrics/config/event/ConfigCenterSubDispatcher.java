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

import org.apache.dubbo.metrics.config.collector.ConfigCenterMetricsCollector;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.SimpleMetricsEventMulticaster;
import org.apache.dubbo.metrics.event.TimeCounterEvent;
import org.apache.dubbo.metrics.listener.AbstractMetricsKeyListener;
import org.apache.dubbo.metrics.model.key.MetricsKey;

import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_SIZE;
import static org.apache.dubbo.metrics.config.ConfigCenterMetricsConstants.ATTACHMENT_KEY_CHANGE_TYPE;
import static org.apache.dubbo.metrics.config.ConfigCenterMetricsConstants.ATTACHMENT_KEY_CONFIG_FILE;
import static org.apache.dubbo.metrics.config.ConfigCenterMetricsConstants.ATTACHMENT_KEY_CONFIG_GROUP;
import static org.apache.dubbo.metrics.config.ConfigCenterMetricsConstants.ATTACHMENT_KEY_CONFIG_PROTOCOL;

public final class ConfigCenterSubDispatcher extends SimpleMetricsEventMulticaster {

    public ConfigCenterSubDispatcher(ConfigCenterMetricsCollector collector) {

        super.addListener(new AbstractMetricsKeyListener(MetricsKey.CONFIGCENTER_METRIC_TOTAL) {
            @Override
            public boolean isSupport(MetricsEvent event) {
                return event instanceof ConfigCenterEvent;
            }

            @Override
            public void onEvent(TimeCounterEvent event) {
                collector.increase(
                        event.getAttachmentValue(ATTACHMENT_KEY_CONFIG_FILE),
                        event.getAttachmentValue(ATTACHMENT_KEY_CONFIG_GROUP),
                        event.getAttachmentValue(ATTACHMENT_KEY_CONFIG_PROTOCOL),
                        event.getAttachmentValue(ATTACHMENT_KEY_CHANGE_TYPE),
                        event.getAttachmentValue(ATTACHMENT_KEY_SIZE));
            }
        });
    }
}
