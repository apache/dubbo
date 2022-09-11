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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.AllowClassNotifyListener;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.SerializeCheckStatus;
import org.apache.dubbo.common.utils.SerializeSecurityManager;
import org.apache.dubbo.rpc.model.FrameworkModel;

import com.alibaba.fastjson2.filter.ContextAutoTypeBeforeHandler;
import com.alibaba.fastjson2.filter.Filter;
import com.alibaba.fastjson2.util.TypeUtils;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.alibaba.fastjson2.util.TypeUtils.loadClass;

public class Fastjson2SecurityManager implements AllowClassNotifyListener {
    private Filter securityFilter = new Handler(AllowClassNotifyListener.DEFAULT_STATUS, new String[0]);

    private final static Logger logger = LoggerFactory.getLogger(Fastjson2SecurityManager.class);

    private final static Set<String> warnedClasses = new ConcurrentHashSet<>(1);

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
            super(acceptNames);
            this.status = status;
        }

        @Override
        public Class<?> apply(String typeName, Class<?> expectClass, long features) {
            switch (status) {
                case STRICT:
                    return super.apply(typeName, expectClass, features);
                case WARN:
                    Class<?> tryLoad = super.apply(typeName, expectClass, features);
                    if (tryLoad != null) {
                        return tryLoad;
                    }
                case DISABLED:
                    Class<?> localClass = loadClassDirectly(typeName);
                    if (localClass != null) {
                        if (status == SerializeCheckStatus.WARN && warnedClasses.add(typeName)) {
                            logger.error("[Serialization Security] Serialized class " + localClass.getName() + " is not in allow list. " +
                                "Current mode is `WARN`, will allow to deserialize it by default. " +
                                "Dubbo will set to `STRICT` mode by default in the future. " +
                                "Please add it into security/serialize.allowlist or follow FAQ to configure it.");
                        }
                        return localClass;
                    }
                default:
                    return null;
            }
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
