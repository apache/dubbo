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
package org.apache.dubbo.registry.nacos.util;

import org.apache.dubbo.common.utils.CollectionUtils;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Instance manage util for multiple serviceNames
 * To resolve bug with https://github.com/apache/dubbo/issues/5885 and https://github.com/apache/dubbo/issues/5899
 *
 * @since 2.7.6
 */
public class NacosInstanceManageUtil {

    /**
     * serviceName -> refreshed instance list
     */
    private static final Map<String, List<Instance>> SERVICE_INSTANCE_LIST_MAP = Maps.newConcurrentMap();

    /**
     * serviceName -> corresponding serviceName list
     */
    private static final Map<String, Set<String>> CORRESPONDING_SERVICE_NAMES_MAP = Maps.newConcurrentMap();

    public static void setCorrespondingServiceNames(String serviceName, Set<String> serviceNames) {
        CORRESPONDING_SERVICE_NAMES_MAP.put(serviceName, serviceNames);
    }

    public static void initOrRefreshServiceInstanceList(String serviceName, List<Instance> instanceList) {
        SERVICE_INSTANCE_LIST_MAP.put(serviceName, instanceList);
    }

    public static List<Instance> getAllCorrespondingServiceInstanceList(String serviceName) {
        if (!CORRESPONDING_SERVICE_NAMES_MAP.containsKey(serviceName)) {
            return Lists.newArrayList();
        }
        List<Instance> allInstances = Lists.newArrayList();
        for (String correspondingServiceName : CORRESPONDING_SERVICE_NAMES_MAP.get(serviceName)) {
            if (SERVICE_INSTANCE_LIST_MAP.containsKey(correspondingServiceName)
                && CollectionUtils.isNotEmpty(SERVICE_INSTANCE_LIST_MAP.get(correspondingServiceName))) {
                allInstances.addAll(SERVICE_INSTANCE_LIST_MAP.get(correspondingServiceName));
            }
        }
        return allInstances;
    }

}
