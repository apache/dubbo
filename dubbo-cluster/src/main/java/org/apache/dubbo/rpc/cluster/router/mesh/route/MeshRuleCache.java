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
package org.apache.dubbo.rpc.cluster.router.mesh.route;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.VsDestinationGroup;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.destination.DestinationRule;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.destination.DestinationRuleSpec;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.destination.Subset;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MeshRuleCache {
    private final List<String> appList;
    private final Map<String, VsDestinationGroup> appToVDGroup;
    private final Map<String, Map<String, List<Invoker<?>>>> totalSubsetMap;
    private final List<Invoker<?>> unmatchedInvokers;

    private MeshRuleCache(List<String> appList, Map<String, VsDestinationGroup> appToVDGroup, Map<String, Map<String, List<Invoker<?>>>> totalSubsetMap, List<Invoker<?>> unmatchedInvokers) {
        this.appList = appList;
        this.appToVDGroup = appToVDGroup;
        this.totalSubsetMap = totalSubsetMap;
        this.unmatchedInvokers = unmatchedInvokers;
    }

    public List<String> getAppList() {
        return appList;
    }

    public Map<String, VsDestinationGroup> getAppToVDGroup() {
        return appToVDGroup;
    }

    public Map<String, Map<String, List<Invoker<?>>>> getTotalSubsetMap() {
        return totalSubsetMap;
    }

    public List<Invoker<?>> getUnmatchedInvokers() {
        return unmatchedInvokers;
    }

    public VsDestinationGroup getVsDestinationGroup(String appName) {
        return appToVDGroup.get(appName);
    }

    public List<Invoker<?>> getSubsetInvokers(String appName, String subset) {
        Map<String, List<Invoker<?>>> appToSubSets = totalSubsetMap.get(appName);
        if (CollectionUtils.isNotEmptyMap(appToSubSets)) {
            List<Invoker<?>> subsetInvokers = appToSubSets.get(subset);
            if (CollectionUtils.isNotEmpty(subsetInvokers)) {
                return subsetInvokers;
            }
        }
        return new LinkedList<>();
    }

    public boolean containsRule() {
        return !totalSubsetMap.isEmpty();
    }

    public static MeshRuleCache build(String protocolServiceKey, List<Invoker<?>> invokers, Map<String, VsDestinationGroup> vsDestinationGroupMap) {
        if (CollectionUtils.isNotEmptyMap(vsDestinationGroupMap)) {
            List<Invoker<?>> unmatchedInvokers = new LinkedList<>();
            Map<String, Map<String, List<Invoker<?>>>> totalSubsetMap = new HashMap<>();

            for (Invoker<?> invoker : invokers) {
                String remoteApplication = invoker.getUrl().getRemoteApplication();
                VsDestinationGroup vsDestinationGroup = vsDestinationGroupMap.get(remoteApplication);
                if (vsDestinationGroup == null) {
                    unmatchedInvokers.add(invoker);
                    continue;
                }
                Map<String, List<Invoker<?>>> subsetMap = totalSubsetMap.computeIfAbsent(remoteApplication, (k) -> new HashMap<>());

                for (DestinationRule destinationRule : vsDestinationGroup.getDestinationRuleList()) {
                    DestinationRuleSpec destinationRuleSpec = destinationRule.getSpec();
                    List<Subset> subsetList = destinationRuleSpec.getSubsets();
                    for (Subset subset : subsetList) {
                        String subsetName = subset.getName();
                        List<Invoker<?>> subsetInvokers = subsetMap.computeIfAbsent(subsetName, (k) -> new LinkedList<>());

                        Map<String, String> labels = subset.getLabels();
                        if (containMapKeyValue(invoker.getUrl().getServiceParameters(protocolServiceKey), labels)) {
                            subsetInvokers.add(invoker);
                        }
                    }
                }
            }

            return new MeshRuleCache(new LinkedList<>(vsDestinationGroupMap.keySet()),
                Collections.unmodifiableMap(vsDestinationGroupMap),
                Collections.unmodifiableMap(totalSubsetMap),
                unmatchedInvokers);
        } else {
            return new MeshRuleCache(Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap(), invokers);
        }
    }

    public static MeshRuleCache emptyCache() {
        return new MeshRuleCache(Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList());
    }

    protected static boolean containMapKeyValue(Map<String, String> originMap, Map<String, String> inputMap) {
        if (inputMap == null || inputMap.size() == 0) {
            return true;
        }

        for (Map.Entry<String, String> entry : inputMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            String originMapValue = originMap.get(key);
            if (!value.equals(originMapValue)) {
                return false;
            }
        }

        return true;
    }

}
