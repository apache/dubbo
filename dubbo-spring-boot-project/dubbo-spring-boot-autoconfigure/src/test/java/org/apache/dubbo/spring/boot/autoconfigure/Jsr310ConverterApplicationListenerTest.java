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
package org.apache.dubbo.spring.boot.autoconfigure;

import org.apache.dubbo.common.utils.PojoUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * {@link Jsr310ConverterApplicationListener} Test
 * @see Jsr310ConverterApplicationListener
 * @since 3.3.6
 */
@TestPropertySource(
        properties = {
            "dubbo.generic.local-date-time-format = yyyy-MM-dd HH:mm:ss.SSS",
            "dubbo.generic.local-time-format = HH:mm:ss||HH:mm:ss.SSS"
        })
@SpringBootTest(classes = {Jsr310ConverterApplicationListener.class})
@Disabled
public class Jsr310ConverterApplicationListenerTest {

    @BeforeAll
    public static void init() {
        ApplicationModel.reset();
    }

    @AfterAll
    public static void destroy() {
        ApplicationModel.reset();
    }

    @Test
    public void testOnApplicationEvent() {
        LocalDateTime now = LocalDateTime.now();
        Object localDateTimeGen = PojoUtils.generalize(now);
        Object localDateTime = PojoUtils.realize(localDateTimeGen, LocalDateTime.class);
        assertEquals(localDateTimeGen.toString().length(), "yyyy-MM-dd HH:mm:ss.SSS".length());
        assertInstanceOf(LocalDateTime.class, localDateTime);

        LocalTime nowTime = LocalTime.now();
        Object localTimeGen = PojoUtils.generalize(nowTime);
        Object localTime = PojoUtils.realize(localTimeGen, LocalTime.class);
        assertInstanceOf(LocalTime.class, localTime);
        assertEquals(8, localTimeGen.toString().length());

        localTime = PojoUtils.realize(localTimeGen + ".001", LocalTime.class);
        assertInstanceOf(LocalTime.class, localTime);
        assertEquals(12, localTime.toString().length());
    }
}
