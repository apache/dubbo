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
package org.apache.dubbo.validation.support.jvalidation;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.validation.support.jvalidation.mock.ValidationParameter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.validation.ValidationException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class JValidatorTest {
    @Test
    void testItWithNonExistMethod() {
        Assertions.assertThrows(NoSuchMethodException.class, () -> {
            URL url = URL.valueOf("test://test:11/org.apache.dubbo.validation.support.jvalidation.mock.JValidatorTestTarget");
            JValidator jValidator = new JValidator(url);
            jValidator.validate("nonExistingMethod", new Class<?>[]{String.class}, new Object[]{"arg1"});
        });
    }

    @Test
    void testItWithExistMethod() throws Exception {
        URL url = URL.valueOf("test://test:11/org.apache.dubbo.validation.support.jvalidation.mock.JValidatorTestTarget");
        JValidator jValidator = new JValidator(url);
        jValidator.validate("someMethod1", new Class<?>[]{String.class}, new Object[]{"anything"});
    }

    @Test
    void testItWhenItViolatedConstraint() {
        Assertions.assertThrows(ValidationException.class, () -> {
            URL url = URL.valueOf("test://test:11/org.apache.dubbo.validation.support.jvalidation.mock.JValidatorTestTarget");
            JValidator jValidator = new JValidator(url);
            jValidator.validate("someMethod2", new Class<?>[]{ValidationParameter.class}, new Object[]{new ValidationParameter()});
        });
    }

    @Test
    void testItWhenItMeetsConstraint() throws Exception {
        URL url = URL.valueOf("test://test:11/org.apache.dubbo.validation.support.jvalidation.mock.JValidatorTestTarget");
        JValidator jValidator = new JValidator(url);
        jValidator.validate("someMethod2", new Class<?>[]{ValidationParameter.class}, new Object[]{new ValidationParameter("NotBeNull")});
    }

    @Test
    void testItWithArrayArg() throws Exception {
        URL url = URL.valueOf("test://test:11/org.apache.dubbo.validation.support.jvalidation.mock.JValidatorTestTarget");
        JValidator jValidator = new JValidator(url);
        jValidator.validate("someMethod3", new Class<?>[]{ValidationParameter[].class}, new Object[]{new ValidationParameter[]{new ValidationParameter("parameter")}});
    }

    @Test
    void testItWithCollectionArg() throws Exception {
        URL url = URL.valueOf("test://test:11/org.apache.dubbo.validation.support.jvalidation.mock.JValidatorTestTarget");
        JValidator jValidator = new JValidator(url);
        jValidator.validate("someMethod4", new Class<?>[]{List.class}, new Object[]{Arrays.asList("parameter")});
    }

    @Test
    void testItWithMapArg() throws Exception {
        URL url = URL.valueOf("test://test:11/org.apache.dubbo.validation.support.jvalidation.mock.JValidatorTestTarget");
        JValidator jValidator = new JValidator(url);
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        jValidator.validate("someMethod5", new Class<?>[]{Map.class}, new Object[]{map});
    }
}