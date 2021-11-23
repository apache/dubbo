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

package org.apache.dubbo.rpc.cluster.router.mesh.util;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class MeshRuleDispatcher {
    public static final Logger logger = LoggerFactory.getLogger(MeshRuleDispatcher.class);

    private final String appName;
    private final Map<String, List<VsDestinationGroupRuleListener>> listenerMap = new ConcurrentHashMap<>();

    public MeshRuleDispatcher(String appName) {
        this.appName = appName;
    }


    public synchronized void post(Map<String, List<Map<String, Object>>> ruleMap) {
        for (Map.Entry<String, List<Map<String, Object>>> entry : ruleMap.entrySet()) {
            String ruleType = entry.getKey();
            List<VsDestinationGroupRuleListener> listeners = listenerMap.get(ruleType);
            if (CollectionUtils.isNotEmpty(listeners)) {
                for (VsDestinationGroupRuleListener listener : listeners) {
                    listener.onRuleChange(appName, entry.getValue());
                }
            } else {
                logger.warn("");
            }
        }
    }

    public synchronized boolean register(VsDestinationGroupRuleListener listener) {
        if (listener == null) {
            return false;
        }
        return listenerMap.computeIfAbsent(listener.ruleSuffix(),
                (k) -> new CopyOnWriteArrayList<>())
            .add(listener);
    }

    public synchronized void unregister(VsDestinationGroupRuleListener listener) {
        if (listener == null) {
            return;
        }
        List<VsDestinationGroupRuleListener> listeners = listenerMap.get(listener.ruleSuffix());
        if (CollectionUtils.isNotEmpty(listeners)) {
            listeners.remove(listener);
        }
    }

    public boolean isEmpty() {
        return listenerMap.isEmpty();
    }
}
