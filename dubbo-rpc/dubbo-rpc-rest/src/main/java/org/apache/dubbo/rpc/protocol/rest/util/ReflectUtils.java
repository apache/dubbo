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
package org.apache.dubbo.rpc.protocol.rest.util;


import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ReflectUtils {
    private final static Class[] EMPTY_CLASS_ARRAY = new Class[0];
    private final static Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    public static Field getField(Class clazz, String field) throws IllegalAccessException {
        Field[] fields = clazz.getDeclaredFields();

        for (Field field1 : fields) {
            if (field1.getName().equals(field)) {
                return setModifiersUnFinal(field1);
            }
        }

        fields = clazz.getFields();

        for (Field field1 : fields) {
            if (field1.getName().equals(field)) {
                return setModifiersUnFinal(field1);
            }
        }

        return null;
    }


    public static Method getMethod(Class clazz, String method, Class[] paramTypes) throws NoSuchMethodException {
        Method declaredMethod = clazz.getDeclaredMethod(method, paramTypes);
        declaredMethod.setAccessible(true);
        return declaredMethod;
    }

    private static Field setModifiersUnFinal(Field field) throws IllegalAccessException {
        // public
//        MODIFIERS.set(field, 1);
        field.setAccessible(true);
        return field;
    }

    public static Class findClass(String name) throws ClassNotFoundException {

        return findClass(Thread.currentThread().getContextClassLoader(), name);

    }

    public static Class findClass(String name, ClassLoader classLoader) throws ClassNotFoundException {

        return classLoader.loadClass(name);

    }

    public static Class findClassAndTryCatch(String name, ClassLoader classLoader) {

        try {
            return findClass(name, classLoader);
        } catch (Throwable e) {

        }
        return null;

    }

    public static Class findClass(String... name) throws ClassNotFoundException {

        return findClass(Thread.currentThread().getContextClassLoader(), name);
    }


    public static Class findClass(ClassLoader classLoader, String... name) throws ClassNotFoundException {

        String[] names = name;

        Class tmp;
        for (String s : names) {
            tmp = findClassAndTryCatch(s, classLoader);
            if (tmp == null) {
                continue;
            } else {
                return tmp;
            }
        }
        throw new ClassNotFoundException();
    }


    public static Class findClassTryException(ClassLoader classLoader, String... name) {

        try {
            return findClass(classLoader, name);
        } catch (Exception e) {

        }
        return null;

    }

    public static Class findClassTryException(String... name) {
        return findClassTryException(Thread.currentThread().getContextClassLoader(), name);
    }


    public static Object getArrayElement(Object obj, int index) {
        return Array.get(obj, index);
    }

    public static List<Object> getArrayElements(Object obj) {

        List<Object> objects = new ArrayList<Object>();
        int length = Array.getLength(obj);

        if (length == 0) {
            return objects;
        }

        for (int i = 0; i < length; i++) {
            objects.add(Array.get(obj, i));
        }

        return objects;
    }


    public static Field getFieldAndTryCatch(Class clazz, String field) {

        try {
            return getField(clazz, field);
        } catch (Exception e) {

        }
        return null;
    }

    public static Method getMethod(Class clazz, String method) throws NoSuchMethodException {
        Method declaredMethod = clazz.getDeclaredMethod(method, new Class[]{});
        declaredMethod.setAccessible(true);
        return declaredMethod;
    }

    public static Method getMethodAndTry(Class clazz, String method) {
        Method declaredMethod = null;
        try {
            declaredMethod = clazz.getDeclaredMethod(method, EMPTY_CLASS_ARRAY);
            declaredMethod.setAccessible(true);
        } catch (Exception e) {

        }

        if (declaredMethod == null) {
            try {
                declaredMethod = getMethodByName(clazz, method);
            } catch (Exception e) {

            }
        }

        return declaredMethod;
    }

    public static Object invoke(Object object, Method method) {
        try {
            return method.invoke(object, EMPTY_OBJECT_ARRAY);
        } catch (Exception e) {
            return null;
        }
    }

    public static Object invoke(Object object, Method method, Object[] params) throws InvocationTargetException, IllegalAccessException {
        return method.invoke(object, params);

    }

    public static Object invokeAndTryCatch(Object object, Method method, Object[] params) {
        try {
            return invoke(object, method, params);
        } catch (Exception e) {

        }

        return null;
    }

    public static Class findClassAndTry(String name) {

        try {
            return findClass(name);
        } catch (Exception e) {
            return null;
        }

    }

    public static Method getMethodByName(Class clazz, String name) {
        Method[] declaredMethods = clazz.getMethods();

        for (Method declaredMethod : declaredMethods) {
            if (name.equals(declaredMethod.getName())) {

                return declaredMethod;
            }
        }

        return null;

    }


    public static Object getValueByFields(Object obj, Field... fields) {
        for (Field field : fields) {

            try {
                Object o = field.get(obj);
                if (o != null) {
                    return o;
                }
            } catch (Exception e) {

            }

        }

        return null;
    }

    public static Method getMethodAndTryCatch(Class clazz, String method, Class[] paramTypes) {
        try {
            return getMethod(clazz, method, paramTypes);
        } catch (Throwable e) {

        }
        return null;

    }


    public static Object getFieldValueAndTryCatch(Object obj, String field) {
        try {
            return ReflectUtils.getValueByFields(obj, ReflectUtils.getFieldAndTryCatch(obj.getClass(), field));
        } catch (Exception e) {
            return null;
        }
    }


}
