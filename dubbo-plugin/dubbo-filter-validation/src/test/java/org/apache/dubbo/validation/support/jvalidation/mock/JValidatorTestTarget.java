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
package org.apache.dubbo.validation.support.jvalidation.mock;

import org.apache.dubbo.validation.MethodValidated;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

import org.hibernate.validator.constraints.NotBlank;

public interface JValidatorTestTarget {
    @MethodValidated
    void someMethod1(String anything);

    @MethodValidated(Test2.class)
    void someMethod2(@NotNull ValidationParameter validationParameter);

    void someMethod3(ValidationParameter[] parameters);

    void someMethod4(List<String> strings);

    void someMethod5(Map<String, String> map);

    void someMethod6(
            Integer intValue,
            @NotBlank(message = "string must not be blank") String string,
            @NotNull(message = "longValue must not be null") Long longValue);

    void someMethod7(@NotNull BaseParam<Param> baseParam);

    @interface Test2 {}

    class BaseParam<T> {

        @Valid
        @NotNull(message = "body must not be null")
        private T body;

        public T getBody() {
            return body;
        }

        public void setBody(T body) {
            this.body = body;
        }
    }

    class Param {

        @NotNull(message = "name must not be null")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
