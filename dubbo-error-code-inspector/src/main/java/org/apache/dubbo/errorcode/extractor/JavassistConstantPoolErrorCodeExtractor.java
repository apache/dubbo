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
import java.util.function.Predicate;
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

    @Override
    public List<MethodDefinition> getIllegalLoggerMethodInvocations(String classFilePath) {

        ClassFile cf = openClassFile(classFilePath);
        List<Object> cpi = getConstPoolItems(cf.getConstPool());

        List<Integer> interfaceMethodRefIndices = cpi.stream().filter(x -> {
            try {
                if (x == null) return false;
                return x.getClass() == Class.forName("javassist.bytecode.InterfaceMethodrefInfo");
            } catch (ClassNotFoundException e) {
                return false;
            }
        }).map(this::getIndexFieldInConstPoolItems).collect(Collectors.toList());

        List<MethodDefinition> methodDefinitions = new ArrayList<>();

        for (int index : interfaceMethodRefIndices) {
            ConstPool cp = cf.getConstPool();

            MethodDefinition methodDefinition = new MethodDefinition();
            methodDefinition.setClassName(
                cp.getInterfaceMethodrefClassName(index)
            );

            methodDefinition.setMethodName(
                cp.getUtf8Info(
                    cp.getNameAndTypeName(
                        cp.getInterfaceMethodrefNameAndType(index)
                    )
                )
            );

            methodDefinition.setArguments(
                cp.getUtf8Info(
                    cp.getNameAndTypeDescriptor(
                        cp.getInterfaceMethodrefNameAndType(index)
                    )
                )
            );

            methodDefinitions.add(methodDefinition);
        }

        Predicate<MethodDefinition> legacyLoggerClass = x -> x.getClassName().equals("org.apache.dubbo.common.logger.Logger");
        Predicate<MethodDefinition> errorTypeAwareLoggerClass = x -> x.getClassName().equals("org.apache.dubbo.common.logger.ErrorTypeAwareLogger");
        Predicate<MethodDefinition> loggerClass = legacyLoggerClass.or(errorTypeAwareLoggerClass);

        return methodDefinitions.stream()
            .filter(loggerClass)
            .filter(x -> x.getMethodName().equals("warn") || x.getMethodName().equals("error"))
            .filter(x -> x.getArguments().split(";").length < 4)
            .collect(Collectors.toList());
    }

    private int getIndexFieldInConstPoolItems(Object item) {
        // Searches in super classes recursively.
        Field indexField = getDeclaredFieldRecursively(item.getClass(), "index");

        try {
            return (int) indexField.get(item);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Field getDeclaredFieldRecursively(Class cls, String name) {
        try {
            // Searches in super classes recursively.
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
