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

import org.apache.dubbo.rpc.model.FrameworkModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
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
                if (genericParameterType instanceof Class) {
                    checkClass(markedClass, (Class<?>) genericParameterType);
                }
            }

            Class<?> returnType = method.getReturnType();
            checkClass(markedClass, returnType);

            Type genericReturnType = method.getGenericReturnType();
            if (genericReturnType instanceof Class) {
                checkClass(markedClass, (Class<?>) genericReturnType);
            }

            Class<?>[] exceptionTypes = method.getExceptionTypes();
            for (Class<?> exceptionType : exceptionTypes) {
                checkClass(markedClass, exceptionType);
            }

            Type[] genericExceptionTypes = method.getGenericExceptionTypes();
            for (Type genericExceptionType : genericExceptionTypes) {
                if (genericExceptionType instanceof Class) {
                    checkClass(markedClass, (Class<?>) genericExceptionType);
                }
            }
        }
    }

    protected void checkClass(Set<Class<?>> markedClass, Class<?> clazz) {
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
            Class<?> fieldClass = field.getDeclaringClass();
            checkClass(markedClass, fieldClass);
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
                for (AllowClassNotifyListener listener : listeners) {
                    listener.notify(allowedPrefix);
                }
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
            for (AllowClassNotifyListener listener : listeners) {
                listener.notify(allowedPrefix);
            }
        }
    }

    protected Set<String> getAllowedPrefix() {
        return allowedPrefix;
    }
}
