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
package org.apache.dubbo.rpc.cluster.router.condition.config;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.AbstractRouterRule;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotNode;
import org.apache.dubbo.rpc.cluster.router.condition.ConditionStateRouter;
import org.apache.dubbo.rpc.cluster.router.condition.MultiDestConditionRouter;
import org.apache.dubbo.rpc.cluster.router.condition.config.model.ConditionRouterRule;
import org.apache.dubbo.rpc.cluster.router.condition.config.model.ConditionRuleParser;
import org.apache.dubbo.rpc.cluster.router.condition.config.model.MultiDestConditionRouterRule;
import org.apache.dubbo.rpc.cluster.router.state.AbstractStateRouter;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.cluster.router.state.TailStateRouter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CLUSTER_FAILED_RULE_PARSING;
import static org.apache.dubbo.rpc.cluster.Constants.RULE_VERSION_V31;

/**
 * Abstract router which listens to dynamic configuration
 */
public abstract class ListenableStateRouter<T> extends AbstractStateRouter<T> implements ConfigurationListener {
    public static final String NAME = "LISTENABLE_ROUTER";
    public static final String RULE_SUFFIX = ".condition-router";

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(ListenableStateRouter.class);
    private volatile AbstractRouterRule routerRule;
    private volatile List<ConditionStateRouter<T>> conditionRouters = Collections.emptyList();

    //    for v3.1
    private volatile List<MultiDestConditionRouter<T>> multiDestConditionRouters = Collections.emptyList();
    private final String ruleKey;

    public ListenableStateRouter(URL url, String ruleKey) {
        super(url);
        this.setForce(false);
        this.init(ruleKey);
        this.ruleKey = ruleKey;
    }

    @Override
    public synchronized void process(ConfigChangedEvent event) {
        if (logger.isInfoEnabled()) {
            logger.info("Notification of condition rule, change type is: " + event.getChangeType() + ", raw rule is:\n "
                    + event.getContent());
        }

        if (event.getChangeType().equals(ConfigChangeType.DELETED)) {
            routerRule = null;
            conditionRouters = Collections.emptyList();
            //          for v3.1
            multiDestConditionRouters = Collections.emptyList();
        } else {
            try {
                routerRule = ConditionRuleParser.parse(event.getContent());
                generateConditions(routerRule);
            } catch (Exception e) {
                logger.error(
                        CLUSTER_FAILED_RULE_PARSING,
                        "Failed to parse the raw condition rule",
                        "",
                        "Failed to parse the raw condition rule and it will not take effect, please check "
                                + "if the condition rule matches with the template, the raw rule is:\n "
                                + event.getContent(),
                        e);
            }
        }
    }

    @Override
    public BitList<Invoker<T>> doRoute(
            BitList<Invoker<T>> invokers,
            URL url,
            Invocation invocation,
            boolean needToPrintMessage,
            Holder<RouterSnapshotNode<T>> nodeHolder,
            Holder<String> messageHolder)
            throws RpcException {
        if (CollectionUtils.isEmpty(invokers)
                || (conditionRouters.size() == 0 && multiDestConditionRouters.size() == 0)) {
            if (needToPrintMessage) {
                messageHolder.set(
                        "Directly return. Reason: Invokers from previous router is empty or conditionRouters is empty.");
            }
            return invokers;
        }

        // We will check enabled status inside each router.
        StringBuilder resultMessage = null;
        if (needToPrintMessage) {
            resultMessage = new StringBuilder();
        }

        BitList<Invoker<T>> routeResult = invokers;
        if (routerRule instanceof MultiDestConditionRouterRule
                || routerRule.getVersion() != null && routerRule.getVersion().startsWith(RULE_VERSION_V31)) {
            boolean trafficDisable = false;
            for (MultiDestConditionRouter<T> multiDestConditionRouter : multiDestConditionRouters) {
                routeResult = multiDestConditionRouter.route(invokers, url, invocation, needToPrintMessage, nodeHolder);
                if (needToPrintMessage) {
                    resultMessage.append(messageHolder.get());
                }
                if (invokers == routeResult) {
                    //                    not match or disable to continue next multiDestConditionRouter
                    continue;
                } else if (routeResult.size() == 0
                        && !multiDestConditionRouter.isTrafficDisable()
                        && !multiDestConditionRouter.isForce()) {
                    //                    empty but can continue to next multiDestConditionRouter
                    continue;
                } else {
                    trafficDisable = multiDestConditionRouter.isTrafficDisable();
                    break;
                }
            }
            //            if trafficDisable ignore root.force
            if (routeResult.size() == 0 && !routerRule.isForce() && !trafficDisable) {
                routeResult = invokers;
            }
        } else {
            for (AbstractStateRouter<T> router : conditionRouters) {
                routeResult = router.route(routeResult, url, invocation, needToPrintMessage, nodeHolder);
                if (needToPrintMessage) {
                    resultMessage.append(messageHolder.get());
                }
            }
        }

        if (needToPrintMessage) {
            messageHolder.set(resultMessage.toString());
        }

        return routeResult;
    }

    @Override
    public boolean isForce() {
        return (routerRule != null && routerRule.isForce());
    }

    private boolean isRuleRuntime() {
        return routerRule != null && routerRule.isValid() && routerRule.isRuntime();
    }

    private void generateConditions(AbstractRouterRule rule) {
        if (rule == null || !rule.isValid()) {
            return;
        }

        if (rule instanceof ConditionRouterRule) {
            this.conditionRouters = ((ConditionRouterRule) rule)
                    .getConditions().stream()
                            .map(condition ->
                                    new ConditionStateRouter<T>(getUrl(), condition, rule.isForce(), rule.isEnabled()))
                            .collect(Collectors.toList());

            for (ConditionStateRouter<T> conditionRouter : this.conditionRouters) {
                conditionRouter.setNextRouter(TailStateRouter.getInstance());
            }
        } else if (rule instanceof MultiDestConditionRouterRule) {
            this.multiDestConditionRouters = ((MultiDestConditionRouterRule) rule)
                    .getConditions().stream()
                            .map(condition -> new MultiDestConditionRouter<T>(getUrl(), condition, rule.isEnabled()))
                            .sorted((a, b) -> a.getPriority() - b.getPriority())
                            .collect(Collectors.toList());

            for (MultiDestConditionRouter<T> conditionRouter : this.multiDestConditionRouters) {
                conditionRouter.setNextRouter(TailStateRouter.getInstance());
            }
        }
    }

    private synchronized void init(String ruleKey) {
        if (StringUtils.isEmpty(ruleKey)) {
            return;
        }
        String routerKey = ruleKey + RULE_SUFFIX;
        this.getRuleRepository().addListener(routerKey, this);
        String rule = this.getRuleRepository().getRule(routerKey, DynamicConfiguration.DEFAULT_GROUP);
        if (StringUtils.isNotEmpty(rule)) {
            this.process(new ConfigChangedEvent(routerKey, DynamicConfiguration.DEFAULT_GROUP, rule));
        }
    }

    @Override
    public void stop() {
        this.getRuleRepository().removeListener(ruleKey + RULE_SUFFIX, this);
    }
}
