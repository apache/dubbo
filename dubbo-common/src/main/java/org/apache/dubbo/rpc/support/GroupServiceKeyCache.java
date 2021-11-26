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

import org.apache.dubbo.common.utils.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GroupServiceKeyCache {

    private final String serviceGroup;

    //ConcurrentMap<serviceName, ConcurrentMap<serviceVersion, ConcurrentMap<port, String>>>
    private final ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<Integer, String>>> serviceKeyMap;

    public GroupServiceKeyCache(String serviceGroup) {
        this.serviceGroup = serviceGroup;
        this.serviceKeyMap = new ConcurrentHashMap<>(512);
    }

    public String getServiceKey(String serviceName, String serviceVersion, int port) {
        ConcurrentMap<String, ConcurrentMap<Integer, String>> versionMap = serviceKeyMap.get(serviceName);
        if (versionMap == null) {
            serviceKeyMap.putIfAbsent(serviceName, new ConcurrentHashMap<>());
            versionMap = serviceKeyMap.get(serviceName);
        }

        serviceVersion = serviceVersion == null ? "" : serviceVersion;
        ConcurrentMap<Integer, String> portMap = versionMap.get(serviceVersion);
        if (portMap == null) {
            versionMap.putIfAbsent(serviceVersion, new ConcurrentHashMap<>());
            portMap = versionMap.get(serviceVersion);
        }

        String serviceKey = portMap.get(port);
        if (serviceKey == null) {
            serviceKey = createServiceKey(serviceName, serviceVersion, port);
            portMap.put(port, serviceKey);
        }
        return serviceKey;
    }

    private String createServiceKey(String serviceName, String serviceVersion, int port) {
        StringBuilder buf = new StringBuilder();
        if (StringUtils.isNotEmpty(serviceGroup)) {
            buf.append(serviceGroup).append('/');
        }

        buf.append(serviceName);
        if (StringUtils.isNotEmpty(serviceVersion) && !"0.0.0".equals(serviceVersion) && !"*".equals(serviceVersion)) {
            buf.append(':').append(serviceVersion);
        }
        buf.append(':').append(port);
        return buf.toString();
    }
}
