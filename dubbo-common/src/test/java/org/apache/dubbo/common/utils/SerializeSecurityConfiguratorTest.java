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

import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for {@link SerializeSecurityConfigurator} to verify serialization
 * security configuration handles missing types gracefully.
 */
class SerializeSecurityConfiguratorTest {

    @Test
    void testSafeCheckClass_Null() {
        assertFalse(SerializeSecurityConfigurator.safeCheckClass(null));
    }

    @Test
    void testSafeCheckClass_NullType() {
        SerializeSecurityConfigurator.safeCheckType(null);
    }

    @Test
    void testSafeCheckClass_String() {
        assertTrue(SerializeSecurityConfigurator.safeCheckClass(String.class));
    }

    @Test
    void testSafeCheckClass_Integer() {
        assertTrue(SerializeSecurityConfigurator.safeCheckClass(Integer.class));
    }

    @Test
    void testSafeCheckClass_OtherPackage() {
        assertTrue(SerializeSecurityConfigurator.safeCheckClass(SerializeSecurityConfigurator.class));
    }

    @Test
    void testSafeCheckType_StringClass() {
        SerializeSecurityConfigurator.safeCheckType(String.class);
    }

    @Test
    void testSafeCheckType_StringParameterizedType() {
        Type stringListType = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{String.class};
            }

            @Override
            public Type getRawType() {
                return List.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };
        SerializeSecurityConfigurator.safeCheckType(stringListType);
    }

    @Test
    void testSafeCheckType_UnknownType() {
        // Should not throw when type cannot be resolved
        SerializeSecurityConfigurator.safeCheckType((Type) null);
    }
}
