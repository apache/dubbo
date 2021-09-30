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

import static org.apache.dubbo.common.utils.MethodUtils.excludedDeclaredClass;
import static org.apache.dubbo.common.utils.MethodUtils.findMethod;
import static org.apache.dubbo.common.utils.MethodUtils.findNearestOverriddenMethod;
import static org.apache.dubbo.common.utils.MethodUtils.findOverriddenMethod;
import static org.apache.dubbo.common.utils.MethodUtils.getAllDeclaredMethods;
import static org.apache.dubbo.common.utils.MethodUtils.getAllMethods;
import static org.apache.dubbo.common.utils.MethodUtils.getDeclaredMethods;
import static org.apache.dubbo.common.utils.MethodUtils.getMethods;
import static org.apache.dubbo.common.utils.MethodUtils.invokeMethod;
import static org.apache.dubbo.common.utils.MethodUtils.overrides;

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

    @Test
    public void testIsDeprecated() throws Exception {
        Assertions.assertTrue(MethodUtils.isDeprecated(MethodTestClazz.class.getMethod("deprecatedMethod")));
        Assertions.assertFalse(MethodUtils.isDeprecated(MethodTestClazz.class.getMethod("getValue")));
    }

    @Test
    public void testIsMetaMethod() {
        boolean containMetaMethod = false;
        for (Method method : MethodTestClazz.class.getMethods()) {
            if (MethodUtils.isMetaMethod(method)) {
                containMetaMethod = true;
            }
        }
        Assertions.assertTrue(containMetaMethod);
    }

    @Test
    public void testGetMethods() throws NoSuchMethodException {
        Assertions.assertTrue(getDeclaredMethods(MethodTestClazz.class, excludedDeclaredClass(String.class)).size() > 0);
        Assertions.assertTrue(getMethods(MethodTestClazz.class).size() > 0);
        Assertions.assertTrue(getAllDeclaredMethods(MethodTestClazz.class).size() > 0);
        Assertions.assertTrue(getAllMethods(MethodTestClazz.class).size() > 0);
        Assertions.assertNotNull(findMethod(MethodTestClazz.class, "getValue"));

        MethodTestClazz methodTestClazz = new MethodTestClazz();
        invokeMethod(methodTestClazz, "setValue", "Test");
        Assertions.assertEquals(methodTestClazz.getValue(), "Test");

        Assertions.assertTrue(overrides(MethodOverrideClazz.class.getMethod("get"),
                MethodTestClazz.class.getMethod("get")));
        Assertions.assertEquals(findNearestOverriddenMethod(MethodOverrideClazz.class.getMethod("get")),
                MethodTestClazz.class.getMethod("get"));
        Assertions.assertEquals(findOverriddenMethod(MethodOverrideClazz.class.getMethod("get"), MethodOverrideClazz.class),
                MethodTestClazz.class.getMethod("get"));

    }

    @Test
    public void testExtractFieldName() throws Exception {
        Method m1 = MethodFieldTestClazz.class.getMethod("is");
        Method m2 = MethodFieldTestClazz.class.getMethod("get");
        Method m3 = MethodFieldTestClazz.class.getMethod("getClass");
        Method m4 = MethodFieldTestClazz.class.getMethod("getObject");
        Method m5 = MethodFieldTestClazz.class.getMethod("getFieldName1");
        Method m6 = MethodFieldTestClazz.class.getMethod("setFieldName2");
        Method m7 = MethodFieldTestClazz.class.getMethod("isFieldName3");

        Assertions.assertEquals("", MethodUtils.extractFieldName(m1));
        Assertions.assertEquals("", MethodUtils.extractFieldName(m2));
        Assertions.assertEquals("", MethodUtils.extractFieldName(m3));
        Assertions.assertEquals("", MethodUtils.extractFieldName(m4));
        Assertions.assertEquals("fieldName1", MethodUtils.extractFieldName(m5));
        Assertions.assertEquals("fieldName2", MethodUtils.extractFieldName(m6));
        Assertions.assertEquals("fieldName3", MethodUtils.extractFieldName(m7));
    }

    public class MethodFieldTestClazz {
        public String is() {
            return "";
        }

        public String get() {
            return "";
        }

        public String getObject() {
            return "";
        }

        public String getFieldName1() {
            return "";
        }

        public String setFieldName2() {
            return "";
        }

        public String isFieldName3() {
            return "";
        }

    }

    public class MethodTestClazz {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public MethodTestClazz get() {
            return this;
        }

        @Deprecated
        public Boolean deprecatedMethod() {
            return true;
        }
    }

    public class MethodOverrideClazz extends MethodTestClazz {
        @Override
        public MethodTestClazz get() {
            return this;
        }
    }

}
