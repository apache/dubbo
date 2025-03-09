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
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialization.ClassHolder;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Set;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_UNTRUSTED_SERIALIZE_CLASS;

/**
 * Inspired by Fastjson2
 * See {@code com.alibaba.fastjson2.filter.ContextAutoTypeBeforeHandler#apply(java.lang.String, java.lang.Class, long)}
 */
public class DefaultSerializeClassChecker implements AllowClassNotifyListener {

    private static final long MAGIC_HASH_CODE = 0xcbf29ce484222325L;
    private static final long MAGIC_PRIME = 0x100000001b3L;

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(DefaultSerializeClassChecker.class);

    private volatile SerializeCheckStatus checkStatus = AllowClassNotifyListener.DEFAULT_STATUS;
    private volatile boolean checkSerializable = true;

    private final SerializeSecurityManager serializeSecurityManager;
    private final ClassHolder classHolder;
    private volatile long[] allowPrefixes = new long[0];
    private volatile long[] disAllowPrefixes = new long[0];

    public DefaultSerializeClassChecker(FrameworkModel frameworkModel) {
        this.serializeSecurityManager =
                frameworkModel.getBeanFactory().getOrRegisterBean(SerializeSecurityManager.class);
        this.serializeSecurityManager.registerListener(this);

        this.classHolder = NativeDetector.inNativeImage()
                ? frameworkModel.getBeanFactory().getBean(ClassHolder.class)
                : null;
    }

    @Override
    public synchronized void notifyPrefix(Set<String> allowedList, Set<String> disAllowedList) {
        this.allowPrefixes = loadPrefix(allowedList);
        this.disAllowPrefixes = loadPrefix(disAllowedList);
    }

    @Override
    public synchronized void notifyCheckStatus(SerializeCheckStatus status) {
        this.checkStatus = status;
    }

    @Override
    public synchronized void notifyCheckSerializable(boolean checkSerializable) {
        this.checkSerializable = checkSerializable;
    }

    /**
     * Loads class name prefixes into a sorted hash array.
     *
     * @param allowedList Set of class name prefixes to be allowed.
     * @return Sorted array of hashed prefixes.
     */
    private static long[] loadPrefix(Set<String> allowedList) {
        long[] array = new long[allowedList.size()];
        int index = 0;

        for (String name : allowedList) {
            if (name == null || name.isEmpty()) {
                continue;
            }

            long hashCode = MAGIC_HASH_CODE;
            for (int j = 0; j < name.length(); ++j) {
                char ch = (name.charAt(j) == '$') ? '.' : name.charAt(j);
                hashCode ^= ch;
                hashCode *= MAGIC_PRIME;
            }

            array[index++] = hashCode;
        }

        if (index != array.length) {
            array = Arrays.copyOf(array, index);
        }

        Arrays.sort(array);
        return array;
    }

    /**
     * Tries to load a class.
     *
     * @param classLoader The class loader.
     * @param className   The class name.
     * @return The loaded class.
     * @throws ClassNotFoundException If the class is not found.
     * @throws IllegalArgumentException If the class is blocked (in STRICT mode).
     */
    public Class<?> loadClass(ClassLoader classLoader, String className) throws ClassNotFoundException {
        Class<?> aClass = loadClass0(classLoader, className);

        if (!aClass.isPrimitive() && !Serializable.class.isAssignableFrom(aClass)) {
            String msg = "[Serialization Security] Serialized class " + className +
                    " has not implemented Serializable interface. " +
                    "Current mode is strict check, will disallow to deserialize it by default.";

            if (serializeSecurityManager.getWarnedClasses().add(className)) {
                logger.error(PROTOCOL_UNTRUSTED_SERIALIZE_CLASS, "", "", msg);
            }

            if (checkSerializable) {
                throw new IllegalArgumentException(msg);
            }
        }

        return aClass;
    }

    private Class<?> loadClass0(ClassLoader classLoader, String className) throws ClassNotFoundException {
        if (checkStatus == SerializeCheckStatus.DISABLE) {
            return classForName(classLoader, className);
        }

        long hash = MAGIC_HASH_CODE;
        for (int i = 0, len = className.length(); i < len; ++i) {
            char ch = (className.charAt(i) == '$') ? '.' : className.charAt(i);
            hash ^= ch;
            hash *= MAGIC_PRIME;

            if (Arrays.binarySearch(allowPrefixes, hash) >= 0) {
                return classForName(classLoader, className);
            }
        }

        if (checkStatus == SerializeCheckStatus.STRICT) {
            String msg = "[Serialization Security] Serialized class " + className +
                    " is not in allow list. Current mode is `STRICT`, " +
                    "will disallow deserialization by default. Please add it to security/serialize.allowlist.";

            if (serializeSecurityManager.getWarnedClasses().add(className)) {
                logger.error(PROTOCOL_UNTRUSTED_SERIALIZE_CLASS, "", "", msg);
            }

            throw new IllegalArgumentException(msg);
        }

        if (checkStatus == SerializeCheckStatus.WARN) {
            String msg = "[Serialization Security] Serialized class " + className +
                    " is in disallow list. Current mode is `WARN`, " +
                    "allowing deserialization but logging a warning. " +
                    "Consider adding it to security/serialize.allowlist.";

            if (serializeSecurityManager.getWarnedClasses().add(className)) {
                logger.warn(PROTOCOL_UNTRUSTED_SERIALIZE_CLASS, "", "", msg);
            }

            return classForName(classLoader, className);  // ✅ Allow deserialization with a warning
        }

        return classForName(classLoader, className);
    }

    /**
     * Attempts to load a class using the ClassHolder if available.
     *
     * @param classLoader The class loader.
     * @param className   The class name.
     * @return The loaded class.
     * @throws ClassNotFoundException If the class cannot be found.
     */
    private Class<?> classForName(ClassLoader classLoader, String className) throws ClassNotFoundException {
        if (classHolder != null) {
            Class<?> aClass = classHolder.loadClass(className, classLoader);
            if (aClass != null) {
                return aClass;
            }
        }
        return ClassUtils.forName(className, classLoader);
    }

    /**
     * Retrieves the singleton instance of DefaultSerializeClassChecker.
     *
     * @return Instance of DefaultSerializeClassChecker.
     */
    public static DefaultSerializeClassChecker getInstance() {
        return FrameworkModel.defaultModel().getBeanFactory().getBean(DefaultSerializeClassChecker.class);
    }

    public boolean isCheckSerializable() {
        return checkSerializable;
    }
}
