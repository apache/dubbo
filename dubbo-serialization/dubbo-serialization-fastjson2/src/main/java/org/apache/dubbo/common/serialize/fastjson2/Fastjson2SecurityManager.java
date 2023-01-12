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
import com.alibaba.fastjson2.filter.Filter;
import com.alibaba.fastjson2.util.TypeUtils;

import static com.alibaba.fastjson2.util.TypeUtils.loadClass;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_UNTRUSTED_SERIALIZE_CLASS;
import static org.apache.dubbo.common.utils.SerializeCheckStatus.STRICT;

public class Fastjson2SecurityManager implements AllowClassNotifyListener {
    private Filter securityFilter = new Handler(AllowClassNotifyListener.DEFAULT_STATUS, new String[0]);

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(Fastjson2SecurityManager.class);

    private static final Set<String> warnedClasses = new ConcurrentHashSet<>(1);

    public Fastjson2SecurityManager(FrameworkModel frameworkModel) {
        SerializeSecurityManager securityManager = frameworkModel.getBeanFactory().getOrRegisterBean(SerializeSecurityManager.class);
        securityManager.registerListener(this);
    }

    public void notify(SerializeCheckStatus status, Set<String> prefixList) {
        this.securityFilter = new Handler(status, prefixList.toArray(new String[0]));
    }

    public Filter getSecurityFilter() {
        return securityFilter;
    }

    public static class Handler extends ContextAutoTypeBeforeHandler {
        final SerializeCheckStatus status;
        final Map<String, Class<?>> classCache = new ConcurrentHashMap<>(16, 0.75f, 1);

        public Handler(SerializeCheckStatus status, String[] acceptNames) {
            super(true, acceptNames);
            this.status = status;
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
                if (warnedClasses.add(typeName)) {
                    logger.error(PROTOCOL_UNTRUSTED_SERIALIZE_CLASS, "", "", msg);
                }

                return null;
            }

            // 3. try load
            Class<?> localClass = loadClassDirectly(typeName);
            if (localClass != null) {
                if (status == SerializeCheckStatus.WARN && warnedClasses.add(typeName)) {
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

        public Class<?> loadClassDirectly(String typeName) {
            Class<?> clazz = classCache.get(typeName);

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


            return clazz;
        }
    }
}
