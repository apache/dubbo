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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.VsDestinationGroup;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.destination.DestinationRule;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.destination.DestinationRuleSpec;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.destination.Subset;
import org.apache.dubbo.rpc.cluster.router.state.BitList;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.apache.dubbo.rpc.cluster.router.mesh.route.MeshRuleConstants.INVALID_APP_NAME;

public class MeshRuleCache<T> {
    private final List<String> appList;
    private final Map<String, VsDestinationGroup> appToVDGroup;
    private final Map<String, Map<String, BitList<Invoker<T>>>> totalSubsetMap;
    private final BitList<Invoker<T>> unmatchedInvokers;

    private MeshRuleCache(List<String> appList, Map<String, VsDestinationGroup> appToVDGroup, Map<String, Map<String, BitList<Invoker<T>>>> totalSubsetMap, BitList<Invoker<T>> unmatchedInvokers) {
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

    public Map<String, Map<String, BitList<Invoker<T>>>> getTotalSubsetMap() {
        return totalSubsetMap;
    }

    public BitList<Invoker<T>> getUnmatchedInvokers() {
        return unmatchedInvokers;
    }

    public VsDestinationGroup getVsDestinationGroup(String appName) {
        return appToVDGroup.get(appName);
    }

    public BitList<Invoker<T>> getSubsetInvokers(String appName, String subset) {
        Map<String, BitList<Invoker<T>>> appToSubSets = totalSubsetMap.get(appName);
        if (CollectionUtils.isNotEmptyMap(appToSubSets)) {
            BitList<Invoker<T>> subsetInvokers = appToSubSets.get(subset);
            if (CollectionUtils.isNotEmpty(subsetInvokers)) {
                return subsetInvokers;
            }
        }
        return BitList.emptyList();
    }

    public boolean containsRule() {
        return !totalSubsetMap.isEmpty();
    }

    public static <T> MeshRuleCache<T> build(String protocolServiceKey, BitList<Invoker<T>> invokers, Map<String, VsDestinationGroup> vsDestinationGroupMap) {
        if (CollectionUtils.isNotEmptyMap(vsDestinationGroupMap)) {
            BitList<Invoker<T>> unmatchedInvokers = new BitList<>(invokers.getOriginList(), true);
            Map<String, Map<String, BitList<Invoker<T>>>> totalSubsetMap = new HashMap<>();

            for (Invoker<T> invoker : invokers) {
                String remoteApplication = invoker.getUrl().getRemoteApplication();
                if (StringUtils.isEmpty(remoteApplication) || INVALID_APP_NAME.equals(remoteApplication)) {
                    unmatchedInvokers.add(invoker);
                    continue;
                }
                VsDestinationGroup vsDestinationGroup = vsDestinationGroupMap.get(remoteApplication);
                if (vsDestinationGroup == null) {
                    unmatchedInvokers.add(invoker);
                    continue;
                }
                Map<String, BitList<Invoker<T>>> subsetMap = totalSubsetMap.computeIfAbsent(remoteApplication, (k) -> new HashMap<>());

                boolean matched = false;
                for (DestinationRule destinationRule : vsDestinationGroup.getDestinationRuleList()) {
                    DestinationRuleSpec destinationRuleSpec = destinationRule.getSpec();
                    List<Subset> subsetList = destinationRuleSpec.getSubsets();
                    for (Subset subset : subsetList) {
                        String subsetName = subset.getName();
                        List<Invoker<T>> subsetInvokers = subsetMap.computeIfAbsent(subsetName, (k) -> new BitList<>(invokers.getOriginList(), true));

                        Map<String, String> labels = subset.getLabels();
                        if (containMapKeyValue(invoker.getUrl().getServiceParameters(protocolServiceKey), labels)) {
                            subsetInvokers.add(invoker);
                            matched = true;
                        }
                    }
                }
                if (!matched) {
                    unmatchedInvokers.add(invoker);
                }
            }

            return new MeshRuleCache<>(new LinkedList<>(vsDestinationGroupMap.keySet()),
                Collections.unmodifiableMap(vsDestinationGroupMap),
                Collections.unmodifiableMap(totalSubsetMap),
                unmatchedInvokers);
        } else {
            return new MeshRuleCache<T>(Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap(), invokers);
        }
    }

    public static <T> MeshRuleCache<T> emptyCache() {
        return new MeshRuleCache<>(Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap(), BitList.emptyList());
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MeshRuleCache<?> ruleCache = (MeshRuleCache<?>) o;
        return Objects.equals(appList, ruleCache.appList) && Objects.equals(appToVDGroup, ruleCache.appToVDGroup) && Objects.equals(totalSubsetMap, ruleCache.totalSubsetMap) && Objects.equals(unmatchedInvokers, ruleCache.unmatchedInvokers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appList, appToVDGroup, totalSubsetMap, unmatchedInvokers);
    }
}
