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

import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Error code extractor based on constant pool extracting with Javassist.
 */
public class JavassistConstantPoolErrorCodeExtractor implements ErrorCodeExtractor {

    private static final Map<Class, Field> stringFieldCache = new HashMap<>(2, 1);

    private static Method getItemMethodCache = null;

    @Override
    public List<String> getErrorCodes(String classFilePath) {

        ClassFile clsF = openClassFile(classFilePath);
        ConstPool cp = clsF.getConstPool();

        List<String> cpItems = getConstPoolStringItems(cp);

        return cpItems.stream().filter(x -> ERROR_CODE_PATTERN.matcher(x).matches()).collect(Collectors.toList());
    }

    private ClassFile openClassFile(String classFilePath) {
        try (FileChannel fileChannel = FileChannel.open(Paths.get(classFilePath))) {

            ByteBuffer byteBuffer = ByteBuffer.allocate((int) fileChannel.size());
            fileChannel.read(byteBuffer);

            byte[] clsB = byteBuffer.array();

            return new ClassFile(new DataInputStream(new ByteArrayInputStream(clsB)));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Object> getConstPoolItems(ConstPool cp) {
        List<Object> objects = new ArrayList<>(cp.getSize());

        for (int i = 0; i < cp.getSize(); i++) {
            objects.add(getItem(cp, i));
        }

        return objects;
    }

    private List<String> getConstPoolStringItems(ConstPool cp) {
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

    private static Field getStringFieldInConstPoolItems(Object item) {
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

    /**
     * Calls ConstPool.getItem() method reflectively.
     *
     * @param cp The ConstPool object.
     * @param index The index of items.
     * @return The XXXInfo Object. Since it's invisible, return Object instead.
     */
    private Object getItem(ConstPool cp, int index) {

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
}
