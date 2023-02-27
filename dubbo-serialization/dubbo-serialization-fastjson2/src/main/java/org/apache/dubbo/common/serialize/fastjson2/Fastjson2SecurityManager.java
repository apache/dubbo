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
package org.apache.dubbo.common.serialize.fastjson2;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.AllowClassNotifyListener;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.SerializeCheckStatus;
import org.apache.dubbo.common.utils.SerializeSecurityManager;
import org.apache.dubbo.rpc.model.FrameworkModel;

import com.alibaba.fastjson2.filter.ContextAutoTypeBeforeHandler;
import com.alibaba.fastjson2.util.TypeUtils;

import static com.alibaba.fastjson2.util.TypeUtils.loadClass;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_UNTRUSTED_SERIALIZE_CLASS;
import static org.apache.dubbo.common.utils.SerializeCheckStatus.STRICT;

public class Fastjson2SecurityManager implements AllowClassNotifyListener {
    private volatile Handler securityFilter;

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(Fastjson2SecurityManager.class);

    private final SerializeSecurityManager securityManager;

    private volatile SerializeCheckStatus status = AllowClassNotifyListener.DEFAULT_STATUS;

    private volatile boolean checkSerializable = true;

    private volatile Set<String> allowedList = new ConcurrentHashSet<>(1);

    private volatile Set<String> disAllowedList = new ConcurrentHashSet<>(1);

    public Fastjson2SecurityManager(FrameworkModel frameworkModel) {
        securityManager = frameworkModel.getBeanFactory().getOrRegisterBean(SerializeSecurityManager.class);
        securityManager.registerListener(this);
        securityFilter = new Handler(AllowClassNotifyListener.DEFAULT_STATUS, securityManager, true, new String[0], new ConcurrentHashSet<>());
    }

    @Override
    public synchronized void notifyPrefix(Set<String> allowedList, Set<String> disAllowedList) {
        this.allowedList = allowedList;
        this.disAllowedList = disAllowedList;
        this.securityFilter = new Handler(this.status, this.securityManager, this.checkSerializable, this.allowedList.toArray(new String[0]), this.disAllowedList);
    }

    @Override
    public synchronized void notifyCheckStatus(SerializeCheckStatus status) {
        this.status = status;
        this.securityFilter = new Handler(this.status, this.securityManager, this.checkSerializable, this.allowedList.toArray(new String[0]), this.disAllowedList);
    }

    @Override
    public synchronized void notifyCheckSerializable(boolean checkSerializable) {
        this.checkSerializable = checkSerializable;
        this.securityFilter = new Handler(this.status, this.securityManager, this.checkSerializable, this.allowedList.toArray(new String[0]), this.disAllowedList);

    }

    public Handler getSecurityFilter() {
        return securityFilter;
    }

    public static class Handler extends ContextAutoTypeBeforeHandler {
        final SerializeCheckStatus status;
        final SerializeSecurityManager serializeSecurityManager;
        final Map<String, Class<?>> classCache = new ConcurrentHashMap<>(16, 0.75f, 1);

        final Set<String> disAllowedList;

        final boolean checkSerializable;

        public Handler(SerializeCheckStatus status, SerializeSecurityManager serializeSecurityManager, boolean checkSerializable, String[] acceptNames, Set<String> disAllowedList) {
            super(true, acceptNames);
            this.status = status;
            this.serializeSecurityManager = serializeSecurityManager;
            this.checkSerializable = checkSerializable;
            this.disAllowedList = disAllowedList;
        }

        @Override
        public Class<?> apply(String typeName, Class<?> expectClass, long features) {
            Class<?> tryLoad = super.apply(typeName, expectClass, features);

            // 1. in allow list, return
            if (tryLoad != null) {
                return tryLoad;
            }

            // 2. check if in strict mode
            if (status == STRICT) {
                String msg = "[Serialization Security] Serialized class " + typeName + " is not in allow list. " +
                    "Current mode is `STRICT`, will disallow to deserialize it by default. " +
                    "Please add it into security/serialize.allowlist or follow FAQ to configure it.";
                if (serializeSecurityManager.getWarnedClasses().add(typeName)) {
                    logger.error(PROTOCOL_UNTRUSTED_SERIALIZE_CLASS, "", "", msg);
                }

                return null;
            }

            // 3. try load
            Class<?> localClass = loadClassDirectly(typeName);
            if (localClass != null) {
                if (status == SerializeCheckStatus.WARN && serializeSecurityManager.getWarnedClasses().add(typeName)) {
                    logger.error(PROTOCOL_UNTRUSTED_SERIALIZE_CLASS, "", "",
                        "[Serialization Security] Serialized class " + localClass.getName() + " is not in allow list. " +
                            "Current mode is `WARN`, will allow to deserialize it by default. " +
                            "Dubbo will set to `STRICT` mode by default in the future. " +
                            "Please add it into security/serialize.allowlist or follow FAQ to configure it.");
                }
                return localClass;
            }

            // 4. class not found
            return null;
        }

        public boolean checkIfDisAllow(String typeName) {
            return disAllowedList.stream().anyMatch(typeName::startsWith);
        }

        public boolean isCheckSerializable() {
            return checkSerializable;
        }

        public Class<?> loadClassDirectly(String typeName) {
            Class<?> clazz = classCache.get(typeName);

            if (clazz == null && checkIfDisAllow(typeName)) {
                clazz = DenyClass.class;
                String msg = "[Serialization Security] Serialized class " + typeName + " is in disAllow list. " +
                    "Current mode is `WARN`, will disallow to deserialize it by default. " +
                    "Please add it into security/serialize.allowlist or follow FAQ to configure it.";
                if (serializeSecurityManager.getWarnedClasses().add(typeName)) {
                    logger.error(PROTOCOL_UNTRUSTED_SERIALIZE_CLASS, "", "", msg);
                }
            }

            if (clazz == null) {
                clazz = TypeUtils.getMapping(typeName);
            }

            if (clazz == null) {
                clazz = loadClass(typeName);
            }

            if (clazz != null) {
                Class<?> origin = classCache.putIfAbsent(typeName, clazz);
                if (origin != null) {
                    clazz = origin;
                }
            }

            if (clazz == DenyClass.class) {
                return null;
            }

            return clazz;
        }

    }

    private static class DenyClass {
        // To indicate that the target class has been reject
    }
}
