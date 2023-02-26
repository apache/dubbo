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
import org.apache.dubbo.rpc.cluster.router.condition.matcher.ConditionMatcher;
import org.apache.dubbo.rpc.cluster.router.condition.matcher.ConditionMatcherFactory;
import org.apache.dubbo.rpc.cluster.router.condition.matcher.pattern.ValuePattern;
import org.apache.dubbo.rpc.cluster.router.state.AbstractStateRouter;
import org.apache.dubbo.rpc.cluster.router.state.BitList;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.dubbo.common.constants.CommonConstants.ENABLED_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CLUSTER_CONDITIONAL_ROUTE_LIST_EMPTY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CLUSTER_FAILED_EXEC_CONDITION_ROUTER;
import static org.apache.dubbo.rpc.cluster.Constants.FORCE_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.RULE_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.RUNTIME_KEY;

/**
 * Condition Router directs traffics matching the 'when condition' to a particular address subset determined by the 'then condition'.
 * One typical condition rule is like below, with
 * 1. the 'when condition' on the left side of '=>' contains matching rule like 'method=sayHello' and 'method=sayHi'
 * 2. the 'then condition' on the right side of '=>' contains matching rule like 'region=hangzhou' and 'address=*:20881'
 * <p>
 * By default, condition router support matching rules like 'foo=bar', 'foo=bar*', 'arguments[0]=bar', 'attachments[foo]=bar', 'attachments[foo]=1~100', etc.
 * It's also very easy to add customized matching rules by extending {@link ConditionMatcherFactory}
 * and {@link ValuePattern}
 * <p>
 * ---
 * scope: service
 * force: true
 * runtime: true
 * enabled: true
 * key: org.apache.dubbo.samples.governance.api.DemoService
 * conditions:
 * - method=sayHello => region=hangzhou
 * - method=sayHi => address=*:20881
 * ...
 */
public class ConditionStateRouter<T> extends AbstractStateRouter<T> {
    public static final String NAME = "condition";

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AbstractStateRouter.class);
    protected static final Pattern ROUTE_PATTERN = Pattern.compile("([&!=,]*)\\s*([^&!=,\\s]+)");
    protected Map<String, ConditionMatcher> whenCondition;
    protected Map<String, ConditionMatcher> thenCondition;
    protected List<ConditionMatcherFactory> matcherFactories;

    private final boolean enabled;

    public ConditionStateRouter(URL url, String rule, boolean force, boolean enabled) {
        super(url);
        this.setForce(force);
        this.enabled = enabled;
        matcherFactories = moduleModel.getExtensionLoader(ConditionMatcherFactory.class).getActivateExtensions();
        if (enabled) {
            this.init(rule);
        }
    }

    public ConditionStateRouter(URL url) {
        super(url);
        this.setUrl(url);
        this.setForce(url.getParameter(FORCE_KEY, false));
        matcherFactories = moduleModel.getExtensionLoader(ConditionMatcherFactory.class).getActivateExtensions();
        this.enabled = url.getParameter(ENABLED_KEY, true);
        if (enabled) {
            init(url.getParameterAndDecoded(RULE_KEY));
        }
    }

    public void init(String rule) {
        try {
            if (rule == null || rule.trim().length() == 0) {
                throw new IllegalArgumentException("Illegal route rule!");
            }
            rule = rule.replace("consumer.", "").replace("provider.", "");
            int i = rule.indexOf("=>");
            String whenRule = i < 0 ? null : rule.substring(0, i).trim();
            String thenRule = i < 0 ? rule.trim() : rule.substring(i + 2).trim();
            Map<String, ConditionMatcher> when = StringUtils.isBlank(whenRule) || "true".equals(whenRule) ? new HashMap<>() : parseRule(whenRule);
            Map<String, ConditionMatcher> then = StringUtils.isBlank(thenRule) || "false".equals(thenRule) ? null : parseRule(thenRule);
            // NOTE: It should be determined on the business level whether the `When condition` can be empty or not.
            this.whenCondition = when;
            this.thenCondition = then;
        } catch (ParseException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private Map<String, ConditionMatcher> parseRule(String rule)
        throws ParseException {
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
                    throw new ParseException("Illegal route rule \""
                        + rule + "\", The error char '" + separator
                        + "' at index " + matcher.start() + " before \""
                        + content + "\".", matcher.start());
                }

                values = matcherPair.getMatches();
                values.add(content);
            }
            // The Value in the KV part.
            else if ("!=".equals(separator)) {
                if (matcherPair == null) {
                    throw new ParseException("Illegal route rule \""
                        + rule + "\", The error char '" + separator
                        + "' at index " + matcher.start() + " before \""
                        + content + "\".", matcher.start());
                }

                values = matcherPair.getMismatches();
                values.add(content);
            }
            // The Value in the KV part, if Value have more than one items.
            else if (",".equals(separator)) { // Should be separated by ','
                if (values == null || values.isEmpty()) {
                    throw new ParseException("Illegal route rule \""
                            + rule + "\", The error char '" + separator
                            + "' at index " + matcher.start() + " before \""
                            + content + "\".", matcher.start());
                }
                values.add(content);
            } else {
                throw new ParseException("Illegal route rule \"" + rule
                        + "\", The error char '" + separator + "' at index "
                        + matcher.start() + " before \"" + content + "\".", matcher.start());
            }
        }
        return condition;
    }

    @Override
    protected BitList<Invoker<T>> doRoute(BitList<Invoker<T>> invokers, URL url, Invocation invocation,
                                          boolean needToPrintMessage, Holder<RouterSnapshotNode<T>> nodeHolder,
                                          Holder<String> messageHolder) throws RpcException {
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
            if (thenCondition == null) {
                logger.warn(CLUSTER_CONDITIONAL_ROUTE_LIST_EMPTY,"condition state router thenCondition is empty","","The current consumer in the service blacklist. consumer: " + NetUtils.getLocalHost() + ", service: " + url.getServiceKey());                if (needToPrintMessage) {
                    messageHolder.set("Empty return. Reason: ThenCondition is empty.");
                }
                return BitList.emptyList();
            }
            BitList<Invoker<T>> result = invokers.clone();
            result.removeIf(invoker -> !matchThen(invoker.getUrl(), url));

            if (!result.isEmpty()) {
                if (needToPrintMessage) {
                    messageHolder.set("Match return.");
                }
                return result;
            } else if (this.isForce()) {
                logger.warn(CLUSTER_CONDITIONAL_ROUTE_LIST_EMPTY,"execute condition state router result list is empty. and force=true","","The route result is empty and force execute. consumer: " + NetUtils.getLocalHost() + ", service: " + url.getServiceKey() + ", router: " + url.getParameterAndDecoded(RULE_KEY));
                if (needToPrintMessage) {
                    messageHolder.set("Empty return. Reason: Empty result from condition and condition is force.");
                }
                return result;
            }
        } catch (Throwable t) {
            logger.error(CLUSTER_FAILED_EXEC_CONDITION_ROUTER,"execute condition state router exception","","Failed to execute condition router rule: " + getUrl() + ", invokers: " + invokers + ", cause: " + t.getMessage(),t);
        }
        if (needToPrintMessage) {
            messageHolder.set("Directly return. Reason: Error occurred ( or result is empty ).");
        }
        return invokers;
    }

    @Override
    public boolean isRuntime() {
        // We always return true for previously defined Router, that is, old Router doesn't support cache anymore.
//        return true;
        return this.getUrl().getParameter(RUNTIME_KEY, false);
    }

    private ConditionMatcher getMatcher(String key) {
        for (ConditionMatcherFactory factory : matcherFactories) {
            if (factory.shouldMatch(key)) {
                return factory.createMatcher(key, moduleModel);
            }
        }
        return moduleModel.getExtensionLoader(ConditionMatcherFactory.class).getExtension("param").createMatcher(key, moduleModel);
    }

    boolean matchWhen(URL url, Invocation invocation) {
        if (CollectionUtils.isEmptyMap(whenCondition)) {
            return true;
        }

        return doMatch(url, null, invocation, whenCondition, true);
    }

    private boolean matchThen(URL url, URL param) {
        if (CollectionUtils.isEmptyMap(thenCondition)) {
            return false;
        }

        return doMatch(url, param, null, thenCondition, false);
    }

    private boolean doMatch(URL url, URL param, Invocation invocation, Map<String, ConditionMatcher> conditions, boolean isWhenCondition) {
        Map<String, String> sample = url.toOriginalMap();
        for (Map.Entry<String, ConditionMatcher> entry : conditions.entrySet()) {
            ConditionMatcher matchPair = entry.getValue();
            if (!matchPair.isMatch(sample, param, invocation, isWhenCondition)) {
                return false;
            }
        }
        return true;
    }
}
