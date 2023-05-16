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

import org.apache.dubbo.rpc.cluster.router.xds.rule.ClusterWeight;
import org.apache.dubbo.rpc.cluster.router.xds.rule.XdsRouteRule;

import com.google.protobuf.BoolValue;
import com.google.protobuf.UInt32Value;
import io.envoyproxy.envoy.config.route.v3.HeaderMatcher;
import io.envoyproxy.envoy.config.route.v3.Route;
import io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.envoyproxy.envoy.config.route.v3.RouteMatch;
import io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.envoyproxy.envoy.config.route.v3.WeightedCluster;
import io.envoyproxy.envoy.type.matcher.v3.RegexMatcher;
import io.envoyproxy.envoy.type.v3.Int64Range;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RdsVirtualHostListenerTest {

    private final String domain = "testApp";

    private final Map<String, List<XdsRouteRule>> dataCache = new HashMap<>();

    private final XdsRouteRuleListener listener = new XdsRouteRuleListener() {
        @Override
        public void onRuleChange(String appName, List<XdsRouteRule> xdsRouteRules) {
            dataCache.put(appName, xdsRouteRules);
        }

        @Override
        public void clearRule(String appName) {
            dataCache.remove(appName);
        }
    };

    private final RdsRouteRuleManager manager = new RdsRouteRuleManager();

    private final RdsVirtualHostListener rdsVirtualHostListener = new RdsVirtualHostListener("testApp", manager);

    @BeforeEach
    public void init() {
        dataCache.clear();
        manager.subscribeRds(domain, listener);
    }


    @Test
    public void parsePathPathMatcherTest() {
        String path = "/test/name";
        VirtualHost virtualHost = VirtualHost.newBuilder()
            .addDomains(domain)
            .addRoutes(Route.newBuilder().setName("route-test")
                .setMatch(RouteMatch.newBuilder().setPath(path).setCaseSensitive(BoolValue.newBuilder().setValue(true).build()).build())
                .setRoute(RouteAction.newBuilder().setCluster("cluster-test").build())
                .build()
            ).build();
        rdsVirtualHostListener.parseVirtualHost(virtualHost);
        List<XdsRouteRule> rules = dataCache.get(domain);
        assertNotNull(rules);
        assertEquals(rules.get(0).getMatch().getPathMatcher().getPath(), path);
        assertTrue(rules.get(0).getMatch().getPathMatcher().isCaseSensitive());
    }

    @Test
    public void parsePrefixPathMatcherTest() {
        String prefix = "/test";
        VirtualHost virtualHost = VirtualHost.newBuilder()
            .addDomains(domain)
            .addRoutes(Route.newBuilder().setName("route-test")
                .setMatch(RouteMatch.newBuilder().setPrefix(prefix).build())
                .setRoute(RouteAction.newBuilder().setCluster("cluster-test").build())
                .build()
            )
            .build();
        rdsVirtualHostListener.parseVirtualHost(virtualHost);
        List<XdsRouteRule> rules = dataCache.get(domain);
        assertNotNull(rules);
        assertEquals(rules.get(0).getMatch().getPathMatcher().getPrefix(), prefix);
    }

    @Test
    public void parseRegexPathMatcherTest() {
        String regex = "/test/.*";
        VirtualHost virtualHost = VirtualHost.newBuilder()
            .addDomains(domain)
            .addRoutes(Route.newBuilder().setName("route-test")
                .setMatch(RouteMatch.newBuilder().setSafeRegex(
                    RegexMatcher.newBuilder().setRegex(regex).build()
                ).build())
                .setRoute(RouteAction.newBuilder().setCluster("cluster-test").build())
                .build()
            )
            .build();
        rdsVirtualHostListener.parseVirtualHost(virtualHost);
        List<XdsRouteRule> rules = dataCache.get(domain);
        assertNotNull(rules);
        assertEquals(rules.get(0).getMatch().getPathMatcher().getRegex(), regex);
    }

    @Test
    public void parseHeadMatcherTest() {
        VirtualHost virtualHost = VirtualHost.newBuilder()
            .addDomains(domain)
            .addRoutes(Route.newBuilder().setName("route-test")
                .setMatch(RouteMatch.newBuilder()
                    .addHeaders(HeaderMatcher.newBuilder()
                        .setName("head-exactValue")
                        .setExactMatch("exactValue")
                        .setInvertMatch(true)
                        .build())
                    .addHeaders(HeaderMatcher.newBuilder()
                        .setName("head-regex")
                        .setSafeRegexMatch(RegexMatcher.newBuilder().setRegex("regex").build())
                        .build())
                    .addHeaders(HeaderMatcher.newBuilder()
                        .setName("head-range")
                        .setRangeMatch(Int64Range.newBuilder().setStart(1).setEnd(100).build())
                        .build())
                    .addHeaders(HeaderMatcher.newBuilder()
                        .setName("head-present")
                        .setPresentMatch(true)
                        .build())
                    .addHeaders(HeaderMatcher.newBuilder()
                        .setName("head-prefix")
                        .setPrefixMatch("prefix")
                        .build())
                    .addHeaders(HeaderMatcher.newBuilder()
                        .setName("head-suffix")
                        .setSuffixMatch("suffix")
                        .build())
                    .build()
                )
                .setRoute(RouteAction.newBuilder().setCluster("cluster-test").build())
                .build()
            )
            .build();
        rdsVirtualHostListener.parseVirtualHost(virtualHost);
        List<XdsRouteRule> rules = dataCache.get(domain);
        assertNotNull(rules);
        List<org.apache.dubbo.rpc.cluster.router.xds.rule.HeaderMatcher> headerMatcherList = rules.get(0).getMatch().getHeaderMatcherList();
        for (org.apache.dubbo.rpc.cluster.router.xds.rule.HeaderMatcher matcher : headerMatcherList) {
            if (matcher.getName().equals("head-exactValue")) {
                assertEquals(matcher.getExactValue(), "exactValue");
            } else if (matcher.getName().equals("head-regex")) {
                assertEquals(matcher.getRegex(), "regex");
            } else if (matcher.getName().equals("head-range")) {
                assertEquals(matcher.getRange().getStart(), 1);
                assertEquals(matcher.getRange().getEnd(), 100);
            } else if (matcher.getName().equals("head-present")) {
                assertTrue(matcher.getPresent());
            } else if (matcher.getName().equals("head-prefix")) {
                assertEquals(matcher.getPrefix(), "prefix");
            } else if (matcher.getName().equals("head-suffix")) {
                assertEquals(matcher.getSuffix(), "suffix");
            }
        }
    }

    @Test
    public void parseRouteClusterTest() {
        String cluster = "cluster-test";
        VirtualHost virtualHost = VirtualHost.newBuilder()
            .addDomains(domain)
            .addRoutes(Route.newBuilder().setName("route-test")
                .setMatch(RouteMatch.newBuilder().setPrefix("/test").build())
                .setRoute(RouteAction.newBuilder().setCluster(cluster).build())
                .build()
            )
            .build();
        rdsVirtualHostListener.parseVirtualHost(virtualHost);
        List<XdsRouteRule> rules = dataCache.get(domain);
        assertNotNull(rules);
        assertEquals(rules.get(0).getRoute().getCluster(), cluster);
    }

    @Test
    public void parseRouteWeightClusterTest() {
        VirtualHost virtualHost = VirtualHost.newBuilder()
            .addDomains(domain)
            .addRoutes(Route.newBuilder().setName("route-test")
                .setMatch(RouteMatch.newBuilder().setPrefix("/test").build())
                .setRoute(RouteAction.newBuilder().setWeightedClusters(
                    WeightedCluster.newBuilder()
                        .addClusters(WeightedCluster.ClusterWeight.newBuilder().setName("cluster-test1")
                            .setWeight(UInt32Value.newBuilder().setValue(40).build()).build())
                        .addClusters(WeightedCluster.ClusterWeight.newBuilder().setName("cluster-test2")
                            .setWeight(UInt32Value.newBuilder().setValue(60).build()).build())
                        .build()
                ).build())
                .build())
            .build();
        rdsVirtualHostListener.parseVirtualHost(virtualHost);
        List<XdsRouteRule> rules = dataCache.get(domain);
        assertNotNull(rules);
        List<ClusterWeight> weightedClusters = rules.get(0).getRoute().getWeightedClusters();
        assertEquals(weightedClusters.size(), 2);
        for (ClusterWeight weightedCluster : weightedClusters) {
            if (weightedCluster.getName().equals("cluster-test1")) {
                assertEquals(weightedCluster.getWeight(), 40);
            } else if (weightedCluster.getName().equals("cluster-test2")) {
                assertEquals(weightedCluster.getWeight(), 60);
            }
        }
    }

}
