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

import org.apache.dubbo.metrics.listener.MetricsLifeListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SimpleMetricsEventMulticasterTest {

    private SimpleMetricsEventMulticaster eventMulticaster;
    private Object[] obj;
    MetricsEvent requestEvent;

    @BeforeEach
    public void setup() {
        eventMulticaster = new SimpleMetricsEventMulticaster();
        obj = new Object[]{new Object()};
        eventMulticaster.addListener(event -> obj[0] = new Object());
        requestEvent = new RequestEvent(obj[0], MetricsEvent.Type.TOTAL);

    }


    @Test
    void testPublishEvent() {

        // emptyEvent do nothing
        MetricsEvent emptyEvent = new EmptyEvent(obj[0]);
        eventMulticaster.publishEvent(emptyEvent);
        Assertions.assertSame(emptyEvent.getSource(), obj[0]);

        // not empty Event change obj[]
        MetricsEvent requestEvent = new RequestEvent(obj[0], MetricsEvent.Type.TOTAL);
        eventMulticaster.publishEvent(requestEvent);
        Assertions.assertNotSame(requestEvent.getSource(), obj[0]);

    }

    @Test
    void testPublishFinishEvent() {

        //do nothing with no MetricsLifeListener
        eventMulticaster.publishFinishEvent(requestEvent);
        Assertions.assertSame(requestEvent.getSource(), obj[0]);

        //do onEventFinish with MetricsLifeListener
        eventMulticaster.addListener((new MetricsLifeListener<MetricsEvent>() {

            @Override
            public void onEvent(MetricsEvent event) {

            }

            @Override
            public void onEventFinish(MetricsEvent event) {
                obj[0] = new Object();
            }

            @Override
            public void onEventError(MetricsEvent event) {

            }
        }));
        eventMulticaster.publishFinishEvent(requestEvent);
        Assertions.assertNotSame(requestEvent.getSource(), obj[0]);

    }

    @Test
    void testPublishErrorEvent() {

    }
}
