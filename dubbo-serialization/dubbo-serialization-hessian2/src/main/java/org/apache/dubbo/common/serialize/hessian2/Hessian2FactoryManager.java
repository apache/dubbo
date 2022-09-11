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
package org.apache.dubbo.common.serialize.hessian2;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;

import com.alibaba.com.caucho.hessian.io.SerializerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class Hessian2FactoryManager {
    String WHITELIST = "dubbo.application.hessian2.whitelist";
    String ALLOW = "dubbo.application.hessian2.allow";
    String DENY = "dubbo.application.hessian2.deny";
    private volatile SerializerFactory SYSTEM_SERIALIZER_FACTORY;
    private final Map<ClassLoader, SerializerFactory> CL_2_SERIALIZER_FACTORY = new ConcurrentHashMap<>();

    private final Hessian2AllowClassManager hessian2AllowClassManager;

    public Hessian2FactoryManager(FrameworkModel frameworkModel) {
        hessian2AllowClassManager = new Hessian2AllowClassManager(frameworkModel);
    }

    public SerializerFactory getSerializerFactory(ClassLoader classLoader) {
        if (classLoader == null) {
            // system classloader
            if (SYSTEM_SERIALIZER_FACTORY == null) {
                synchronized (this) {
                    if (SYSTEM_SERIALIZER_FACTORY == null) {
                        SYSTEM_SERIALIZER_FACTORY = createSerializerFactory();
                    }
                }
            }
            return SYSTEM_SERIALIZER_FACTORY;
        }

        if (!CL_2_SERIALIZER_FACTORY.containsKey(classLoader)) {
            synchronized (this) {
                if (!CL_2_SERIALIZER_FACTORY.containsKey(classLoader)) {
                    SerializerFactory serializerFactory = createSerializerFactory();
                    CL_2_SERIALIZER_FACTORY.put(classLoader, serializerFactory);
                    return serializerFactory;
                }
            }
        }
        return CL_2_SERIALIZER_FACTORY.get(classLoader);
    }

    private SerializerFactory createSerializerFactory() {
        String whitelist = System.getProperty(WHITELIST);
        if (StringUtils.isNotEmpty(whitelist)) {
            return createWhiteListSerializerFactory();
        }

        return createDefaultSerializerFactory();
    }

    private SerializerFactory createDefaultSerializerFactory() {
        Hessian2SerializerFactory hessian2SerializerFactory = new Hessian2SerializerFactory(hessian2AllowClassManager);
        hessian2SerializerFactory.setAllowNonSerializable(Boolean.parseBoolean(System.getProperty("dubbo.hessian.allowNonSerializable", "false")));
        hessian2SerializerFactory.getClassFactory().allow("org.apache.dubbo.*");
        return hessian2SerializerFactory;
    }

    public SerializerFactory createWhiteListSerializerFactory() {
        SerializerFactory serializerFactory = new Hessian2SerializerFactory(hessian2AllowClassManager);
        String whiteList = System.getProperty(WHITELIST);
        if ("true".equals(whiteList)) {
            serializerFactory.getClassFactory().setWhitelist(true);
            String allowPattern = System.getProperty(ALLOW);
            if (StringUtils.isNotEmpty(allowPattern)) {
                for (String pattern : allowPattern.split(";")) {
                    serializerFactory.getClassFactory().allow(pattern);
                }
            }
        } else {
            serializerFactory.getClassFactory().setWhitelist(false);
            String denyPattern = System.getProperty(DENY);
            if (StringUtils.isNotEmpty(denyPattern)) {
                for (String pattern : denyPattern.split(";")) {
                    serializerFactory.getClassFactory().deny(pattern);
                }
            }
        }
        serializerFactory.setAllowNonSerializable(Boolean.parseBoolean(System.getProperty("dubbo.hessian.allowNonSerializable", "false")));
        serializerFactory.getClassFactory().allow("org.apache.dubbo.*");
        return serializerFactory;
    }

    public void onRemoveClassLoader(ClassLoader classLoader) {
        CL_2_SERIALIZER_FACTORY.remove(classLoader);
    }
}
