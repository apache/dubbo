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

package org.apache.dubbo.metrics.event;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.metrics.listener.MetricsLifeListener;
import org.apache.dubbo.metrics.listener.MetricsListener;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.EXECUTOR_MANAGEMENT_MODE_DEFAULT;

public class SimpleMetricsEventMulticasterTest {

    private SimpleMetricsEventMulticaster eventMulticaster;
    private Object[] objects;
    private final Object obj = new Object();
    private MetricsEvent requestEvent;

    @BeforeEach
    public void setup() {
        eventMulticaster = new SimpleMetricsEventMulticaster();
        objects = new Object[]{obj};
        eventMulticaster.addListener(new MetricsListener<MetricsEvent>() {
            @Override
            public void onEvent(MetricsEvent event) {
                objects[0] = new Object();
            }
        });
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        ApplicationConfig applicationConfig = new ApplicationConfig("provider-app");
        applicationConfig.setExecutorManagementMode(EXECUTOR_MANAGEMENT_MODE_DEFAULT);
        applicationModel.getApplicationConfigManager().setApplication(applicationConfig);
        ConfigManager configManager = new ConfigManager(applicationModel);
        configManager.setApplication(applicationConfig);
        applicationModel.setConfigManager(configManager);
        requestEvent = new MetricsEvent(applicationModel) {
        };
    }


    @Test
    void testPublishEvent() {

        // emptyEvent do nothing
        MetricsEvent emptyEvent = EmptyEvent.instance();
        eventMulticaster.publishEvent(emptyEvent);
        Assertions.assertSame(obj, objects[0]);

    }

    @Test
    void testPublishFinishEvent() {

        //do nothing with no MetricsLifeListener
        eventMulticaster.publishFinishEvent(requestEvent);
        Assertions.assertSame(obj, objects[0]);

        //do onEventFinish with MetricsLifeListener
        eventMulticaster.addListener((new MetricsLifeListener<MetricsEvent>() {

            @Override
            public void onEvent(MetricsEvent event) {

            }

            @Override
            public void onEventFinish(MetricsEvent event) {
                objects[0] = new Object();
            }

            @Override
            public void onEventError(MetricsEvent event) {

            }
        }));
        eventMulticaster.publishFinishEvent(requestEvent);
        Assertions.assertNotSame(obj, objects[0]);

    }

}
