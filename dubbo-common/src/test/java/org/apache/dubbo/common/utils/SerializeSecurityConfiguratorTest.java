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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.List;

import org.junit.jupiter.api.Test;

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
        Class<?> clazz = SerializeSecurityConfigurator.class;
        assertTrue(SerializeSecurityConfigurator.safeCheckClass(clazz));
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
                return new Type[] {String.class};
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

    @Test
    void testSafeCheckClass_WithInterfaces() {
        // String.class has interfaces CharSequence, Comparable, Constable
        assertTrue(SerializeSecurityConfigurator.safeCheckClass(String.class));
    }

    @Test
    void testSafeCheckClass_WithGenericSuperclass() {
        // Integer.class has superclass Number
        assertTrue(SerializeSecurityConfigurator.safeCheckClass(Integer.class));
    }

    @Test
    void testSafeCheckClass_WithGenericParameterizedType() {
        // ArrayList<String> has ParameterizedType generic superclass
        Type arrayListString = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] {String.class};
            }

            @Override
            public Type getRawType() {
                return ArrayList.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };
        SerializeSecurityConfigurator.safeCheckType(arrayListString);
    }

    @Test
    void testSafeCheckType_GenericArrayType() {
        Type stringArrayType = new GenericArrayType() {
            @Override
            public Type getComponentType() {
                return String.class;
            }
        };
        SerializeSecurityConfigurator.safeCheckType(stringArrayType);
    }

    @Test
    void testSafeCheckType_TypeVariable() {
        Type typeVariable = new TypeVariable() {
            @Override
            public String getName() {
                return "T";
            }

            @Override
            public Type[] getBounds() {
                return new Type[]{Object.class};
            }

            @Override
            public Type getGenericDeclaration() {
                return null;
            }

            @Override
            public Type[] getUpperBounds() {
                return new Type[]{Object.class};
            }

            @Override
            public Type[] getLowerBounds() {
                return new Type[]{};
            }
        };
        SerializeSecurityConfigurator.safeCheckType(typeVariable);
    }

    @Test
    void testSafeCheckType_WildcardType() {
        Type wildcardType = new WildcardType() {
            @Override
            public Type[] getUpperBounds() {
                return new Type[]{Object.class};
            }

            @Override
            public Type[] getLowerBounds() {
                return new Type[]{String.class};
            }
        };
        SerializeSecurityConfigurator.safeCheckType(wildcardType);
    }

    @Test
    void testSafeCheckClass_ClassWithFields() {
        // Test class with fields to cover field.getType() and field.getGenericType()
        assertTrue(SerializeSecurityConfigurator.safeCheckClass(TestFieldClass.class));
    }

    public static class TestFieldClass {
        public String name;
        public int value;
        public List<String> items;
        public Map<String, Integer> map;
    }

    @Test
    void testSafeCheckType_ParameterizedTypeWithOwner() {
        // Test ParameterizedType with owner type
        Type mapEntryType = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{String.class, Integer.class};
            }

            @Override
            public Type getRawType() {
                return Map.Entry.class;
            }

            @Override
            public Type getOwnerType() {
                return Map.class;
            }
        };
        SerializeSecurityConfigurator.safeCheckType(mapEntryType);
    }
}
