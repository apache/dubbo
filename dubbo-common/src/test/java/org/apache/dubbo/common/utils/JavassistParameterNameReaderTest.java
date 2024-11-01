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
package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.URL;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.equalTo;

class JavassistParameterNameReaderTest {

    private final ParameterNameReader reader = new JavassistParameterNameReader();

    @Test
    void readFromConstructor() {
        Class<?> clazz = URL.class;
        for (Constructor<?> ctor : clazz.getConstructors()) {
            String[] names = reader.readParameterNames(ctor);
            // System.out.println(ctor + " -> " + Arrays.toString(names));
            if (names.length == 7) {
                assertThat(names[0], equalTo("protocol"));
            }
        }
    }

    @Test
    void readFromMethod() {
        Class<?> clazz = URL.class;
        for (Method method : clazz.getMethods()) {
            String[] names = reader.readParameterNames(method);
            // System.out.println(method + " -> " + Arrays.toString(names));
            switch (method.getName()) {
                case "getAddress":
                    assertThat(names, emptyArray());
                    break;
                case "setAddress":
                    assertThat(names[0], equalTo("address"));
                    break;
                case "buildKey":
                    assertThat(names[0], equalTo("path"));
                    break;
                default:
            }
        }
    }
}
