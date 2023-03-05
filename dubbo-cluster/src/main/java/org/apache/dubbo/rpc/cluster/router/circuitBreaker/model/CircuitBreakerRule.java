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
package org.apache.dubbo.rpc.cluster.router.circuitBreaker.model;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.router.AbstractRouterRule;
import org.apache.dubbo.rpc.cluster.router.circuitBreaker.CircuitBreakerRouter;
import org.apache.dubbo.rpc.cluster.router.state.BitList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.dubbo.rpc.cluster.Constants.CIRCUIT_BREAKER_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.RULE_VERSION_V30;

/**
 * %YAML1.2
 * ---
 * force: true
 * runtime: false
 * enabled: true
 * priority: 1
 * key: demo-provider
 * tags:
 * - name: tag1
 * addresses: [ip1, ip2]
 * - name: tag2
 * addresses: [ip3, ip4]
 * ...
 */
public class CircuitBreakerRule extends AbstractRouterRule {
    
    private List<CircuitBreaker> circuitBreakers;

    private final Map<String, Set<String>> addressToCircuitBreakernames = new HashMap<>();
    private final Map<String, Set<String>> circuitBreakernameToAddresses = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static CircuitBreakerRule parseFromMap(Map<String, Object> map) {
        CircuitBreakerRule tagRouterRule = new CircuitBreakerRule();
        tagRouterRule.parseFromMap0(map);

        Object circuitBreakers = map.get(CIRCUIT_BREAKER_KEY);
        if (circuitBreakers != null && List.class.isAssignableFrom(circuitBreakers.getClass())) {
            tagRouterRule.setTags(((List<Map<String, Object>>) circuitBreakers).stream()
                .map(objMap -> CircuitBreaker.parseFromMap(objMap, tagRouterRule.getVersion())).collect(Collectors.toList()));
        }

        return tagRouterRule;
    }

    public void init(CircuitBreakerRouter<?> router) {
        if (!isValid()) {
            return;
        }

        BitList<? extends Invoker<?>> invokers = router.getInvokers();

        // for tags with 'addresses` field set and 'match' field not set
        circuitBreakers.stream().filter(circuitBreaker -> CollectionUtils.isNotEmpty(circuitBreaker.getAddresses())).forEach(circuitBreaker -> {
            circuitBreakernameToAddresses.put(circuitBreaker.getName(), new HashSet<>(circuitBreaker.getAddresses()));
            circuitBreaker.getAddresses().forEach(addr -> {
                Set<String> tagNames = addressToCircuitBreakernames.computeIfAbsent(addr, k -> new HashSet<>());
                tagNames.add(circuitBreaker.getName());
            });
        });

        if (this.getVersion() != null && this.getVersion().startsWith(RULE_VERSION_V30)) {
            // for tags with 'match` field set and 'addresses' field not set
            if (CollectionUtils.isNotEmpty(invokers)) {
                circuitBreakers.stream().filter(circuitBreaker -> CollectionUtils.isEmpty(circuitBreaker.getAddresses())).forEach(circuitBreaker -> {
                    Set<String> addresses = new HashSet<>();
                    List<ParamMatch> paramMatchers = circuitBreaker.getMatch();
                    invokers.forEach(invoker -> {
                        boolean isMatch = true;
                        for (ParamMatch matcher : paramMatchers) {
                            if (!matcher.isMatch(invoker.getUrl().getOriginalParameter(matcher.getKey()))) {
                                isMatch = false;
                                break;
                            }
                        }
                        if (isMatch) {
                            addresses.add(invoker.getUrl().getAddress());
                        }
                    });
                    if (CollectionUtils.isNotEmpty(addresses)) {// null means tag not set
                        circuitBreakernameToAddresses.put(circuitBreaker.getName(), addresses);
                    }
                });
            }
        }
    }

    public Set<String> getAddresses() {
        return circuitBreakernameToAddresses.entrySet().stream()
            .filter(entry -> CollectionUtils.isNotEmpty(entry.getValue()))
            .flatMap(entry -> entry.getValue().stream())
            .collect(Collectors.toSet());
    }

//    public List<String> getTagNames() {
//        return circuitBreakers.stream().map(CircuitBreaker::getName).collect(Collectors.toList());
//    }

    public List<String> getCircuitBreakerNames() {
        return circuitBreakers.stream().map(CircuitBreaker::getName).collect(Collectors.toList());
    }

    public Map<String, Set<String>> getAddressToCircuitBreakernames() {
        return addressToCircuitBreakernames;
    }


    public Map<String, Set<String>> getCircuitBreakernameToAddresses() {
        return circuitBreakernameToAddresses;
    }

    public List<CircuitBreaker> getCircuitBreakers() {
        return circuitBreakers;
    }

    public void setTags(List<CircuitBreaker> circuitBreakers) {
        this.circuitBreakers = circuitBreakers;
    }
}
