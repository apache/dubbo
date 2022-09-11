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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.SERIALIZE_ALLOW_LIST_FILE_PATH;

public class SerializeSecurityManager {
    private final Set<String> allowedPrefix = new LinkedHashSet<>();

    private final static Logger logger = LoggerFactory.getLogger(SerializeSecurityManager.class);

    private final SerializeClassChecker checker = SerializeClassChecker.getInstance();

    private final Set<AllowClassNotifyListener> listeners;

    private volatile SerializeCheckStatus checkStatus = AllowClassNotifyListener.DEFAULT_STATUS;

    public SerializeSecurityManager(FrameworkModel frameworkModel) {
        listeners = frameworkModel.getExtensionLoader(AllowClassNotifyListener.class).getSupportedExtensionInstances();

        try {
            Set<ClassLoader> classLoaders = frameworkModel.getClassLoaders();
            List<URL> urls = ClassLoaderResourceLoader.loadResources(SERIALIZE_ALLOW_LIST_FILE_PATH, classLoaders)
                .values()
                .stream()
                .flatMap(Set::stream)
                .collect(Collectors.toList());
            for (URL u : urls) {
                try {
                    String[] lines = IOUtils.readLines(u.openStream());
                    for (String line : lines) {
                        line = line.trim();
                        if (StringUtils.isEmpty(line) || line.startsWith("#")) {
                            continue;
                        }
                        allowedPrefix.add(line);
                    }
                } catch (IOException e) {
                    logger.error("Failed to load allow class list! Will ignore allow lis from " + u, e);
                }
            }
        } catch (InterruptedException e) {
            logger.error("Failed to load allow class list! Will ignore allow list from configuration.", e);
        }

        notifyListeners();
    }

    public void registerInterface(Class<?> clazz) {
        Set<Class<?>> markedClass = new HashSet<>();
        markedClass.add(clazz);

        addToAllow(clazz.getName());

        Method[] methodsToExport = clazz.getMethods();

        for (Method method : methodsToExport) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (Class<?> parameterType : parameterTypes) {
                checkClass(markedClass, parameterType);
            }

            Type[] genericParameterTypes = method.getGenericParameterTypes();
            for (Type genericParameterType : genericParameterTypes) {
                checkType(markedClass, genericParameterType);
            }

            Class<?> returnType = method.getReturnType();
            checkClass(markedClass, returnType);

            Type genericReturnType = method.getGenericReturnType();
            checkType(markedClass, genericReturnType);

            Class<?>[] exceptionTypes = method.getExceptionTypes();
            for (Class<?> exceptionType : exceptionTypes) {
                checkClass(markedClass, exceptionType);
            }

            Type[] genericExceptionTypes = method.getGenericExceptionTypes();
            for (Type genericExceptionType : genericExceptionTypes) {
                checkType(markedClass, genericExceptionType);
            }
        }
    }

    private void checkType(Set<Class<?>> markedClass, Type type) {
        if (type instanceof Class) {
            checkClass(markedClass, (Class<?>) type);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            checkClass(markedClass, (Class<?>) parameterizedType.getRawType());
            for (Type actualTypeArgument : parameterizedType.getActualTypeArguments()) {
                checkType(markedClass, actualTypeArgument);
            }
        } else if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            checkType(markedClass, genericArrayType.getGenericComponentType());
        } else if (type instanceof TypeVariable) {
            TypeVariable typeVariable = (TypeVariable) type;
            for (Type bound : typeVariable.getBounds()) {
                checkType(markedClass, bound);
            }
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            for (Type bound : wildcardType.getUpperBounds()) {
                checkType(markedClass, bound);
            }
            for (Type bound : wildcardType.getLowerBounds()) {
                checkType(markedClass, bound);
            }
        }
    }

    private void checkClass(Set<Class<?>> markedClass, Class<?> clazz) {
        if (markedClass.contains(clazz)) {
            return;
        }

        markedClass.add(clazz);

        addToAllow(clazz.getName());

        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> interfaceClass : interfaces) {
            checkClass(markedClass, interfaceClass);
        }

        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            checkClass(markedClass, superclass);
        }

        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            if (Modifier.isTransient(field.getModifiers())) {
                continue;
            }

            Class<?> fieldClass = field.getType();
            checkClass(markedClass, fieldClass);
            checkType(markedClass, field.getGenericType());
        }
    }

    protected void addToAllow(String className) {
        if (!checker.validateClass(className, false)) {
            return;
        }

        boolean modified;

        // ignore jdk
        if (className.startsWith("java.") || className.startsWith("javax.") || className.startsWith("com.sun.") ||
            className.startsWith("sun.") || className.startsWith("jdk.")) {
            modified = allowedPrefix.add(className);
            if (modified) {
                notifyListeners();
            }
            return;
        }

        // add group package
        String[] subs = className.split("\\.");
        if (subs.length > 3) {
            modified = allowedPrefix.add(subs[0] + "." + subs[1] + "." + subs[2]);
        } else {
            modified = allowedPrefix.add(className);
        }

        if (modified) {
            notifyListeners();
        }
    }

    private void notifyListeners() {
        for (AllowClassNotifyListener listener : listeners) {
            listener.notify(checkStatus, allowedPrefix);
        }
    }

    protected Set<String> getAllowedPrefix() {
        return allowedPrefix;
    }
}
