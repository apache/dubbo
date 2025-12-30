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
package org.apache.dubbo.rpc.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.CommonConstants.GENERIC_RAW_RETURN;
import static org.apache.dubbo.common.constants.CommonConstants.GENERIC_SERIALIZATION_BEAN;
import static org.apache.dubbo.common.constants.CommonConstants.GENERIC_SERIALIZATION_DEFAULT;
import static org.apache.dubbo.common.constants.CommonConstants.GENERIC_SERIALIZATION_GSON;
import static org.apache.dubbo.common.constants.CommonConstants.GENERIC_SERIALIZATION_NATIVE_JAVA;
import static org.apache.dubbo.common.constants.CommonConstants.GENERIC_SERIALIZATION_PROTOBUF;

/**
 * ProtocolUtils
 */
public final class ProtocolUtils {

    private static final ConcurrentMap<String, GroupServiceKeyCache> GROUP_SERVICE_KEY_CACHE_MAP =
            new ConcurrentHashMap<>();

    private ProtocolUtils() {}

    /**
     * @param url url
     * @return service key
     */
    public static String serviceKey(final URL url) {
        return serviceKey(url.getPort(), url.getPath(), url.getVersion(), url.getGroup());
    }

    /**
     * @param port           port
     * @param serviceName    serviceName
     * @param serviceVersion serviceVersion
     * @param serviceGroup   serviceGroup
     * @return service key
     */
    public static String serviceKey(
            final int port, final String serviceName, final String serviceVersion, final String serviceGroup) {
        String group = serviceGroup == null ? "" : serviceGroup;
        GroupServiceKeyCache groupServiceKeyCache = GROUP_SERVICE_KEY_CACHE_MAP.get(group);
        if (groupServiceKeyCache == null) {
            GROUP_SERVICE_KEY_CACHE_MAP.putIfAbsent(group, new GroupServiceKeyCache(group));
            groupServiceKeyCache = GROUP_SERVICE_KEY_CACHE_MAP.get(group);
        }
        return groupServiceKeyCache.getServiceKey(serviceName, serviceVersion, port);
    }

    /**
     * @param generic generic
     * @return is generic
     */
    public static boolean isGeneric(final String generic) {
        return StringUtils.isNotEmpty(generic)
                && (GENERIC_SERIALIZATION_DEFAULT.equalsIgnoreCase(generic)
                        || GENERIC_SERIALIZATION_NATIVE_JAVA.equalsIgnoreCase(generic)
                        || GENERIC_SERIALIZATION_BEAN.equalsIgnoreCase(generic)
                        || GENERIC_SERIALIZATION_PROTOBUF.equalsIgnoreCase(generic)
                        || GENERIC_SERIALIZATION_GSON.equalsIgnoreCase(generic)
                        || GENERIC_RAW_RETURN.equalsIgnoreCase(generic));
    }

    /**
     * @param generic generic
     * @return is valid generic value
     */
    public static boolean isValidGenericValue(final String generic) {
        return isGeneric(generic) || Boolean.FALSE.toString().equalsIgnoreCase(generic);
    }

    /**
     * @param generic generic
     * @return is default generic
     */
    public static boolean isDefaultGenericSerialization(final String generic) {
        return isGeneric(generic) && GENERIC_SERIALIZATION_DEFAULT.equalsIgnoreCase(generic);
    }

    /**
     * @param generic generic
     * @return is java generic
     */
    public static boolean isJavaGenericSerialization(final String generic) {
        return isGeneric(generic) && GENERIC_SERIALIZATION_NATIVE_JAVA.equalsIgnoreCase(generic);
    }

    /**
     * @param generic generic
     * @return is gson generic
     */
    public static boolean isGsonGenericSerialization(final String generic) {
        return isGeneric(generic) && GENERIC_SERIALIZATION_GSON.equalsIgnoreCase(generic);
    }

    /**
     * @param generic generic
     * @return is bean generic
     */
    public static boolean isBeanGenericSerialization(final String generic) {
        return isGeneric(generic) && GENERIC_SERIALIZATION_BEAN.equals(generic);
    }

    /**
     * @param generic generic
     * @return is protobuf generic
     */
    public static boolean isProtobufGenericSerialization(final String generic) {
        return isGeneric(generic) && GENERIC_SERIALIZATION_PROTOBUF.equals(generic);
    }

    /**
     * @param generic generic
     * @return is generic return raw result
     */
    public static boolean isGenericReturnRawResult(final String generic) {
        return GENERIC_RAW_RETURN.equals(generic);
    }
}
