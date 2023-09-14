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
package org.apache.dubbo.spring.boot.context.event;

import org.apache.dubbo.common.utils.PojoUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author: Ares
 * @time: 2023-09-13 20:00:03
 * {@link Jsr310ConverterApplicationListener} Test
 * @see Jsr310ConverterApplicationListener
 * @since 2.7.24
 */
@RunWith(SpringRunner.class)
@TestPropertySource(
        properties = {
                "dubbo.generic.local-date-time-format = yyyy-MM-dd HH:mm:ss.SSS",
                "dubbo.generic.local-time-format = HH:mm:ss||HH:mm:ss.SSS"}
)
@SpringBootTest(
        classes = {Jsr310ConverterApplicationListener.class}
)
public class Jsr310ConverterApplicationListenerTest {

    @BeforeClass
    public static void init() {
        ApplicationModel.reset();
    }

    @AfterClass
    public static void destroy() {
        ApplicationModel.reset();
    }

    @Test
    public void testOnApplicationEvent() {
        LocalDateTime now = LocalDateTime.now();
        Object localDateTimeGen = PojoUtils.generalize(now);
        Object localDateTime = PojoUtils.realize(localDateTimeGen, LocalDateTime.class);
        assertEquals(localDateTimeGen.toString().length(), "yyyy-MM-dd HH:mm:ss.SSS".length());
        assertTrue(localDateTime instanceof LocalDateTime);

        LocalTime nowTime = LocalTime.now();
        Object localTimeGen = PojoUtils.generalize(nowTime);
        Object localTime = PojoUtils.realize(localTimeGen, LocalTime.class);
        assertTrue(localTime instanceof LocalTime);
        assertEquals(localTimeGen.toString().length(), 8);

        localTime = PojoUtils.realize(localTimeGen + ".001", LocalTime.class);
        assertTrue(localTime instanceof LocalTime);
        assertEquals(localTime.toString().length(), 12);
    }

}
