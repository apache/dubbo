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
package org.apache.dubbo.metrices;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

public class MetricsBucket {

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, LinkedList<Metrics>>> store;

    public MetricsBucket() {
        this.store = new ConcurrentHashMap<>();
    }

    public void insert(String uniqueInterfaceName, String methodName, Metrics metrics) {
        if (!store.containsKey(uniqueInterfaceName)) {
            store.put(uniqueInterfaceName, new ConcurrentHashMap<>());
        }
        ConcurrentHashMap<String, LinkedList<Metrics>> interfaceStore =
            store.getOrDefault(uniqueInterfaceName, new ConcurrentHashMap<>());
        if (!interfaceStore.containsKey(methodName)) {
            interfaceStore.put(methodName, new LinkedList<>());
        }
        LinkedList<Metrics> list = interfaceStore.get(methodName);
        list.add(metrics);
    }

    public LinkedList<Metrics> getStore(String uniqueInterfaceName, String methodName) {
        ConcurrentHashMap<String, LinkedList<Metrics>> interfaceStore =
            store.getOrDefault(uniqueInterfaceName, new ConcurrentHashMap<>());
        return interfaceStore.getOrDefault(methodName, new LinkedList<>());
    }
}
