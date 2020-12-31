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


import java.lang.reflect.Method;

/**
 * @see org.apache.dubbo.common.utils.ClassUtils
 * @deprecated Replace to <code>ClassUtils</code>
 */
public class ClassHelper {
    public static Class<?> forNameWithThreadContextClassLoader(String name) throws ClassNotFoundException {
        return ClassUtils.forName(name, Thread.currentThread().getContextClassLoader());
    }

    public static Class<?> forNameWithCallerClassLoader(String name, Class<?> caller) throws ClassNotFoundException {
        return ClassUtils.forName(name, caller.getClassLoader());
    }

    public static ClassLoader getCallerClassLoader(Class<?> caller) {
        return caller.getClassLoader();
    }

    /**
     * get class loader
     *
     * @param clazz
     * @return class loader
     */
    public static ClassLoader getClassLoader(Class<?> clazz) {
        return ClassUtils.getClassLoader(clazz);
    }

    /**
     * Return the default ClassLoader to use: typically the thread context
     * ClassLoader, if available; the ClassLoader that loaded the ClassUtils
     * class will be used as fallback.
     * <p>
     * Call this method if you intend to use the thread context ClassLoader in a
     * scenario where you absolutely need a non-null ClassLoader reference: for
     * example, for class path resource loading (but not necessarily for
     * <code>Class.forName</code>, which accepts a <code>null</code> ClassLoader
     * reference as well).
     *
     * @return the default ClassLoader (never <code>null</code>)
     * @see java.lang.Thread#getContextClassLoader()
     */
    public static ClassLoader getClassLoader() {
        return getClassLoader(ClassHelper.class);
    }

    /**
     * Same as <code>Class.forName()</code>, except that it works for primitive
     * types.
     */
    public static Class<?> forName(String name) throws ClassNotFoundException {
        return forName(name, getClassLoader());
    }

    /**
     * Replacement for <code>Class.forName()</code> that also returns Class
     * instances for primitives (like "int") and array class names (like
     * "String[]").
     *
     * @param name        the name of the Class
     * @param classLoader the class loader to use (may be <code>null</code>,
     *                    which indicates the default class loader)
     * @return Class instance for the supplied name
     * @throws ClassNotFoundException if the class was not found
     * @throws LinkageError           if the class file could not be loaded
     * @see Class#forName(String, boolean, ClassLoader)
     */
    public static Class<?> forName(String name, ClassLoader classLoader)
            throws ClassNotFoundException, LinkageError {
        return ClassUtils.forName(name, classLoader);
    }

    /**
     * Resolve the given class name as primitive class, if appropriate,
     * according to the JVM's naming rules for primitive classes.
     * <p>
     * Also supports the JVM's internal class names for primitive arrays. Does
     * <i>not</i> support the "[]" suffix notation for primitive arrays; this is
     * only supported by {@link #forName}.
     *
     * @param name the name of the potentially primitive class
     * @return the primitive class, or <code>null</code> if the name does not
     * denote a primitive class or primitive array class
     */
    public static Class<?> resolvePrimitiveClassName(String name) {
        return ClassUtils.resolvePrimitiveClassName(name);
    }

    public static String toShortString(Object obj) {
        return ClassUtils.toShortString(obj);

    }

    public static String simpleClassName(Class<?> clazz) {
        return ClassUtils.simpleClassName(clazz);
    }

    /**
     * @see org.apache.dubbo.common.utils.MethodUtils#isSetter(Method)
     * @deprecated Replace to <code>MethodUtils#isSetter(Method)</code>
     */
    public static boolean isSetter(Method method) {
        return MethodUtils.isSetter(method);
    }

    /**
     * @see org.apache.dubbo.common.utils.MethodUtils#isGetter(Method) (Method)
     * @deprecated Replace to <code>MethodUtils#isGetter(Method)</code>
     */
    public static boolean isGetter(Method method) {
        return MethodUtils.isGetter(method);
    }

    public static boolean isPrimitive(Class<?> type) {
        return ClassUtils.isPrimitive(type);
    }

    public static Object convertPrimitive(Class<?> type, String value) {
        return ClassUtils.convertPrimitive(type,value);
    }


    /**
     * We only check boolean value at this moment.
     *
     * @param type
     * @param value
     * @return
     */
    public static boolean isTypeMatch(Class<?> type, String value) {
    return ClassUtils.isTypeMatch(type,value);
    }
}
