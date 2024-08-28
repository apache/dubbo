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
package org.apache.dubbo.common.event;

import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DubboEventTest {

    private FrameworkModel frameworkModel;
    private ApplicationModel applicationModel;
    private ModuleModel moduleModel;

    @BeforeEach
    public void setUp() {
        frameworkModel = new FrameworkModel();
        applicationModel = frameworkModel.newApplication();
        moduleModel = applicationModel.newModule();
    }

    @AfterEach
    public void reset() {
        frameworkModel.destroy();
    }

    @Test
    public void testDubboListener() {
        AtomicInteger num = new AtomicInteger(0);
        List<Class<?>> classes = new ArrayList<>();
        DubboListener<DubboEvent> dubboListener = new DubboListener<DubboEvent>() {
            @Override
            public boolean support(Class<? extends DubboEvent> eventClass) {
                classes.add(eventClass);
                return true;
            }

            @Override
            public void onEvent(DubboEvent event) {
                num.incrementAndGet();
            }
        };
        DubboEventBus.addListener(applicationModel, dubboListener);

        FirstDubboEvent firstDubboEvent = new FirstDubboEvent(applicationModel);
        SecondDubboEvent secondDubboEvent = new SecondDubboEvent(applicationModel);
        DubboEventBus.publish(firstDubboEvent);
        DubboEventBus.publish(secondDubboEvent);

        Assertions.assertEquals(num.get(), 2);
        Assertions.assertEquals(classes.size(), 2);
        Assertions.assertTrue(classes.contains(FirstDubboEvent.class));
        Assertions.assertTrue(classes.contains(SecondDubboEvent.class));
    }

    @Test
    public void testAbstractDubboListener() {
        List<DubboEvent> eventList = new ArrayList<>();
        AbstractDubboListener<FirstDubboEvent> dubboListener = new AbstractDubboListener<FirstDubboEvent>() {
            @Override
            public void onEvent(FirstDubboEvent event) {
                eventList.add(event);
            }
        };
        DubboEventBus.addListener(applicationModel, dubboListener);

        FirstDubboEvent firstDubboEvent = new FirstDubboEvent(applicationModel);
        SecondDubboEvent secondDubboEvent = new SecondDubboEvent(applicationModel);
        DubboEventBus.publish(firstDubboEvent);
        DubboEventBus.publish(secondDubboEvent);

        Assertions.assertEquals(eventList.size(), 1);
        Assertions.assertTrue(eventList.contains(firstDubboEvent));
        Assertions.assertFalse(eventList.contains(secondDubboEvent));
    }

    @Test
    public void testAbstractDubboLifecycleListener() {
        List<DubboEvent> beforeEventList = new ArrayList<>();
        List<DubboEvent> afterEventList = new ArrayList<>();
        List<DubboEvent> errorEventList = new ArrayList<>();
        AbstractDubboLifecycleListener<FirstDubboEvent> dubboListener =
                new AbstractDubboLifecycleListener<FirstDubboEvent>() {

                    @Override
                    public void onEventBefore(FirstDubboEvent event) {
                        beforeEventList.add(event);
                    }

                    @Override
                    public void onEventFinish(FirstDubboEvent event) {
                        afterEventList.add(event);
                    }

                    @Override
                    public void onEventError(FirstDubboEvent event) {
                        errorEventList.add(event);
                    }
                };
        DubboEventBus.addListener(applicationModel, dubboListener);

        FirstDubboEvent firstDubboEvent = new FirstDubboEvent(applicationModel);
        SecondDubboEvent secondDubboEvent = new SecondDubboEvent(applicationModel);

        DubboEventBus.post(firstDubboEvent, FirstDubboEvent.class::getName);
        DubboEventBus.post(secondDubboEvent, SecondDubboEvent.class::getName);

        Assertions.assertEquals(beforeEventList.size(), 1);
        Assertions.assertEquals(beforeEventList.get(0), firstDubboEvent);
        Assertions.assertEquals(afterEventList.size(), 1);
        Assertions.assertEquals(afterEventList.get(0), firstDubboEvent);
        Assertions.assertEquals(errorEventList.size(), 0);

        beforeEventList.clear();
        afterEventList.clear();
        errorEventList.clear();

        try {
            DubboEventBus.post(firstDubboEvent, () -> {
                throw new RuntimeException();
            });
        } catch (RuntimeException e) {
        }
        try {
            DubboEventBus.post(secondDubboEvent, () -> {
                throw new RuntimeException();
            });
        } catch (RuntimeException e) {
        }

        Assertions.assertEquals(beforeEventList.size(), 1);
        Assertions.assertEquals(beforeEventList.get(0), firstDubboEvent);
        Assertions.assertEquals(afterEventList.size(), 0);
        Assertions.assertEquals(errorEventList.size(), 1);
        Assertions.assertEquals(errorEventList.get(0), firstDubboEvent);

        beforeEventList.clear();
        afterEventList.clear();
        errorEventList.clear();

        DubboEventBus.post(firstDubboEvent, FirstDubboEvent.class::getName, result -> false);
        DubboEventBus.post(secondDubboEvent, SecondDubboEvent.class::getName, result -> false);

        Assertions.assertEquals(beforeEventList.size(), 1);
        Assertions.assertEquals(beforeEventList.get(0), firstDubboEvent);
        Assertions.assertEquals(afterEventList.size(), 0);
        Assertions.assertEquals(errorEventList.size(), 1);
        Assertions.assertEquals(errorEventList.get(0), firstDubboEvent);
    }

    @Test
    public void testCustomAfterPostEventListener() {
        SecondDubboEvent secondDubboEvent = new SecondDubboEvent(applicationModel);

        DubboEventBus.post(secondDubboEvent, SecondDubboEvent.class::getName);
        List<String> resultList = secondDubboEvent.getResultList();

        Assertions.assertEquals(resultList.size(), 1);
        Assertions.assertEquals(resultList.get(0), SecondDubboEvent.class.getName());
    }

    public static class FirstDubboEvent extends DubboEvent {

        public FirstDubboEvent(ApplicationModel source) {
            super(source);
        }
    }

    public static class SecondDubboEvent extends DubboEvent implements CustomAfterPost<String> {

        private final List<String> resultList;

        public SecondDubboEvent(ApplicationModel source) {
            super(source);
            resultList = new ArrayList<>();
        }

        @Override
        public void customAfterPost(String postResult) {
            resultList.add(postResult);
        }

        public List<String> getResultList() {
            return resultList;
        }
    }
}
