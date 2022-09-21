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

package org.apache.dubbo.errorcode.util;

import java.lang.reflect.Field;

/**
 * Tools of reflective operations.
 */
public final class ReflectUtils {
    private ReflectUtils() {
        throw new UnsupportedOperationException("No instance of ReflectUtils for you! ");
    }

    /**
     * Searches (a private) field in super classes recursively.
     *
     * @param cls the actual type
     * @param name the field name
     * @return the corresponding Field object, or null if the field does not exist.
     */
    public static Field getDeclaredFieldRecursively(Class cls, String name) {
        try {
            Field indexField = cls.getDeclaredField(name);
            indexField.setAccessible(true);

            return indexField;
        } catch (NoSuchFieldException e) {
            if (cls == Object.class) {
                return null;
            }

            return getDeclaredFieldRecursively(cls.getSuperclass(), name);
        }
    }
}
