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


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ReflectUtils {

    public static Class findClass(String name, ClassLoader classLoader) throws ClassNotFoundException {

        return classLoader.loadClass(name);

    }

    public static Class findClass(String name) throws ClassNotFoundException {

        return findClass(Thread.currentThread().getContextClassLoader(), name);

    }

    public static Class findClassAndTryCatch(String name, ClassLoader classLoader) {

        try {
            return findClass(name, classLoader);
        } catch (Throwable e) {

        }
        return null;

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

    public static List<Method> getMethodByNameList(Class clazz, String name) {
        List<Method> methods = new ArrayList<>();

        try {
            Method[] declaredMethods = clazz.getDeclaredMethods();

            for (Method declaredMethod : declaredMethods) {
                if (name.equals(declaredMethod.getName())) {
                    declaredMethod.setAccessible(true);
                    methods.add(declaredMethod);
                }
            }

            for (Method declaredMethod : clazz.getMethods()) {
                if (name.equals(declaredMethod.getName())) {
                    declaredMethod.setAccessible(true);
                    methods.add(declaredMethod);
                }
            }
        } catch (Exception e) {

        }

        return methods;


    }

    public static Method getMethodByName(Class clazz, String name) {

        List<Method> methodByNameList = getMethodByNameList(clazz, name);
        if (methodByNameList.isEmpty()) {
            return null;
        } else {
            return methodByNameList.get(0);
        }
    }

    public static Class findClassTryException(String... name) {
        return findClassTryException(Thread.currentThread().getContextClassLoader(), name);
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


}
