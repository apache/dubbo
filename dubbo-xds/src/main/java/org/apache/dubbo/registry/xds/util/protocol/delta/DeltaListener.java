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
package org.apache.dubbo.registry.xds.util.protocol.delta;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.registry.xds.util.protocol.DeltaResource;
import org.apache.dubbo.registry.xds.util.protocol.message.ListenerResult;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DeltaListener implements DeltaResource<ListenerResult> {
    private final Map<String, Set<String>> data = new ConcurrentHashMap<>();

    public void addResource(String resourceName, Set<String> listeners) {
        data.put(resourceName, listeners);
    }

    public void removeResource(Collection<String> resourceName) {
        if (CollectionUtils.isNotEmpty(resourceName)) {
            resourceName.forEach(data::remove);
        }
    }

    @Override
    public ListenerResult getResource() {
        Set<String> set = data.values().stream()
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
        return new ListenerResult(set);
    }
}
