/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.common.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

public class MethodUtilsTest {

    @Test
    public void testGetMethod() {
        Method getMethod = null;
        for (Method method : MethodTestClazz.class.getMethods()) {
            if (MethodUtils.isGetter(method)) {
                getMethod = method;
            }
        }
        Assertions.assertNotNull(getMethod);
        Assertions.assertEquals("getValue", getMethod.getName());
    }

    @Test
    public void testSetMethod() {
        Method setMethod = null;
        for (Method method : MethodTestClazz.class.getMethods()) {
            if (MethodUtils.isSetter(method)) {
                setMethod = method;
            }
        }
        Assertions.assertNotNull(setMethod);
        Assertions.assertEquals("setValue", setMethod.getName());
    }

    public class MethodTestClazz {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

}
