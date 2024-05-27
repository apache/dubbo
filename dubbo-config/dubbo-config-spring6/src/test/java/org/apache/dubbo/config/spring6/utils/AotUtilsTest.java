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
package org.apache.dubbo.config.spring6.utils;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;

public class AotUtilsTest {

    @Test
    void registerSerializationForServiceTest() {

        RuntimeHints runtimeHints = new RuntimeHints();
        AotUtils.registerSerializationForService(DemoService.class, runtimeHints);

        AtomicBoolean containHelloRequest = new AtomicBoolean(false);
        runtimeHints.serialization().javaSerializationHints().forEach(s -> {
            if (s.getType().getName().equals(HelloRequest.class.getName())) {
                containHelloRequest.set(true);
            }
        });

        AtomicBoolean containPerson = new AtomicBoolean(false);
        runtimeHints.serialization().javaSerializationHints().forEach(s -> {
            if (s.getType().getName().equals(HelloRequest.class.getName())) {
                containPerson.set(true);
            }
        });

        AtomicBoolean containString = new AtomicBoolean(false);
        runtimeHints.serialization().javaSerializationHints().forEach(s -> {
            if (s.getType().getName().equals(HelloRequest.class.getName())) {
                containString.set(true);
            }
        });

        AtomicBoolean containHelloRequestSuper = new AtomicBoolean(false);
        runtimeHints.serialization().javaSerializationHints().forEach(s -> {
            if (s.getType().getName().equals(HelloRequest.class.getName())) {
                containHelloRequestSuper.set(true);
            }
        });

        AtomicBoolean containHelloResponse = new AtomicBoolean(false);
        runtimeHints.serialization().javaSerializationHints().forEach(s -> {
            if (s.getType().getName().equals(HelloRequest.class.getName())) {
                containHelloResponse.set(true);
            }
        });

        Assertions.assertTrue(containHelloRequest.get());
        Assertions.assertTrue(containPerson.get());
        Assertions.assertTrue(containString.get());
        Assertions.assertTrue(containHelloRequestSuper.get());
        Assertions.assertTrue(containHelloResponse.get());
    }

    @Test
    void registerSerializationForCircularDependencyFieldTest() {
        RuntimeHints runtimeHints = new RuntimeHints();
        AotUtils.registerSerializationForService(CircularDependencyDemoService.class, runtimeHints);
        AtomicBoolean containDemoA = new AtomicBoolean(false);
        runtimeHints.serialization().javaSerializationHints().forEach(s -> {
            if (s.getType().getName().equals(DemoA.class.getName())) {
                containDemoA.set(true);
            }
        });
        AtomicBoolean containDemoB = new AtomicBoolean(false);
        runtimeHints.serialization().javaSerializationHints().forEach(s -> {
            if (s.getType().getName().equals(DemoB.class.getName())) {
                containDemoB.set(true);
            }
        });

        Assertions.assertTrue(containDemoA.get());
        Assertions.assertTrue(containDemoB.get());

        AotUtils.registerSerializationForService(DemoService.class, runtimeHints);
        AtomicBoolean containSexEnum = new AtomicBoolean(false);
        runtimeHints.serialization().javaSerializationHints().forEach(s -> {
            if (s.getType().getName().equals(SexEnum.class.getName())) {
                containSexEnum.set(true);
            }
        });
        Assertions.assertTrue(containSexEnum.get());
    }
}
