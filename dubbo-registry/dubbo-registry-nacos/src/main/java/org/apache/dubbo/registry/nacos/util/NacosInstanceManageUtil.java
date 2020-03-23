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

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.dubbo.common.utils.CollectionUtils;

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
    private static final Map<String, List<Instance>> serviceInstanceListMap = Maps.newHashMap();

    /**
     * serviceName -> corresponding serviceName list
     */
    private static final Map<String, Set<String>> correspondingServiceNamesMap = Maps.newHashMap();

    public static void setCorrespondingServiceNames(String serviceName, Set<String> serviceNames) {
        correspondingServiceNamesMap.put(serviceName, serviceNames);
    }

    public static void initOrRefreshServiceInstanceList(String serviceName, List<Instance> instanceList) {
        serviceInstanceListMap.put(serviceName, instanceList);
    }

    public static List<Instance> getAllCorrespondingServiceInstanceList(String serviceName) {
        if (!correspondingServiceNamesMap.containsKey(serviceName)) {
            return Lists.newArrayList();
        }
        List<Instance> allInstances = Lists.newArrayList();
        for (String correspondingServiceName : correspondingServiceNamesMap.get(serviceName)) {
            if (serviceInstanceListMap.containsKey(correspondingServiceName) && CollectionUtils.isNotEmpty(serviceInstanceListMap.get(correspondingServiceName))) {
                allInstances.addAll(serviceInstanceListMap.get(correspondingServiceName));
            }
        }
        return allInstances;
    }

}
