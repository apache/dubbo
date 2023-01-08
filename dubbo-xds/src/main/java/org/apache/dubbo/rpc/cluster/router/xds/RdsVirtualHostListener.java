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

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.cluster.router.xds.rule.ClusterWeight;
import org.apache.dubbo.rpc.cluster.router.xds.rule.HTTPRouteDestination;
import org.apache.dubbo.rpc.cluster.router.xds.rule.HeaderMatcher;
import org.apache.dubbo.rpc.cluster.router.xds.rule.HttpRequestMatch;
import org.apache.dubbo.rpc.cluster.router.xds.rule.LongRangeMatch;
import org.apache.dubbo.rpc.cluster.router.xds.rule.PathMatcher;
import org.apache.dubbo.rpc.cluster.router.xds.rule.XdsRouteRule;

import io.envoyproxy.envoy.config.route.v3.Route;
import io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.envoyproxy.envoy.config.route.v3.RouteMatch;
import io.envoyproxy.envoy.config.route.v3.VirtualHost;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RdsVirtualHostListener {

    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(RdsVirtualHostListener.class);

    private final String domain;

    private final RdsRouteRuleManager routeRuleManager;


    public RdsVirtualHostListener(String domain, RdsRouteRuleManager routeRuleManager) {
        this.domain = domain;
        this.routeRuleManager = routeRuleManager;
    }

    public void parseVirtualHost(VirtualHost virtualHost) {
        if (virtualHost == null || CollectionUtils.isEmpty(virtualHost.getRoutesList())) {
            // post empty
            routeRuleManager.notifyRuleChange(domain, new ArrayList<>());
            return;
        }
        try {
            List<XdsRouteRule> xdsRouteRules = virtualHost.getRoutesList().stream().map(route -> {
                if (route.getMatch().getQueryParametersCount() != 0) {
                    return null;
                }
                HttpRequestMatch match = parseMatch(route.getMatch());
                HTTPRouteDestination action = parseAction(route);
                return new XdsRouteRule(match, action);
            }).filter(Objects::nonNull).collect(Collectors.toList());
            // post rules
            routeRuleManager.notifyRuleChange(domain, xdsRouteRules);
        } catch (Exception e) {
            LOGGER.error(LoggerCodeConstants.INTERNAL_ERROR, "", "", "parse domain: " + domain + " xds VirtualHost error", e);
        }

    }

    private HttpRequestMatch parseMatch(RouteMatch match) {
        PathMatcher pathMatcher = parsePathMatch(match);
        List<HeaderMatcher> headerMatchers = parseHeadMatch(match);
        return new HttpRequestMatch(pathMatcher, headerMatchers);
    }

    private PathMatcher parsePathMatch(RouteMatch match) {
        boolean caseSensitive = match.getCaseSensitive().getValue();
        PathMatcher pathMatcher = new PathMatcher();
        pathMatcher.setCaseSensitive(caseSensitive);
        switch (match.getPathSpecifierCase()) {
            case PREFIX:
                pathMatcher.setPrefix(match.getPrefix());
                return pathMatcher;
            case PATH:
                pathMatcher.setPath(match.getPath());
                return pathMatcher;
            case SAFE_REGEX:
                String regex = match.getSafeRegex().getRegex();
                pathMatcher.setRegex(regex);
                return pathMatcher;
            case PATHSPECIFIER_NOT_SET:
                return null;
            default:
                throw new IllegalArgumentException("Path specifier is not expect");
        }
    }

    private List<HeaderMatcher> parseHeadMatch(RouteMatch routeMatch) {
        List<HeaderMatcher> headerMatchers = new ArrayList<>();
        List<io.envoyproxy.envoy.config.route.v3.HeaderMatcher> headersList = routeMatch.getHeadersList();
        for (io.envoyproxy.envoy.config.route.v3.HeaderMatcher headerMatcher : headersList) {
            HeaderMatcher matcher = new HeaderMatcher();
            matcher.setName(headerMatcher.getName());
            matcher.setInverted(headerMatcher.getInvertMatch());
            switch (headerMatcher.getHeaderMatchSpecifierCase()) {
                case EXACT_MATCH:
                    matcher.setExactValue(headerMatcher.getExactMatch());
                    headerMatchers.add(matcher);
                    break;
                case SAFE_REGEX_MATCH:
                    matcher.setRegex(headerMatcher.getSafeRegexMatch().getRegex());
                    headerMatchers.add(matcher);
                    break;
                case RANGE_MATCH:
                    LongRangeMatch rang = new LongRangeMatch();
                    rang.setStart(headerMatcher.getRangeMatch().getStart());
                    rang.setEnd(headerMatcher.getRangeMatch().getEnd());
                    matcher.setRange(rang);
                    headerMatchers.add(matcher);
                    break;
                case PRESENT_MATCH:
                    matcher.setPresent(headerMatcher.getPresentMatch());
                    headerMatchers.add(matcher);
                    break;
                case PREFIX_MATCH:
                    matcher.setPrefix(headerMatcher.getPrefixMatch());
                    headerMatchers.add(matcher);
                    break;
                case SUFFIX_MATCH:
                    matcher.setSuffix(headerMatcher.getSuffixMatch());
                    headerMatchers.add(matcher);
                    break;
                case HEADERMATCHSPECIFIER_NOT_SET:
                default:
                    throw new IllegalArgumentException("Header specifier is not expect");
            }
        }
        return headerMatchers;
    }

    private HTTPRouteDestination parseAction(Route route) {
        switch (route.getActionCase()) {
            case ROUTE:
                HTTPRouteDestination httpRouteDestination = new HTTPRouteDestination();
                // only support cluster and weight cluster
                RouteAction routeAction = route.getRoute();
                RouteAction.ClusterSpecifierCase clusterSpecifierCase = routeAction.getClusterSpecifierCase();
                if (clusterSpecifierCase == RouteAction.ClusterSpecifierCase.CLUSTER) {
                    httpRouteDestination.setCluster(routeAction.getCluster());
                    return httpRouteDestination;
                } else if (clusterSpecifierCase == RouteAction.ClusterSpecifierCase.WEIGHTED_CLUSTERS) {
                    List<ClusterWeight> clusterWeights = routeAction.getWeightedClusters().
                        getClustersList().stream()
                        .map(c -> new ClusterWeight(c.getName(), c.getWeight().getValue()))
                        .sorted(Comparator.comparing(ClusterWeight::getWeight))
                        .collect(Collectors.toList());
                    httpRouteDestination.setWeightedClusters(clusterWeights);
                    return httpRouteDestination;
                }
            case REDIRECT:
            case DIRECT_RESPONSE:
            case FILTER_ACTION:
            case ACTION_NOT_SET:
            default:
                throw new IllegalArgumentException("Cluster specifier is not expect");
        }
    }

}
