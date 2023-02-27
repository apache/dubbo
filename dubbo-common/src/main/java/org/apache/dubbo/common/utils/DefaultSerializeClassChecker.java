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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Set;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.model.FrameworkModel;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_UNTRUSTED_SERIALIZE_CLASS;

/**
 * Inspired by Fastjson2
 * see com.alibaba.fastjson2.filter.ContextAutoTypeBeforeHandler#apply(java.lang.String, java.lang.Class, long)
 */
public class DefaultSerializeClassChecker implements AllowClassNotifyListener {

    private static final long MAGIC_HASH_CODE = 0xcbf29ce484222325L;
    private static final long MAGIC_PRIME = 0x100000001b3L;
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(DefaultSerializeClassChecker.class);
    private volatile SerializeCheckStatus checkStatus = AllowClassNotifyListener.DEFAULT_STATUS;
    private volatile boolean checkSerializable = true;

    private final SerializeSecurityManager serializeSecurityManager;
    private volatile long[] allowPrefixes = new long[0];

    private volatile long[] disAllowPrefixes = new long[0];

    public DefaultSerializeClassChecker(FrameworkModel frameworkModel) {
        serializeSecurityManager = frameworkModel.getBeanFactory().getOrRegisterBean(SerializeSecurityManager.class);
        serializeSecurityManager.registerListener(this);
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

    private static long[] loadPrefix(Set<String> allowedList) {
        long[] array = new long[allowedList.size()];

        int index = 0;
        for (String name : allowedList) {
            if (name == null || name.isEmpty()) {
                continue;
            }

            long hashCode = MAGIC_HASH_CODE;
            for (int j = 0; j < name.length(); ++j) {
                char ch = name.charAt(j);
                if (ch == '$') {
                    ch = '.';
                }
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
     * Try load class
     *
     * @param className class name
     * @throws IllegalArgumentException if class is blocked
     */
    public Class<?> loadClass(ClassLoader classLoader, String className) throws ClassNotFoundException {
        Class<?> aClass = loadClass0(classLoader, className);
        if (checkSerializable && !aClass.isPrimitive() && !Serializable.class.isAssignableFrom(aClass)) {
            String msg = "[Serialization Security] Serialized class " + className + " has not implement Serializable interface. " +
                "Current mode is strict check, will disallow to deserialize it by default. ";
            if (serializeSecurityManager.getWarnedClasses().add(className)) {
                logger.error(PROTOCOL_UNTRUSTED_SERIALIZE_CLASS, "", "", msg);
            }

            throw new IllegalArgumentException(msg);
        }

        return aClass;
    }

    private Class<?> loadClass0(ClassLoader classLoader, String className) throws ClassNotFoundException {
        if (checkStatus == SerializeCheckStatus.DISABLE) {
            return ClassUtils.forName(className, classLoader);
        }

        long hash = MAGIC_HASH_CODE;
        for (int i = 0, typeNameLength = className.length(); i < typeNameLength; ++i) {
            char ch = className.charAt(i);
            if (ch == '$') {
                ch = '.';
            }
            hash ^= ch;
            hash *= MAGIC_PRIME;

            if (Arrays.binarySearch(allowPrefixes, hash) >= 0) {
                return ClassUtils.forName(className, classLoader);
            }
        }

        if (checkStatus == SerializeCheckStatus.STRICT) {
            String msg = "[Serialization Security] Serialized class " + className + " is not in allow list. " +
                "Current mode is `STRICT`, will disallow to deserialize it by default. " +
                "Please add it into security/serialize.allowlist or follow FAQ to configure it.";
            if (serializeSecurityManager.getWarnedClasses().add(className)) {
                logger.error(PROTOCOL_UNTRUSTED_SERIALIZE_CLASS, "", "", msg);
            }

            throw new IllegalArgumentException(msg);
        }

        hash = MAGIC_HASH_CODE;
        for (int i = 0, typeNameLength = className.length(); i < typeNameLength; ++i) {
            char ch = className.charAt(i);
            if (ch == '$') {
                ch = '.';
            }
            hash ^= ch;
            hash *= MAGIC_PRIME;

            if (Arrays.binarySearch(disAllowPrefixes, hash) >= 0) {
                String msg = "[Serialization Security] Serialized class " + className + " is in disallow list. " +
                    "Current mode is `WARN`, will disallow to deserialize it by default. " +
                    "Please add it into security/serialize.allowlist or follow FAQ to configure it.";
                if (serializeSecurityManager.getWarnedClasses().add(className)) {
                    logger.error(PROTOCOL_UNTRUSTED_SERIALIZE_CLASS, "", "", msg);
                }

                throw new IllegalArgumentException(msg);
            }
        }

        hash = MAGIC_HASH_CODE;
        for (int i = 0, typeNameLength = className.length(); i < typeNameLength; ++i) {
            char ch = Character.toLowerCase(className.charAt(i));
            if (ch == '$') {
                ch = '.';
            }
            hash ^= ch;
            hash *= MAGIC_PRIME;

            if (Arrays.binarySearch(disAllowPrefixes, hash) >= 0) {
                String msg = "[Serialization Security] Serialized class " + className + " is in disallow list. " +
                    "Current mode is `WARN`, will disallow to deserialize it by default. " +
                    "Please add it into security/serialize.allowlist or follow FAQ to configure it.";
                if (serializeSecurityManager.getWarnedClasses().add(className)) {
                    logger.error(PROTOCOL_UNTRUSTED_SERIALIZE_CLASS, "", "", msg);
                }

                throw new IllegalArgumentException(msg);
            }
        }

        Class<?> clazz = ClassUtils.forName(className, classLoader);
        if (serializeSecurityManager.getWarnedClasses().add(className)) {
            logger.error(PROTOCOL_UNTRUSTED_SERIALIZE_CLASS, "", "",
                "[Serialization Security] Serialized class " + className + " is not in allow list. " +
                    "Current mode is `WARN`, will allow to deserialize it by default. " +
                    "Dubbo will set to `STRICT` mode by default in the future. " +
                    "Please add it into security/serialize.allowlist or follow FAQ to configure it.");
        }
        return clazz;
    }

    public static DefaultSerializeClassChecker getInstance() {
        return FrameworkModel.defaultModel().getBeanFactory().getBean(DefaultSerializeClassChecker.class);
    }

    public boolean isCheckSerializable() {
        return checkSerializable;
    }
}
