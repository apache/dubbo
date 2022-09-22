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

package org.apache.dubbo.errorcode.extractor;

import org.apache.dubbo.errorcode.util.FileUtils;

import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilities of Javassist.
 */
class JavassistUtils {
    private static final Map<Class, Field> stringFieldCache = new HashMap<>(2, 1);

    private static Method getItemMethodCache = null;

    private JavassistUtils() {
        throw new UnsupportedOperationException("No instance of JavassistUtils for you! ");
    }

    static ClassFile openClassFile(String classFilePath) {
        try {
            byte[] clsB = FileUtils.openFileAsByteArray(classFilePath);
            return new ClassFile(new DataInputStream(new ByteArrayInputStream(clsB)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static List<Object> getConstPoolItems(ConstPool cp) {
        List<Object> objects = new ArrayList<>(cp.getSize());

        for (int i = 0; i < cp.getSize(); i++) {
            objects.add(getConstPoolItem(cp, i));
        }

        return objects;
    }

    /**
     * Calls ConstPool.getItem() method reflectively.
     *
     * @param cp The ConstPool object.
     * @param index The index of items.
     * @return The XXXInfo Object. Since it's invisible, return Object instead.
     */
    static Object getConstPoolItem(ConstPool cp, int index) {

        if (getItemMethodCache == null) {
            Class<ConstPool> cpc = ConstPool.class;
            Method getItemMethod;
            try {
                getItemMethod = cpc.getDeclaredMethod("getItem", int.class);
                getItemMethod.setAccessible(true);

                getItemMethodCache = getItemMethod;

            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Javassist internal method changed.", e);
            }
        }

        try {
            return getItemMethodCache.invoke(cp, index);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Javassist internal method changed.", e);
        }
    }

    static List<String> getConstPoolStringItems(ConstPool cp) {
        List<Object> objects = getConstPoolItems(cp);
        List<String> stringItems = new ArrayList<>(cp.getSize());

        for (Object item : objects) {

            Field stringField;

            if (item != null) {
                stringField = getStringFieldInConstPoolItems(item);

                if (stringField == null) {
                    continue;
                }

                Object fieldData;

                try {
                    fieldData = stringField.get(item);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Javassist internal field changed.", e);
                }

                if (fieldData.getClass() == String.class) {
                    stringItems.add((String) fieldData);
                }
            }
        }

        return stringItems;
    }

    /**
     * Obtain the 'string' field in Utf8Info and StringInfo.
     *
     * @param item The instance of Utf8Info and StringInfo.
     * @return 'string' field's value
     */
    static Field getStringFieldInConstPoolItems(Object item) {
        if (stringFieldCache.containsKey(item.getClass())) {
            return stringFieldCache.get(item.getClass());
        } else {
            try {
                Field stringField = item.getClass().getDeclaredField("string");
                stringField.setAccessible(true);
                stringFieldCache.put(item.getClass(), stringField);

                return stringField;
            } catch (NoSuchFieldException ignored) {
            }
        }

        return null;
    }
}
