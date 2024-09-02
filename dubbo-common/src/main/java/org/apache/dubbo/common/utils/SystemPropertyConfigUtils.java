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

import org.apache.dubbo.common.constants.CommonConstants;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class SystemPropertyConfigUtils {

    private static Set<String> systemProperties = new HashSet<>();

    static {
        Class<?>[] classes = new Class[] {
            CommonConstants.SystemProperty.class,
            CommonConstants.ThirdPartyProperty.class,
            CommonConstants.DubboProperty.class
        };
        for (Class<?> clazz : classes) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                try {
                    assert systemProperties != null;
                    Object value = field.get(null);
                    if (value instanceof String) {
                        systemProperties.add((String) value);
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(
                            String.format("%s does not have field of %s", clazz.getName(), field.getName()));
                }
            }
        }
    }

    /**
     * Return property of VM.
     *
     * @param key
     * @return
     */
    public static String getSystemProperty(String key) {
        if (containsKey(key)) {
            return System.getProperty(key);
        } else {
            throw new IllegalStateException(String.format(
                    "System property [%s] does not define in org.apache.dubbo.common.constants.CommonConstants", key));
        }
    }

    /**
     * Return property of VM. If not exist, the default value is returned.
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static String getSystemProperty(String key, String defaultValue) {
        if (containsKey(key)) {
            return System.getProperty(key, defaultValue);
        } else {
            throw new IllegalStateException(String.format(
                    "System property [%s] does not define in org.apache.dubbo.common.constants.CommonConstants", key));
        }
    }

    /**
     * Set property of VM.
     *
     * @param key
     * @param value
     * @return
     */
    public static String setSystemProperty(String key, String value) {
        if (containsKey(key)) {
            return System.setProperty(key, value);
        } else {
            throw new IllegalStateException(String.format(
                    "System property [%s] does not define in org.apache.dubbo.common.constants.CommonConstants", key));
        }
    }

    /**
     * Clear property of VM.
     *
     * @param key
     * @return
     */
    public static String clearSystemProperty(String key) {
        if (containsKey(key)) {
            return System.clearProperty(key);
        } else {
            throw new IllegalStateException(String.format(
                    "System property [%s] does not define in org.apache.dubbo.common.constants.CommonConstants", key));
        }
    }

    /**
     * Check whether the key is valid.
     *
     * @param key
     * @return
     */
    private static boolean containsKey(String key) {
        return systemProperties.contains(key);
    }
}
