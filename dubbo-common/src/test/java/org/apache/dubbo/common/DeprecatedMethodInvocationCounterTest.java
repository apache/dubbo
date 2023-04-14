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

package org.apache.dubbo.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Tests of DeprecatedMethodInvocationCounter.
 */
class DeprecatedMethodInvocationCounterTest {

    private static final String METHOD_DEFINITION = "org.apache.dubbo.common.URL.getServiceName()";

    @Test
    void testRealInvocation() {
        Assertions.assertFalse(DeprecatedMethodInvocationCounter.hasThisMethodInvoked(METHOD_DEFINITION));

        // Invoke a deprecated method.
        URL url = URL.valueOf("dubbo://admin:hello1234@10.20.130.230:20880/context/path#index?version=1.0.0&id=org.apache.dubbo.config.RegistryConfig#0");

        // Not a typo, intentionally invoke twice.
        invokeDeprecatedMethod(url);
        invokeDeprecatedMethod(url);

        Assertions.assertTrue(DeprecatedMethodInvocationCounter.hasThisMethodInvoked(METHOD_DEFINITION));

        Map<String, Integer> record = DeprecatedMethodInvocationCounter.getInvocationRecord();
        Assertions.assertEquals(2, record.get(METHOD_DEFINITION));
    }

    private void invokeDeprecatedMethod(URL url) {
        try {
            Method m = URL.class.getMethod("getServiceName");
            m.invoke(url);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
