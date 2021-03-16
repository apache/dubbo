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

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.apache.dubbo.common.function.Streams.filterAll;
import static org.apache.dubbo.common.utils.ClassUtils.getAllInheritedTypes;
import static org.apache.dubbo.common.utils.MemberUtils.isPrivate;
import static org.apache.dubbo.common.utils.MemberUtils.isStatic;
import static org.apache.dubbo.common.utils.ReflectUtils.EMPTY_CLASS_ARRAY;
import static org.apache.dubbo.common.utils.ReflectUtils.resolveTypes;
import static org.apache.dubbo.common.utils.StringUtils.isNotEmpty;

/**
 * Miscellaneous method utility methods.
 * Mainly for internal use within the framework.
 *
 * @since 2.7.2
 */
public interface MethodUtils {

    /**
     * Return {@code true} if the provided method is a set method.
     * Otherwise, return {@code false}.
     *
     * @param method the method to check
     * @return whether the given method is setter method
     */
    public static boolean isSetter(Method method) {
        return method.getName().startsWith("set")
                && !"set".equals(method.getName())
                && Modifier.isPublic(method.getModifiers())
                && method.getParameterCount() == 1
                && ClassUtils.isPrimitive(method.getParameterTypes()[0]);
    }

    /**
     * Return {@code true} if the provided method is a get method.
     * Otherwise, return {@code false}.
     *
     * @param method the method to check
     * @return whether the given method is getter method
     */
    public static boolean isGetter(Method method) {
        String name = method.getName();
        return (name.startsWith("get") || name.startsWith("is"))
                && !"get".equals(name) && !"is".equals(name)
                && !"getClass".equals(name) && !"getObject".equals(name)
                && Modifier.isPublic(method.getModifiers())
                && method.getParameterTypes().length == 0
                && ClassUtils.isPrimitive(method.getReturnType());
    }

    /**
     * Return {@code true} If this method is a meta method.
     * Otherwise, return {@code false}.
     *
     * @param method the method to check
     * @return whether the given method is meta method
     */
    public static boolean isMetaMethod(Method method) {
        String name = method.getName();
        if (!(name.startsWith("get") || name.startsWith("is"))) {
            return false;
        }
        if ("get".equals(name)) {
            return false;
        }
        if ("getClass".equals(name)) {
            return false;
        }
        if (!Modifier.isPublic(method.getModifiers())) {
            return false;
        }
        if (method.getParameterTypes().length != 0) {
            return false;
        }
        if (!ClassUtils.isPrimitive(method.getReturnType())) {
            return false;
        }
        return true;
    }

    /**
     * Check if the method is a deprecated method. The standard is whether the {@link java.lang.Deprecated} annotation is declared on the class.
     * Return {@code true} if this annotation is present.
     * Otherwise, return {@code false}.
     *
     * @param method the method to check
     * @return whether the given method is deprecated method
     */
    public static boolean isDeprecated(Method method) {
        return method.getAnnotation(Deprecated.class) != null;
    }


    /**
     * Create an instance of {@link Predicate} for {@link Method} to exclude the specified declared class
     *
     * @param declaredClass the declared class to exclude
     * @return non-null
     * @since 2.7.6
     */
    static Predicate<Method> excludedDeclaredClass(Class<?> declaredClass) {
        return method -> !Objects.equals(declaredClass, method.getDeclaringClass());
    }

    /**
     * Get all {@link Method methods} of the declared class
     *
     * @param declaringClass        the declared class
     * @param includeInheritedTypes include the inherited types, e,g. super classes or interfaces
     * @param publicOnly            only public method
     * @param methodsToFilter       (optional) the methods to be filtered
     * @return non-null read-only {@link List}
     * @since 2.7.6
     */
    static List<Method> getMethods(Class<?> declaringClass, boolean includeInheritedTypes, boolean publicOnly,
                                   Predicate<Method>... methodsToFilter) {

        if (declaringClass == null || declaringClass.isPrimitive()) {
            return emptyList();
        }

        // All declared classes
        List<Class<?>> declaredClasses = new LinkedList<>();
        // Add the top declaring class
        declaredClasses.add(declaringClass);
        // If the super classes are resolved, all them into declaredClasses
        if (includeInheritedTypes) {
            declaredClasses.addAll(getAllInheritedTypes(declaringClass));
        }

        // All methods
        List<Method> allMethods = new LinkedList<>();

        for (Class<?> classToSearch : declaredClasses) {
            Method[] methods = publicOnly ? classToSearch.getMethods() : classToSearch.getDeclaredMethods();
            // Add the declared methods or public methods
            for (Method method : methods) {
                allMethods.add(method);
            }
        }

        return unmodifiableList(filterAll(allMethods, methodsToFilter));
    }

    /**
     * Get all declared {@link Method methods} of the declared class, excluding the inherited methods
     *
     * @param declaringClass  the declared class
     * @param methodsToFilter (optional) the methods to be filtered
     * @return non-null read-only {@link List}
     * @see #getMethods(Class, boolean, boolean, Predicate[])
     * @since 2.7.6
     */
    static List<Method> getDeclaredMethods(Class<?> declaringClass, Predicate<Method>... methodsToFilter) {
        return getMethods(declaringClass, false, false, methodsToFilter);
    }

    /**
     * Get all public {@link Method methods} of the declared class, including the inherited methods.
     *
     * @param declaringClass  the declared class
     * @param methodsToFilter (optional) the methods to be filtered
     * @return non-null read-only {@link List}
     * @see #getMethods(Class, boolean, boolean, Predicate[])
     * @since 2.7.6
     */
    static List<Method> getMethods(Class<?> declaringClass, Predicate<Method>... methodsToFilter) {
        return getMethods(declaringClass, false, true, methodsToFilter);
    }

    /**
     * Get all declared {@link Method methods} of the declared class, including the inherited methods.
     *
     * @param declaringClass  the declared class
     * @param methodsToFilter (optional) the methods to be filtered
     * @return non-null read-only {@link List}
     * @see #getMethods(Class, boolean, boolean, Predicate[])
     * @since 2.7.6
     */
    static List<Method> getAllDeclaredMethods(Class<?> declaringClass, Predicate<Method>... methodsToFilter) {
        return getMethods(declaringClass, true, false, methodsToFilter);
    }

    /**
     * Get all public {@link Method methods} of the declared class, including the inherited methods.
     *
     * @param declaringClass  the declared class
     * @param methodsToFilter (optional) the methods to be filtered
     * @return non-null read-only {@link List}
     * @see #getMethods(Class, boolean, boolean, Predicate[])
     * @since 2.7.6
     */
    static List<Method> getAllMethods(Class<?> declaringClass, Predicate<Method>... methodsToFilter) {
        return getMethods(declaringClass, true, true, methodsToFilter);
    }

//    static List<Method> getOverriderMethods(Class<?> implementationClass, Class<?>... superTypes) {

//

//    }

    /**
     * Find the {@link Method} by the the specified type and method name without the parameter types
     *
     * @param type       the target type
     * @param methodName the specified method name
     * @return if not found, return <code>null</code>
     * @since 2.7.6
     */
    static Method findMethod(Class type, String methodName) {
        return findMethod(type, methodName, EMPTY_CLASS_ARRAY);
    }

    /**
     * Find the {@link Method} by the the specified type, method name and parameter types
     *
     * @param type           the target type
     * @param methodName     the method name
     * @param parameterTypes the parameter types
     * @return if not found, return <code>null</code>
     * @since 2.7.6
     */
    static Method findMethod(Class type, String methodName, Class<?>... parameterTypes) {
        Method method = null;
        try {
            if (type != null && isNotEmpty(methodName)) {
                method = type.getDeclaredMethod(methodName, parameterTypes);
            }
        } catch (NoSuchMethodException e) {
        }
        return method;
    }

    /**
     * Invoke the target object and method
     *
     * @param object           the target object
     * @param methodName       the method name
     * @param methodParameters the method parameters
     * @param <T>              the return type
     * @return the target method's execution result
     * @since 2.7.6
     */
    static <T> T invokeMethod(Object object, String methodName, Object... methodParameters) {
        Class type = object.getClass();
        Class[] parameterTypes = resolveTypes(methodParameters);
        Method method = findMethod(type, methodName, parameterTypes);
        T value = null;

        try {
            ReflectUtils.makeAccessible(method);
            value = (T) method.invoke(object, methodParameters);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        return value;
    }


    /**
     * Tests whether one method, as a member of a given type,
     * overrides another method.
     *
     * @param overrider  the first method, possible overrider
     * @param overridden the second method, possibly being overridden
     * @return {@code true} if and only if the first method overrides
     * the second
     * @jls 8.4.8 Inheritance, Overriding, and Hiding
     * @jls 9.4.1 Inheritance and Overriding
     * @see Elements#overrides(ExecutableElement, ExecutableElement, TypeElement)
     */
    static boolean overrides(Method overrider, Method overridden) {

        if (overrider == null || overridden == null) {
            return false;
        }

        // equality comparison: If two methods are same
        if (Objects.equals(overrider, overridden)) {
            return false;
        }

        // Modifiers comparison: Any method must be non-static method
        if (isStatic(overrider) || isStatic(overridden)) { //
            return false;
        }

        // Modifiers comparison: the accessibility of any method must not be private
        if (isPrivate(overrider) || isPrivate(overridden)) {
            return false;
        }

        // Inheritance comparison: The declaring class of overrider must be inherit from the overridden's
        if (!overridden.getDeclaringClass().isAssignableFrom(overrider.getDeclaringClass())) {
            return false;
        }

        // Method comparison: must not be "default" method
        if (overrider.isDefault()) {
            return false;
        }

        // Method comparison: The method name must be equal
        if (!Objects.equals(overrider.getName(), overridden.getName())) {
            return false;
        }

        // Method comparison: The count of method parameters must be equal
        if (!Objects.equals(overrider.getParameterCount(), overridden.getParameterCount())) {
            return false;
        }

        // Method comparison: Any parameter type of overrider must equal the overridden's
        for (int i = 0; i < overrider.getParameterCount(); i++) {
            if (!Objects.equals(overridden.getParameterTypes()[i], overrider.getParameterTypes()[i])) {
                return false;
            }
        }

        // Method comparison: The return type of overrider must be inherit from the overridden's
        if (!overridden.getReturnType().isAssignableFrom(overrider.getReturnType())) {
            return false;
        }

        // Throwable comparison: "throws" Throwable list will be ignored, trust the compiler verify

        return true;
    }

    /**
     * Find the nearest overridden {@link Method method} from the inherited class
     *
     * @param overrider the overrider {@link Method method}
     * @return if found, the overrider <code>method</code>, or <code>null</code>
     */
    static Method findNearestOverriddenMethod(Method overrider) {
        Class<?> declaringClass = overrider.getDeclaringClass();
        Method overriddenMethod = null;
        for (Class<?> inheritedType : getAllInheritedTypes(declaringClass)) {
            overriddenMethod = findOverriddenMethod(overrider, inheritedType);
            if (overriddenMethod != null) {
                break;
            }
        }
        return overriddenMethod;
    }

    /**
     * Find the overridden {@link Method method} from the declaring class
     *
     * @param overrider      the overrider {@link Method method}
     * @param declaringClass the class that is declaring the overridden {@link Method method}
     * @return if found, the overrider <code>method</code>, or <code>null</code>
     */
    static Method findOverriddenMethod(Method overrider, Class<?> declaringClass) {
        List<Method> matchedMethods = getAllMethods(declaringClass, method -> overrides(overrider, method));
        return matchedMethods.isEmpty() ? null : matchedMethods.get(0);
    }
}
