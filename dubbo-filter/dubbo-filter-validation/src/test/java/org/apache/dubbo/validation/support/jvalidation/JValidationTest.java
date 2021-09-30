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
import org.apache.dubbo.validation.Validation;
import org.apache.dubbo.validation.Validator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.validation.ValidationException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class JValidationTest {
    @Test
    public void testReturnTypeWithInvalidValidationProvider() {
        Assertions.assertThrows(ValidationException.class, () -> {
            Validation jValidation = new JValidation();
            URL url = URL.valueOf("test://test:11/org.apache.dubbo.validation.support.jvalidation.JValidation?" +
                    "jvalidation=org.apache.dubbo.validation.Validation");
            jValidation.getValidator(url);
        });

    }

    @Test
    public void testReturnTypeWithDefaultValidatorProvider() {
        Validation jValidation = new JValidation();
        URL url = URL.valueOf("test://test:11/org.apache.dubbo.validation.support.jvalidation.JValidation");
        Validator validator = jValidation.getValidator(url);
        assertThat(validator instanceof JValidator, is(true));
    }
}
