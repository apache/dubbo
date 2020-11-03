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
import static org.apache.dubbo.common.constants.CommonConstants.GENERIC_SERIALIZATION_NATIVE_JAVA;
import static org.apache.dubbo.common.constants.CommonConstants.GENERIC_SERIALIZATION_PROTOBUF;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;

public class ProtocolUtils {

    private static final ConcurrentMap<String, GroupServiceKeyCache> groupServiceKeyCacheMap = new ConcurrentHashMap<>();

    private ProtocolUtils() {
    }

    public static String serviceKey(URL url) {
        return serviceKey(url.getPort(), url.getPath(), url.getParameter(VERSION_KEY),
                url.getParameter(GROUP_KEY));
    }

    public static String serviceKey(int port, String serviceName, String serviceVersion, String serviceGroup) {
        serviceGroup = serviceGroup == null ? "" : serviceGroup;
        GroupServiceKeyCache groupServiceKeyCache = groupServiceKeyCacheMap.get(serviceGroup);
        if (groupServiceKeyCache == null) {
            groupServiceKeyCacheMap.putIfAbsent(serviceGroup, new GroupServiceKeyCache(serviceGroup));
            groupServiceKeyCache = groupServiceKeyCacheMap.get(serviceGroup);
        }
        return groupServiceKeyCache.getServiceKey(serviceName, serviceVersion, port);
    }

    public static boolean isGeneric(String generic) {
        return StringUtils.isNotEmpty(generic)
                && (GENERIC_SERIALIZATION_DEFAULT.equalsIgnoreCase(generic)  /* Normal generalization cal */
                || GENERIC_SERIALIZATION_NATIVE_JAVA.equalsIgnoreCase(generic) /* Streaming generalization call supporting jdk serialization */
                || GENERIC_SERIALIZATION_BEAN.equalsIgnoreCase(generic)
                || GENERIC_SERIALIZATION_PROTOBUF.equalsIgnoreCase(generic)
                || GENERIC_RAW_RETURN.equalsIgnoreCase(generic));

    }

    public static boolean isValidGenericValue(String generic) {
        return isGeneric(generic) || Boolean.FALSE.toString().equalsIgnoreCase(generic);

    }

    public static boolean isDefaultGenericSerialization(String generic) {
        return isGeneric(generic)
                && GENERIC_SERIALIZATION_DEFAULT.equalsIgnoreCase(generic);
    }

    public static boolean isJavaGenericSerialization(String generic) {
        return isGeneric(generic)
                && GENERIC_SERIALIZATION_NATIVE_JAVA.equalsIgnoreCase(generic);
    }

    public static boolean isBeanGenericSerialization(String generic) {
        return isGeneric(generic) && GENERIC_SERIALIZATION_BEAN.equals(generic);
    }

    public static boolean isProtobufGenericSerialization(String generic) {
        return isGeneric(generic) && GENERIC_SERIALIZATION_PROTOBUF.equals(generic);
    }

    public static boolean isGenericReturnRawResult(String generic) {
        return GENERIC_RAW_RETURN.equals(generic);
    }
}
