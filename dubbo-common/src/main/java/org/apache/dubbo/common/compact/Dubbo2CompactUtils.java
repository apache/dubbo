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
package org.apache.dubbo.common.compact;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.StringUtils;

import java.lang.annotation.Annotation;

public class Dubbo2CompactUtils {
    private static volatile boolean enabled = true;
    private static final Class<? extends Annotation> REFERENCE_CLASS;
    private static final Class<? extends Annotation> SERVICE_CLASS;
    private static final Class<?> ECHO_SERVICE_CLASS;
    private static final Class<?> GENERIC_SERVICE_CLASS;

    static {
        initEnabled();
        REFERENCE_CLASS = loadAnnotation("com.alibaba.dubbo.config.annotation.Reference");
        SERVICE_CLASS = loadAnnotation("com.alibaba.dubbo.config.annotation.Service");
        ECHO_SERVICE_CLASS = loadClass("com.alibaba.dubbo.rpc.service.EchoService");
        GENERIC_SERVICE_CLASS = loadClass("com.alibaba.dubbo.rpc.service.GenericService");
    }

    private static void initEnabled() {
        try {
            String fromProp = System.getProperty(CommonConstants.DUBBO2_COMPACT_ENABLE);
            if (StringUtils.isNotEmpty(fromProp)) {
                enabled = Boolean.parseBoolean(fromProp);
                return;
            }
            String fromEnv = System.getenv(CommonConstants.DUBBO2_COMPACT_ENABLE);
            if (StringUtils.isNotEmpty(fromEnv)) {
                enabled = Boolean.parseBoolean(fromEnv);
                return;
            }
            fromEnv = System.getenv(StringUtils.toOSStyleKey(CommonConstants.DUBBO2_COMPACT_ENABLE));
            enabled = !StringUtils.isNotEmpty(fromEnv) || Boolean.parseBoolean(fromEnv);
        } catch (Throwable t) {
            enabled = true;
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        Dubbo2CompactUtils.enabled = enabled;
    }

    private static Class<?> loadClass(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Annotation> loadAnnotation(String name) {
        try {
            Class<?> clazz = Class.forName(name);
            if (clazz.isAnnotation()) {
                return (Class<? extends Annotation>) clazz;
            } else {
                return null;
            }
        } catch (Throwable e) {
            return null;
        }
    }

    public static boolean isReferenceClassLoaded() {
        return REFERENCE_CLASS != null;
    }

    public static Class<? extends Annotation> getReferenceClass() {
        return REFERENCE_CLASS;
    }

    public static boolean isServiceClassLoaded() {
        return SERVICE_CLASS != null;
    }

    public static Class<? extends Annotation> getServiceClass() {
        return SERVICE_CLASS;
    }

    public static boolean isEchoServiceClassLoaded() {
        return ECHO_SERVICE_CLASS != null;
    }

    public static Class<?> getEchoServiceClass() {
        return ECHO_SERVICE_CLASS;
    }

    public static boolean isGenericServiceClassLoaded() {
        return GENERIC_SERVICE_CLASS != null;
    }

    public static Class<?> getGenericServiceClass() {
        return GENERIC_SERVICE_CLASS;
    }
}
