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
import org.apache.dubbo.validation.support.jvalidation.mock.JValidatorTestTarget;
import org.apache.dubbo.validation.support.jvalidation.mock.ValidationParameter;

import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

class JValidatorTest {
    @Test
    void testItWithNonExistMethod() {
        Assertions.assertThrows(NoSuchMethodException.class, () -> {
            URL url = URL.valueOf(
                    "test://test:11/org.apache.dubbo.validation.support.jvalidation.mock.JValidatorTestTarget");
            JValidator jValidator = new JValidator(url);
            jValidator.validate("nonExistingMethod", new Class<?>[] {String.class}, new Object[] {"arg1"});
        });
    }

    @Test
    void testItWithExistMethod() throws Exception {
        URL url =
                URL.valueOf("test://test:11/org.apache.dubbo.validation.support.jvalidation.mock.JValidatorTestTarget");
        JValidator jValidator = new JValidator(url);
        jValidator.validate("someMethod1", new Class<?>[] {String.class}, new Object[] {"anything"});
    }

    @Test
    void testItWhenItViolatedConstraint() {
        Assertions.assertThrows(ValidationException.class, () -> {
            URL url = URL.valueOf(
                    "test://test:11/org.apache.dubbo.validation.support.jvalidation.mock.JValidatorTestTarget");
            JValidator jValidator = new JValidator(url);
            jValidator.validate(
                    "someMethod2", new Class<?>[] {ValidationParameter.class}, new Object[] {new ValidationParameter()
                    });
        });
    }

    @Test
    void testItWhenItMeetsConstraint() throws Exception {
        URL url =
                URL.valueOf("test://test:11/org.apache.dubbo.validation.support.jvalidation.mock.JValidatorTestTarget");
        JValidator jValidator = new JValidator(url);
        jValidator.validate("someMethod2", new Class<?>[] {ValidationParameter.class}, new Object[] {
            new ValidationParameter("NotBeNull")
        });
    }

    @Test
    void testItWithArrayArg() throws Exception {
        URL url =
                URL.valueOf("test://test:11/org.apache.dubbo.validation.support.jvalidation.mock.JValidatorTestTarget");
        JValidator jValidator = new JValidator(url);
        jValidator.validate("someMethod3", new Class<?>[] {ValidationParameter[].class}, new Object[] {
            new ValidationParameter[] {new ValidationParameter("parameter")}
        });
    }

    @Test
    void testItWithCollectionArg() throws Exception {
        URL url =
                URL.valueOf("test://test:11/org.apache.dubbo.validation.support.jvalidation.mock.JValidatorTestTarget");
        JValidator jValidator = new JValidator(url);
        jValidator.validate(
                "someMethod4", new Class<?>[] {List.class}, new Object[] {Collections.singletonList("parameter")});
    }

    @Test
    void testItWithMapArg() throws Exception {
        URL url =
                URL.valueOf("test://test:11/org.apache.dubbo.validation.support.jvalidation.mock.JValidatorTestTarget");
        JValidator jValidator = new JValidator(url);
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        jValidator.validate("someMethod5", new Class<?>[] {Map.class}, new Object[] {map});
    }

    @Test
    void testItWithPrimitiveArg() {
        Assertions.assertThrows(ValidationException.class, () -> {
            URL url = URL.valueOf(
                    "test://test:11/org.apache.dubbo.validation.support.jvalidation.mock.JValidatorTestTarget");
            JValidator jValidator = new JValidator(url);
            jValidator.validate("someMethod6", new Class<?>[] {Integer.class, String.class, Long.class}, new Object[] {
                null, null, null
            });
        });
    }

    @Test
    void testItWithPrimitiveArgWithProvidedMessage() {
        URL url =
                URL.valueOf("test://test:11/org.apache.dubbo.validation.support.jvalidation.mock.JValidatorTestTarget");
        JValidator jValidator = new JValidator(url);
        try {
            jValidator.validate("someMethod6", new Class<?>[] {Integer.class, String.class, Long.class}, new Object[] {
                null, "", null
            });
            Assertions.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("string must not be blank"));
            assertThat(e.getMessage(), containsString("longValue must not be null"));
        }
    }

    @Test
    void testItWithPartialParameterValidation() {
        URL url =
                URL.valueOf("test://test:11/org.apache.dubbo.validation.support.jvalidation.mock.JValidatorTestTarget");
        JValidator jValidator = new JValidator(url);
        try {
            jValidator.validate("someMethod6", new Class<?>[] {Integer.class, String.class, Long.class}, new Object[] {
                null, "", null
            });
            Assertions.fail();
        } catch (Exception e) {
            assertThat(e, instanceOf(ConstraintViolationException.class));
            ConstraintViolationException e1 = (ConstraintViolationException) e;
            assertThat(e1.getConstraintViolations().size(), is(2));
        }
    }

    @Test
    void testItWithNestedParameterValidationWithNullParam() {
        Assertions.assertThrows(ValidationException.class, () -> {
            URL url = URL.valueOf(
                    "test://test:11/org.apache.dubbo.validation.support.jvalidation.mock.JValidatorTestTarget");
            JValidator jValidator = new JValidator(url);
            jValidator.validate(
                    "someMethod7", new Class<?>[] {JValidatorTestTarget.BaseParam.class}, new Object[] {null});
        });
    }

    @Test
    void testItWithNestedParameterValidationWithNullNestedParam() {
        URL url =
                URL.valueOf("test://test:11/org.apache.dubbo.validation.support.jvalidation.mock.JValidatorTestTarget");
        JValidator jValidator = new JValidator(url);
        try {
            JValidatorTestTarget.BaseParam<JValidatorTestTarget.Param> param = new JValidatorTestTarget.BaseParam<>();
            jValidator.validate(
                    "someMethod7", new Class<?>[] {JValidatorTestTarget.BaseParam.class}, new Object[] {param});
            Assertions.fail();
        } catch (Exception e) {
            assertThat(e, instanceOf(ConstraintViolationException.class));
            ConstraintViolationException e1 = (ConstraintViolationException) e;
            assertThat(e1.getConstraintViolations().size(), is(1));
            assertThat(e1.getMessage(), containsString("body must not be null"));
        }
    }

    @Test
    void testItWithNestedParameterValidationWithNullNestedParams() {
        URL url =
                URL.valueOf("test://test:11/org.apache.dubbo.validation.support.jvalidation.mock.JValidatorTestTarget");
        JValidator jValidator = new JValidator(url);
        try {
            JValidatorTestTarget.BaseParam<JValidatorTestTarget.Param> param = new JValidatorTestTarget.BaseParam<>();
            param.setBody(new JValidatorTestTarget.Param());
            jValidator.validate(
                    "someMethod7", new Class<?>[] {JValidatorTestTarget.BaseParam.class}, new Object[] {param});
            Assertions.fail();
        } catch (Exception e) {
            assertThat(e, instanceOf(ConstraintViolationException.class));
            ConstraintViolationException e1 = (ConstraintViolationException) e;
            assertThat(e1.getConstraintViolations().size(), is(1));
            assertThat(e1.getMessage(), containsString("name must not be null"));
        }
    }
}
