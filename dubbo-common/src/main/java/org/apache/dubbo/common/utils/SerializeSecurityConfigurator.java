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

import org.apache.dubbo.common.aot.NativeDetector;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialization.ClassHolder;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeClassLoaderListener;

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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.SERIALIZE_ALLOW_LIST_FILE_PATH;
import static org.apache.dubbo.common.constants.CommonConstants.SERIALIZE_BLOCKED_LIST_FILE_PATH;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_IO_EXCEPTION;

public class SerializeSecurityConfigurator implements ScopeClassLoaderListener<ModuleModel> {
    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(SerializeSecurityConfigurator.class);

    private final Set<Type> markedTypeCache = new HashSet<>();

    private final SerializeSecurityManager serializeSecurityManager;

    private final ModuleModel moduleModel;

    private final ClassHolder classHolder;

    private volatile boolean autoTrustSerializeClass = true;

    private volatile int trustSerializeClassLevel = Integer.MAX_VALUE;

    public SerializeSecurityConfigurator(ModuleModel moduleModel) {
        this.moduleModel = moduleModel;
        moduleModel.addClassLoaderListener(this);

        FrameworkModel frameworkModel = moduleModel.getApplicationModel().getFrameworkModel();
        serializeSecurityManager = frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class);
        classHolder =
                NativeDetector.inNativeImage() ? frameworkModel.getBeanFactory().getBean(ClassHolder.class) : null;

        refreshStatus();
        refreshCheck();
        refreshConfig();

        onAddClassLoader(moduleModel, Thread.currentThread().getContextClassLoader());
    }

    public void refreshCheck() {
        Optional<ApplicationConfig> applicationConfig =
                moduleModel.getApplicationModel().getApplicationConfigManager().getApplication();
        autoTrustSerializeClass = applicationConfig
                .map(ApplicationConfig::getAutoTrustSerializeClass)
                .orElse(true);
        trustSerializeClassLevel = applicationConfig
                .map(ApplicationConfig::getTrustSerializeClassLevel)
                .orElse(Integer.MAX_VALUE);
        serializeSecurityManager.setCheckSerializable(
                applicationConfig.map(ApplicationConfig::getCheckSerializable).orElse(true));
    }

    @Override
    public void onAddClassLoader(ModuleModel scopeModel, ClassLoader classLoader) {
        refreshClassLoader(classLoader);
    }

    @Override
    public void onRemoveClassLoader(ModuleModel scopeModel, ClassLoader classLoader) {
        // ignore
    }

    private void refreshClassLoader(ClassLoader classLoader) {
        loadAllow(classLoader);
        loadBlocked(classLoader);
    }

    private void refreshConfig() {
        String allowedClassList = SystemPropertyConfigUtils.getSystemProperty(
                        CommonConstants.DubboProperty.DUBBO_CLASS_DESERIALIZE_ALLOWED_LIST, "")
                .trim();
        String blockedClassList = SystemPropertyConfigUtils.getSystemProperty(
                        CommonConstants.DubboProperty.DUBBO_CLASS_DESERIALIZE_BLOCKED_LIST, "")
                .trim();

        if (StringUtils.isNotEmpty(allowedClassList)) {
            String[] classStrings = allowedClassList.trim().split(",");
            for (String className : classStrings) {
                className = className.trim();
                if (StringUtils.isNotEmpty(className)) {
                    serializeSecurityManager.addToAlwaysAllowed(className);
                }
            }
        }

        if (StringUtils.isNotEmpty(blockedClassList)) {
            String[] classStrings = blockedClassList.trim().split(",");
            for (String className : classStrings) {
                className = className.trim();
                if (StringUtils.isNotEmpty(className)) {
                    serializeSecurityManager.addToDisAllowed(className);
                }
            }
        }
    }

    private void loadAllow(ClassLoader classLoader) {
        Set<URL> urls = ClassLoaderResourceLoader.loadResources(SERIALIZE_ALLOW_LIST_FILE_PATH, classLoader);
        for (URL u : urls) {
            try {
                LOGGER.info("Read serialize allow list from " + u);
                String[] lines = IOUtils.readLines(u.openStream());
                for (String line : lines) {
                    line = line.trim();
                    if (StringUtils.isEmpty(line) || line.startsWith("#")) {
                        continue;
                    }
                    serializeSecurityManager.addToAlwaysAllowed(line);
                }
            } catch (IOException e) {
                LOGGER.error(
                        COMMON_IO_EXCEPTION,
                        "",
                        "",
                        "Failed to load allow class list! Will ignore allow lis from " + u,
                        e);
            }
        }
    }

    private void loadBlocked(ClassLoader classLoader) {
        Set<URL> urls = ClassLoaderResourceLoader.loadResources(SERIALIZE_BLOCKED_LIST_FILE_PATH, classLoader);
        for (URL u : urls) {
            try {
                LOGGER.info("Read serialize blocked list from " + u);
                String[] lines = IOUtils.readLines(u.openStream());
                for (String line : lines) {
                    line = line.trim();
                    if (StringUtils.isEmpty(line) || line.startsWith("#")) {
                        continue;
                    }
                    serializeSecurityManager.addToDisAllowed(line);
                }
            } catch (IOException e) {
                LOGGER.error(
                        COMMON_IO_EXCEPTION,
                        "",
                        "",
                        "Failed to load blocked class list! Will ignore blocked lis from " + u,
                        e);
            }
        }
    }

    public void refreshStatus() {
        Optional<ApplicationConfig> application =
                moduleModel.getApplicationModel().getApplicationConfigManager().getApplication();
        String statusString =
                application.map(ApplicationConfig::getSerializeCheckStatus).orElse(null);
        SerializeCheckStatus checkStatus = null;

        if (StringUtils.isEmpty(statusString)) {
            String openCheckClass = SystemPropertyConfigUtils.getSystemProperty(
                    CommonConstants.DubboProperty.DUBBO_CLASS_DESERIALIZE_OPEN_CHECK, "true");
            if (!Boolean.parseBoolean(openCheckClass)) {
                checkStatus = SerializeCheckStatus.DISABLE;
            }
            String blockAllClassExceptAllow = SystemPropertyConfigUtils.getSystemProperty(
                    CommonConstants.DubboProperty.DUBBO_CLASS_DESERIALIZE_BLOCK_ALL, "false");
            if (Boolean.parseBoolean(blockAllClassExceptAllow)) {
                checkStatus = SerializeCheckStatus.STRICT;
            }
        } else {
            checkStatus = SerializeCheckStatus.valueOf(statusString);
        }

        if (checkStatus != null) {
            serializeSecurityManager.setCheckStatus(checkStatus);
        }
    }

    public synchronized void registerInterface(Class<?> clazz) {
        if (!autoTrustSerializeClass) {
            return;
        }

        if (!safeCheckClass(clazz)) {
            return;
        }

        addToAllow(clazz);

        Method[] methodsToExport;
        try {
            methodsToExport = clazz.getMethods();
        } catch (Throwable t) {
            LOGGER.warn(
                    COMMON_IO_EXCEPTION,
                    "Failed to get methods of class " + clazz.getName() + ", skipping method scanning",
                    t);
            return;
        }

        for (Method method : methodsToExport) {
            Class<?>[] parameterTypes;
            try {
                parameterTypes = method.getParameterTypes();
            } catch (Throwable t) {
                LOGGER.warn(
                        COMMON_IO_EXCEPTION,
                        "Failed to get parameter types of method " + method.getName()
                                + " in class " + clazz.getName() + ", skipping method",
                        t);
                continue;
            }
            for (Class<?> parameterType : parameterTypes) {
                safeCheckClass(parameterType);
            }

            Type[] genericParameterTypes;
            try {
                genericParameterTypes = method.getGenericParameterTypes();
            } catch (Throwable t) {
                LOGGER.warn(
                        COMMON_IO_EXCEPTION,
                        "Failed to get generic parameter types of method " + method.getName()
                                + " in class " + clazz.getName() + ", skipping method",
                        t);
                continue;
            }
            for (Type genericParameterType : genericParameterTypes) {
                safeCheckType(genericParameterType);
            }

            Class<?> returnType;
            try {
                returnType = method.getReturnType();
            } catch (Throwable t) {
                LOGGER.warn(
                        COMMON_IO_EXCEPTION,
                        "Failed to get return type of method " + method.getName()
                                + " in class " + clazz.getName() + ", skipping method",
                        t);
                continue;
            }
            safeCheckClass(returnType);

            Type genericReturnType;
            try {
                genericReturnType = method.getGenericReturnType();
            } catch (Throwable t) {
                LOGGER.warn(
                        COMMON_IO_EXCEPTION,
                        "Failed to get generic return type of method " + method.getName()
                                + " in class " + clazz.getName() + ", skipping method",
                        t);
                continue;
            }
            safeCheckType(genericReturnType);

            Class<?>[] exceptionTypes;
            try {
                exceptionTypes = method.getExceptionTypes();
            } catch (Throwable t) {
                LOGGER.warn(
                        COMMON_IO_EXCEPTION,
                        "Failed to get exception types of method " + method.getName()
                                + " in class " + clazz.getName() + ", skipping method",
                        t);
                continue;
            }
            for (Class<?> exceptionType : exceptionTypes) {
                safeCheckClass(exceptionType);
            }

            Type[] genericExceptionTypes;
            try {
                genericExceptionTypes = method.getGenericExceptionTypes();
            } catch (Throwable t) {
                LOGGER.warn(
                        COMMON_IO_EXCEPTION,
                        "Failed to get generic exception types of method " + method.getName()
                                + " in class " + clazz.getName() + ", skipping method",
                        t);
                continue;
            }
            for (Type genericExceptionType : genericExceptionTypes) {
                safeCheckType(genericExceptionType);
            }
        }
    }

    private void safeCheckType(Type type) {
        if (type == null) {
            return;
        }

        if (!markedTypeCache.add(type)) {
            return;
        }

        try {
            if (type instanceof Class) {
                safeCheckClass((Class<?>) type);
                return;
            }

            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                safeCheckClass((Class<?>) parameterizedType.getRawType());
                for (Type actualTypeArgument : parameterizedType.getActualTypeArguments()) {
                    safeCheckType(actualTypeArgument);
                }
            } else if (type instanceof GenericArrayType) {
                GenericArrayType genericArrayType = (GenericArrayType) type;
                safeCheckType(genericArrayType.getGenericComponentType());
            } else if (type instanceof TypeVariable) {
                TypeVariable typeVariable = (TypeVariable) type;
                for (Type bound : typeVariable.getBounds()) {
                    safeCheckType(bound);
                }
            } else if (type instanceof WildcardType) {
                WildcardType wildcardType = (WildcardType) type;
                for (Type bound : wildcardType.getUpperBounds()) {
                    safeCheckType(bound);
                }
                for (Type bound : wildcardType.getLowerBounds()) {
                    safeCheckType(bound);
                }
            }
        } catch (Throwable t) {
            LOGGER.warn(
                    COMMON_IO_EXCEPTION,
                    "Failed to process type " + type.getTypeName() + ", skipping type",
                    t);
        }
    }

    private boolean safeCheckClass(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        if (!markedTypeCache.add(clazz)) {
            return false;
        }

        addToAllow(clazz);

        if (ClassUtils.isSimpleType(clazz) || clazz.isPrimitive() || clazz.isArray()) {
            return true;
        }
        String className = clazz.getName();
        if (className.startsWith("java.")
                || className.startsWith("javax.")
                || className.startsWith("com.sun.")
                || className.startsWith("sun.")
                || className.startsWith("jdk.")) {
            return true;
        }

        Class<?>[] interfaces;
        try {
            interfaces = clazz.getInterfaces();
        } catch (Throwable t) {
            LOGGER.warn(
                    COMMON_IO_EXCEPTION,
                    "Failed to get interfaces of class " + clazz.getName() + ", skipping interfaces",
                    t);
            interfaces = new Class<?>[0];
        }
        for (Class<?> interfaceClass : interfaces) {
            safeCheckClass(interfaceClass);
        }

        Type[] genericInterfaces;
        try {
            genericInterfaces = clazz.getGenericInterfaces();
        } catch (Throwable t) {
            LOGGER.warn(
                    COMMON_IO_EXCEPTION,
                    "Failed to get generic interfaces of class " + clazz.getName()
                            + ", skipping generic interfaces",
                    t);
            genericInterfaces = new Type[0];
        }
        for (Type genericInterface : genericInterfaces) {
            safeCheckType(genericInterface);
        }

        Class<?> superclass;
        try {
            superclass = clazz.getSuperclass();
        } catch (Throwable t) {
            LOGGER.warn(
                    COMMON_IO_EXCEPTION,
                    "Failed to get superclass of class " + clazz.getName() + ", skipping superclass",
                    t);
            superclass = null;
        }
        if (superclass != null) {
            safeCheckClass(superclass);
        }

        Type genericSuperclass;
        try {
            genericSuperclass = clazz.getGenericSuperclass();
        } catch (Throwable t) {
            LOGGER.warn(
                    COMMON_IO_EXCEPTION,
                    "Failed to get generic superclass of class " + clazz.getName()
                            + ", skipping generic superclass",
                    t);
            genericSuperclass = null;
        }
        if (genericSuperclass != null) {
            safeCheckType(genericSuperclass);
        }

        Field[] fields;
        try {
            fields = clazz.getDeclaredFields();
        } catch (Throwable t) {
            LOGGER.warn(
                    COMMON_IO_EXCEPTION,
                    "Failed to get declared fields of class " + clazz.getName() + ", skipping fields",
                    t);
            fields = new Field[0];
        }

        for (Field field : fields) {
            if (Modifier.isTransient(field.getModifiers())) {
                continue;
            }

            Class<?> fieldClass;
            try {
                fieldClass = field.getType();
            } catch (Throwable t) {
                LOGGER.warn(
                        COMMON_IO_EXCEPTION,
                        "Failed to get field type of field " + field.getName()
                                + " in class " + clazz.getName() + ", skipping field",
                        t);
                continue;
            }
            safeCheckClass(fieldClass);

            Type genericType;
            try {
                genericType = field.getGenericType();
            } catch (Throwable t) {
                LOGGER.warn(
                        COMMON_IO_EXCEPTION,
                        "Failed to get generic type of field " + field.getName()
                                + " in class " + clazz.getName() + ", skipping field generic type",
                        t);
                continue;
            }
            safeCheckType(genericType);
        }

        return true;
    }

    private void addToAllow(Class<?> clazz) {
        if (classHolder != null) {
            classHolder.storeClass(clazz);
        }

        String className = clazz.getName();
        // ignore jdk
        if (className.startsWith("java.")
                || className.startsWith("javax.")
                || className.startsWith("com.sun.")
                || className.startsWith("jakarta.")
                || className.startsWith("sun.")
                || className.startsWith("jdk.")) {
            serializeSecurityManager.addToAllowed(className);
            return;
        }

        // add group package
        String[] subs = className.split("\\.");
        if (subs.length > trustSerializeClassLevel) {
            serializeSecurityManager.addToAllowed(
                    Arrays.stream(subs).limit(trustSerializeClassLevel).collect(Collectors.joining(".")) + ".");
        } else {
            serializeSecurityManager.addToAllowed(className);
        }
    }
}
