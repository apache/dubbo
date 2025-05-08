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

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.Descriptor;
import javassist.bytecode.LocalVariableAttribute;

import static org.apache.dubbo.common.logger.LoggerFactory.getErrorTypeAwareLogger;

@Activate(order = 100, onClass = "javassist.ClassPool")
public class JavassistParameterNameReader implements ParameterNameReader {

    private static final ErrorTypeAwareLogger LOG = getErrorTypeAwareLogger(JavassistParameterNameReader.class);

    private final Map<Integer, ClassPool> classPoolMap = CollectionUtils.newConcurrentHashMap();

    @Override
    public String[] readParameterNames(Method method) {
        try {
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length == 0) {
                return StringUtils.EMPTY_STRING_ARRAY;
            }
            String descriptor = getDescriptor(paramTypes, method.getReturnType());
            Class<?> clazz = method.getDeclaringClass();
            CtMethod ctMethod = getClassPool(clazz).get(clazz.getName()).getMethod(method.getName(), descriptor);
            return read(ctMethod, Modifier.isStatic(method.getModifiers()) ? 0 : 1, paramTypes.length);
        } catch (Throwable t) {
            LOG.warn(LoggerCodeConstants.INTERNAL_ERROR, "", "", "Read parameter names error", t);
            return null;
        }
    }

    @Override
    public String[] readParameterNames(Constructor<?> ctor) {
        try {
            Class<?>[] paramTypes = ctor.getParameterTypes();
            if (paramTypes.length == 0) {
                return StringUtils.EMPTY_STRING_ARRAY;
            }
            String descriptor = getDescriptor(paramTypes, void.class);
            Class<?> clazz = ctor.getDeclaringClass();
            CtConstructor ctCtor = getClassPool(clazz).get(clazz.getName()).getConstructor(descriptor);
            return read(ctCtor, 1, paramTypes.length);
        } catch (Throwable t) {
            LOG.warn(LoggerCodeConstants.INTERNAL_ERROR, "", "", "Read parameter names error", t);
            return null;
        }
    }

    private static String getDescriptor(Class<?>[] parameterTypes, Class<?> returnType) {
        StringBuilder descriptor = new StringBuilder(32);
        descriptor.append('(');
        for (Class<?> type : parameterTypes) {
            descriptor.append(toJvmName(type));
        }
        descriptor.append(')');
        descriptor.append(toJvmName(returnType));
        return descriptor.toString();
    }

    private static String toJvmName(Class<?> clazz) {
        return clazz.isArray() ? Descriptor.toJvmName(clazz.getName()) : Descriptor.of(clazz.getName());
    }

    private ClassPool getClassPool(Class<?> clazz) {
        ClassLoader classLoader = ClassUtils.getClassLoader(clazz);
        return classPoolMap.computeIfAbsent(System.identityHashCode(classLoader), k -> {
            ClassPool pool = new ClassPool();
            pool.appendClassPath(new LoaderClassPath(classLoader));
            return pool;
        });
    }

    private static String[] read(CtBehavior behavior, int start, int len) {
        if (behavior == null) {
            return null;
        }
        CodeAttribute codeAttr = behavior.getMethodInfo().getCodeAttribute();
        if (codeAttr == null) {
            return null;
        }
        LocalVariableAttribute attr = (LocalVariableAttribute) codeAttr.getAttribute(LocalVariableAttribute.tag);
        if (attr == null) {
            return null;
        }
        String[] names = new String[len];
        for (int i = 0, tLen = attr.tableLength(); i < tLen; i++) {
            int j = attr.index(i) - start;
            if (j >= 0 && j < len) {
                names[j] = attr.variableName(i);
            }
        }

        return names;
    }
}
