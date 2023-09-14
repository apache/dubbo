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
package org.apache.dubbo.registry.nacos;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.registry.NotifyListener;

import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_NACOS_SUB_LEGACY;

public class NacosAggregateListener {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(NacosAggregateListener.class);
    private final NotifyListener notifyListener;
    private final Set<String> serviceNames = new ConcurrentHashSet<>();
    private final Map<String, List<Instance>> serviceInstances = new ConcurrentHashMap<>();
    private final AtomicBoolean warned = new AtomicBoolean(false);
    private static final Pattern SPLITTED_PATTERN = Pattern.compile(".*:.*:.*:.*");

    public NacosAggregateListener(NotifyListener notifyListener) {
        this.notifyListener = notifyListener;
    }

    public List<Instance> saveAndAggregateAllInstances(String serviceName, List<Instance> instances) {
        serviceNames.add(serviceName);
        if (instances == null) {
            serviceInstances.remove(serviceName);
        } else {
            serviceInstances.put(serviceName, instances);
        }
        if (isLegacyName(serviceName) && instances != null &&
            !instances.isEmpty() && warned.compareAndSet(false, true)) {
            logger.error(REGISTRY_NACOS_SUB_LEGACY, "", "",
                "Received not empty notification for legacy service name: " + serviceName + ", " +
                "instances: [" +  instances.stream().map(Instance::getIp).collect(Collectors.joining(" ,")) + "]. " +
                "Please upgrade these Dubbo client(lower than 2.7.3) to the latest version. " +
                "Dubbo will remove the support for legacy service name in the future.");
        }
        return serviceInstances.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }

    private static boolean isLegacyName(String serviceName) {
        return !SPLITTED_PATTERN.matcher(serviceName).matches();
    }

    public NotifyListener getNotifyListener() {
        return notifyListener;
    }

    public Set<String> getServiceNames() {
        return serviceNames;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NacosAggregateListener that = (NacosAggregateListener) o;
        return Objects.equals(notifyListener, that.notifyListener) && Objects.equals(serviceNames, that.serviceNames) && Objects.equals(serviceInstances, that.serviceInstances);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notifyListener, serviceNames, serviceInstances);
    }
}
