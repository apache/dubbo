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
    private final SerializeSecurityManager serializeSecurityManager;

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(SerializeSecurityConfigurator.class);

    private final ModuleModel moduleModel;

    private final ClassHolder classHolder;

    private volatile boolean autoTrustSerializeClass = true;

    private volatile int trustSerializeClassLevel = Integer.MAX_VALUE;

    public SerializeSecurityConfigurator(ModuleModel moduleModel) {
        this.moduleModel = moduleModel;
        moduleModel.addClassLoaderListener(this);

        FrameworkModel frameworkModel = moduleModel.getApplicationModel().getFrameworkModel();
        serializeSecurityManager = frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class);
        classHolder = NativeDetector.inNativeImage() ? frameworkModel.getBeanFactory().getBean(ClassHolder.class) : null;

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
                logger.info("Read serialize allow list from " + u);
                String[] lines = IOUtils.readLines(u.openStream());
                for (String line : lines) {
                    line = line.trim();
                    if (StringUtils.isEmpty(line) || line.startsWith("#")) {
                        continue;
                    }
                    serializeSecurityManager.addToAlwaysAllowed(line);
                }
            } catch (IOException e) {
                logger.error(
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
                logger.info("Read serialize blocked list from " + u);
                String[] lines = IOUtils.readLines(u.openStream());
                for (String line : lines) {
                    line = line.trim();
                    if (StringUtils.isEmpty(line) || line.startsWith("#")) {
                        continue;
                    }
                    serializeSecurityManager.addToDisAllowed(line);
                }
            } catch (IOException e) {
                logger.error(
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

        Set<Type> markedClass = new HashSet<>();
        checkClass(markedClass, clazz);

        addToAllow(clazz);

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

    private void checkType(Set<Type> markedClass, Type type) {
        if (type == null) {
            return;
        }

        if (type instanceof Class) {
            checkClass(markedClass, (Class<?>) type);
            return;
        }

        if (!markedClass.add(type)) {
            return;
        }

        if (type instanceof ParameterizedType) {
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

    private void checkClass(Set<Type> markedClass, Class<?> clazz) {
        if (clazz == null) {
            return;
        }

        if (!markedClass.add(clazz)) {
            return;
        }

        addToAllow(clazz);

        if (ClassUtils.isSimpleType(clazz) || clazz.isPrimitive() || clazz.isArray()) {
            return;
        }
        String className = clazz.getName();
        if (className.startsWith("java.")
                || className.startsWith("javax.")
                || className.startsWith("com.sun.")
                || className.startsWith("sun.")
                || className.startsWith("jdk.")) {
            return;
        }

        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> interfaceClass : interfaces) {
            checkClass(markedClass, interfaceClass);
        }

        for (Type genericInterface : clazz.getGenericInterfaces()) {
            checkType(markedClass, genericInterface);
        }

        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            checkClass(markedClass, superclass);
        }

        Type genericSuperclass = clazz.getGenericSuperclass();
        if (genericSuperclass != null) {
            checkType(markedClass, genericSuperclass);
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
