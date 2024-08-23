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
package org.apache.dubbo.rpc.cluster.router.condition;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotNode;
import org.apache.dubbo.rpc.cluster.router.condition.config.model.ConditionSubSet;
import org.apache.dubbo.rpc.cluster.router.condition.config.model.DestinationSet;
import org.apache.dubbo.rpc.cluster.router.condition.config.model.MultiDestCondition;
import org.apache.dubbo.rpc.cluster.router.condition.matcher.ConditionMatcher;
import org.apache.dubbo.rpc.cluster.router.condition.matcher.ConditionMatcherFactory;
import org.apache.dubbo.rpc.cluster.router.state.AbstractStateRouter;
import org.apache.dubbo.rpc.cluster.router.state.BitList;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CLUSTER_CONDITIONAL_ROUTE_LIST_EMPTY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CLUSTER_FAILED_EXEC_CONDITION_ROUTER;
import static org.apache.dubbo.rpc.cluster.Constants.DefaultRouteConditionSubSetWeight;
import static org.apache.dubbo.rpc.cluster.Constants.RULE_KEY;

public class MultiDestConditionRouter<T> extends AbstractStateRouter<T> {
    public static final String NAME = "multi_condition";

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AbstractStateRouter.class);
    protected static final Pattern ROUTE_PATTERN = Pattern.compile("([&!=,]*)\\s*([^&!=,\\s]+)");
    private Map<String, ConditionMatcher> whenCondition;
    private List<ConditionSubSet> thenCondition;
    private boolean force;
    protected List<ConditionMatcherFactory> matcherFactories;
    private boolean enabled;

    public MultiDestConditionRouter(URL url, MultiDestCondition multiDestCondition, boolean force, boolean enabled) {
        super(url);
        this.setForce(force);
        this.enabled = enabled;
        matcherFactories =
                moduleModel.getExtensionLoader(ConditionMatcherFactory.class).getActivateExtensions();
        this.init(multiDestCondition.getFrom(), multiDestCondition.getTo());
    }

    public void init(Map<String, String> from, List<Map<String, String>> to) {
        try {
            if (from == null || to == null) {
                throw new IllegalArgumentException("Illegal route rule!");
            }
            String whenRule = from.get("match");
            Map<String, ConditionMatcher> when =
                    StringUtils.isBlank(whenRule) || "true".equals(whenRule) ? new HashMap<>() : parseRule(whenRule);
            this.whenCondition = when;

            List<ConditionSubSet> thenConditions = new ArrayList<>();
            for (Map<String, String> toMap : to) {
                String thenRule = toMap.get("match");
                Map<String, ConditionMatcher> then = StringUtils.isBlank(thenRule) || "false".equals(thenRule)
                        ? new HashMap<>()
                        : parseRule(thenRule);
                // NOTE: It should be determined on the business level whether the `When condition` can be empty or not.

                thenConditions.add(new ConditionSubSet(
                        then,
                        Integer.valueOf(
                                toMap.getOrDefault("weight", String.valueOf(DefaultRouteConditionSubSetWeight)))));
            }
            this.thenCondition = thenConditions;
        } catch (ParseException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private Map<String, ConditionMatcher> parseRule(String rule) throws ParseException {
        Map<String, ConditionMatcher> condition = new HashMap<>();
        if (StringUtils.isBlank(rule)) {
            return condition;
        }
        // Key-Value pair, stores both match and mismatch conditions
        ConditionMatcher matcherPair = null;
        // Multiple values
        Set<String> values = null;
        final Matcher matcher = ROUTE_PATTERN.matcher(rule);
        while (matcher.find()) { // Try to match one by one
            String separator = matcher.group(1);
            String content = matcher.group(2);
            // Start part of the condition expression.
            if (StringUtils.isEmpty(separator)) {
                matcherPair = this.getMatcher(content);
                condition.put(content, matcherPair);
            }
            // The KV part of the condition expression
            else if ("&".equals(separator)) {
                if (condition.get(content) == null) {
                    matcherPair = this.getMatcher(content);
                    condition.put(content, matcherPair);
                } else {
                    matcherPair = condition.get(content);
                }
            }
            // The Value in the KV part.
            else if ("=".equals(separator)) {
                if (matcherPair == null) {
                    throw new ParseException(
                            "Illegal route rule \"" + rule + "\", The error char '" + separator + "' at index "
                                    + matcher.start() + " before \"" + content + "\".",
                            matcher.start());
                }

                values = matcherPair.getMatches();
                values.add(content);
            }
            // The Value in the KV part.
            else if ("!=".equals(separator)) {
                if (matcherPair == null) {
                    throw new ParseException(
                            "Illegal route rule \"" + rule + "\", The error char '" + separator + "' at index "
                                    + matcher.start() + " before \"" + content + "\".",
                            matcher.start());
                }

                values = matcherPair.getMismatches();
                values.add(content);
            }
            // The Value in the KV part, if Value have more than one items.
            else if (",".equals(separator)) { // Should be separated by ','
                if (values == null || values.isEmpty()) {
                    throw new ParseException(
                            "Illegal route rule \"" + rule + "\", The error char '" + separator + "' at index "
                                    + matcher.start() + " before \"" + content + "\".",
                            matcher.start());
                }
                values.add(content);
            } else {
                throw new ParseException(
                        "Illegal route rule \"" + rule + "\", The error char '" + separator + "' at index "
                                + matcher.start() + " before \"" + content + "\".",
                        matcher.start());
            }
        }
        return condition;
    }

    private ConditionMatcher getMatcher(String key) {
        for (ConditionMatcherFactory factory : matcherFactories) {
            if (factory.shouldMatch(key)) {
                return factory.createMatcher(key, moduleModel);
            }
        }
        return moduleModel
                .getExtensionLoader(ConditionMatcherFactory.class)
                .getExtension("param")
                .createMatcher(key, moduleModel);
    }

    @Override
    protected BitList<Invoker<T>> doRoute(
            BitList<Invoker<T>> invokers,
            URL url,
            Invocation invocation,
            boolean needToPrintMessage,
            Holder<RouterSnapshotNode<T>> routerSnapshotNodeHolder,
            Holder<String> messageHolder)
            throws RpcException {

        if (!enabled) {
            if (needToPrintMessage) {
                messageHolder.set("Directly return. Reason: ConditionRouter disabled.");
            }
            return invokers;
        }

        if (CollectionUtils.isEmpty(invokers)) {
            if (needToPrintMessage) {
                messageHolder.set("Directly return. Reason: Invokers from previous router is empty.");
            }
            return invokers;
        }

        try {
            if (!matchWhen(url, invocation)) {
                if (needToPrintMessage) {
                    messageHolder.set("Directly return. Reason: WhenCondition not match.");
                }
                return invokers;
            }
            if (thenCondition == null || thenCondition.size() == 0) {
                logger.warn(
                        CLUSTER_CONDITIONAL_ROUTE_LIST_EMPTY,
                        "condition state router thenCondition is empty",
                        "",
                        "The current consumer in the service blocklist. consumer: " + NetUtils.getLocalHost()
                                + ", service: " + url.getServiceKey());
                if (needToPrintMessage) {
                    messageHolder.set("Empty return. Reason: ThenCondition is empty.");
                }
                return BitList.emptyList();
            }

            DestinationSet destinations = new DestinationSet();
            for (ConditionSubSet condition : thenCondition) {
                BitList<Invoker<T>> res = invokers.clone();

                for (Invoker invoker : invokers) {
                    if (!doMatch(invoker.getUrl(), url, null, condition.getCondition(), false)) {
                        res.remove(invoker);
                    }
                }
                if (!res.isEmpty()) {
                    destinations.addDestination(
                            condition.getSubSetWeight() == null
                                    ? DefaultRouteConditionSubSetWeight
                                    : condition.getSubSetWeight(),
                            res.clone());
                }
            }

            if (!destinations.getDestinations().isEmpty()) {
                BitList<Invoker<T>> res = destinations.randDestination();
                return res;
            } else if (this.isForce()) {
                logger.warn(
                        CLUSTER_CONDITIONAL_ROUTE_LIST_EMPTY,
                        "execute condition state router result list is " + "empty. and force=true",
                        "",
                        "The route result is empty and force execute. consumer: " + NetUtils.getLocalHost()
                                + ", service: " + url.getServiceKey() + ", router: "
                                + url.getParameterAndDecoded(RULE_KEY));
                if (needToPrintMessage) {
                    messageHolder.set("Empty return. Reason: Empty result from condition and condition is force.");
                }
                return BitList.emptyList();
            }

        } catch (Throwable t) {
            logger.error(
                    CLUSTER_FAILED_EXEC_CONDITION_ROUTER,
                    "execute condition state router exception",
                    "",
                    "Failed to execute condition router rule: " + getUrl() + ", invokers: " + invokers + ", cause: "
                            + t.getMessage(),
                    t);
        }
        if (needToPrintMessage) {
            messageHolder.set("Directly return. Reason: Error occurred ( or result is empty ).");
        }
        return invokers;
    }

    boolean matchWhen(URL url, Invocation invocation) {
        if (CollectionUtils.isEmptyMap(whenCondition)) {
            return true;
        }

        return doMatch(url, null, invocation, whenCondition, true);
    }

    private boolean doMatch(
            URL url,
            URL param,
            Invocation invocation,
            Map<String, ConditionMatcher> conditions,
            boolean isWhenCondition) {
        Map<String, String> sample = url.toOriginalMap();
        for (Map.Entry<String, ConditionMatcher> entry : conditions.entrySet()) {
            ConditionMatcher matchPair = entry.getValue();

            if (!matchPair.isMatch(sample, param, invocation, isWhenCondition)) {
                return false;
            }
        }
        return true;
    }

    public void setWhenCondition(Map<String, ConditionMatcher> whenCondition) {
        this.whenCondition = whenCondition;
    }

    public void setThenCondition(List<ConditionSubSet> thenCondition) {
        this.thenCondition = thenCondition;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public Map<String, ConditionMatcher> getWhenCondition() {
        return whenCondition;
    }

    public boolean isForce() {
        return force;
    }

    public List<ConditionSubSet> getThenCondition() {
        return thenCondition;
    }

    public List<ConditionMatcherFactory> getMatcherFactories() {
        return matcherFactories;
    }

    public void setMatcherFactories(List<ConditionMatcherFactory> matcherFactories) {
        this.matcherFactories = matcherFactories;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
