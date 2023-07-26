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
package org.apache.dubbo.rpc.cluster.router.xds;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.registry.xds.util.PilotExchanger;
import org.apache.dubbo.registry.xds.util.protocol.message.Endpoint;
import org.apache.dubbo.registry.xds.util.protocol.message.EndpointResult;
import org.apache.dubbo.rpc.model.FrameworkModel;

public class EdsEndpointManager {

    private static final ConcurrentHashMap<String, Set<EdsEndpointListener>> ENDPOINT_LISTENERS = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, Set<Endpoint>> ENDPOINT_DATA_CACHE = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, Consumer<Map<String, EndpointResult>>> EDS_LISTENERS = new ConcurrentHashMap<>();


    public EdsEndpointManager() {
    }

    public synchronized void subscribeEds(String cluster, EdsEndpointListener listener) {

        Set<EdsEndpointListener> listeners = ConcurrentHashMapUtils.computeIfAbsent(ENDPOINT_LISTENERS, cluster, key ->
            new ConcurrentHashSet<>()
        );
        if (CollectionUtils.isEmpty(listeners)) {
            doSubscribeEds(cluster);
        }
        listeners.add(listener);

        if (ENDPOINT_DATA_CACHE.containsKey(cluster)) {
            listener.onEndPointChange(cluster, ENDPOINT_DATA_CACHE.get(cluster));
        }
    }

    private void doSubscribeEds(String cluster) {
        ConcurrentHashMapUtils.computeIfAbsent(EDS_LISTENERS, cluster, key -> endpoints -> {
            Set<Endpoint> result = endpoints.values()
                .stream()
                .map(EndpointResult::getEndpoints)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
            notifyEndpointChange(cluster, result);
        });
        Consumer<Map<String, EndpointResult>> consumer = EDS_LISTENERS.get(cluster);
        if (PilotExchanger.isEnabled()) {
            FrameworkModel.defaultModel().getBeanFactory().getBean(FrameworkExecutorRepository.class)
                .getSharedExecutor().submit(() -> PilotExchanger.getInstance().observeEds(Collections.singleton(cluster), consumer));
        }
    }

    public synchronized void unSubscribeEds(String cluster, EdsEndpointListener listener) {
        Set<EdsEndpointListener> listeners = ENDPOINT_LISTENERS.get(cluster);
        if (CollectionUtils.isEmpty(listeners)) {
            return;
        }
        listeners.remove(listener);
        if (listeners.isEmpty()) {
            ENDPOINT_LISTENERS.remove(cluster);
            doUnsubscribeEds(cluster);
        }
    }

    private void doUnsubscribeEds(String cluster) {
        Consumer<Map<String, EndpointResult>> consumer = EDS_LISTENERS.remove(cluster);

        if (consumer != null && PilotExchanger.isEnabled()) {
            PilotExchanger.getInstance().unObserveEds(Collections.singleton(cluster), consumer);
        }
        ENDPOINT_DATA_CACHE.remove(cluster);
    }


    public void notifyEndpointChange(String cluster, Set<Endpoint> endpoints) {

        ENDPOINT_DATA_CACHE.put(cluster, endpoints);

        Set<EdsEndpointListener> listeners = ENDPOINT_LISTENERS.get(cluster);
        if (CollectionUtils.isEmpty(listeners)) {
            return;
        }
        for (EdsEndpointListener listener : listeners) {
            listener.onEndPointChange(cluster, endpoints);
        }
    }

    // for test
    static ConcurrentHashMap<String, Set<EdsEndpointListener>> getEndpointListeners() {
        return ENDPOINT_LISTENERS;
    }

    // for test
    static ConcurrentHashMap<String, Set<Endpoint>> getEndpointDataCache() {
        return ENDPOINT_DATA_CACHE;
    }

    // for test
    static ConcurrentHashMap<String, Consumer<Map<String, EndpointResult>>> getEdsListeners() {
        return EDS_LISTENERS;
    }

}
