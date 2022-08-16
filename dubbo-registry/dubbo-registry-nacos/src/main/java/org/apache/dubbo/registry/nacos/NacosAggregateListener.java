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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.registry.NotifyListener;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class NacosAggregateListener {
    private final NotifyListener notifyListener;
    private final Set<String> serviceNames = new ConcurrentHashSet<>();
    private final Map<String, List<URL>> serviceUrls = new ConcurrentHashMap<>();

    public NacosAggregateListener(NotifyListener notifyListener) {
        this.notifyListener = notifyListener;
    }

    public List<URL> getAggregatedUrls(String serviceName, List<URL> urls) {
        serviceNames.add(serviceName);
        serviceUrls.put(serviceName, urls);
        return serviceUrls.values().stream().flatMap(List::stream).collect(Collectors.toList());
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
        return Objects.equals(notifyListener, that.notifyListener) && Objects.equals(serviceNames, that.serviceNames) && Objects.equals(serviceUrls, that.serviceUrls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notifyListener, serviceNames, serviceUrls);
    }
}
