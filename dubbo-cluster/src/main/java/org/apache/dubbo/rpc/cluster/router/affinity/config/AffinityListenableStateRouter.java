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
package org.apache.dubbo.rpc.cluster.router.affinity.config;

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
import org.apache.dubbo.rpc.cluster.router.affinity.AffinityStateRouter;
import org.apache.dubbo.rpc.cluster.router.affinity.config.model.AffinityRouterRule;
import org.apache.dubbo.rpc.cluster.router.affinity.config.model.AffinityRuleParser;
import org.apache.dubbo.rpc.cluster.router.state.AbstractStateRouter;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.cluster.router.state.TailStateRouter;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CLUSTER_FAILED_RULE_PARSING;

/**
 * Abstract router which listens to dynamic configuration
 */
public abstract class AffinityListenableStateRouter<T> extends AbstractStateRouter<T> implements ConfigurationListener {
    public static final String NAME = "Affinity_LISTENABLE_ROUTER";
    public static final String RULE_SUFFIX = ".affinity-router";

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(AffinityListenableStateRouter.class);
    private volatile AffinityRouterRule affinityRouterRule;
    private volatile AffinityStateRouter<T> affinityRouter;
    private final String ruleKey;

    public AffinityListenableStateRouter(URL url, String ruleKey) {
        super(url);
        this.setForce(false);
        this.init(ruleKey);
        this.ruleKey = ruleKey;
    }

    @Override
    public synchronized void process(ConfigChangedEvent event) {
        if (logger.isInfoEnabled()) {
            logger.info("Notification of affinity rule, change type is: " + event.getChangeType() + ", raw rule is:\n "
                    + event.getContent());
        }

        if (event.getChangeType().equals(ConfigChangeType.DELETED)) {
            affinityRouterRule = null;
            affinityRouter = null;
        } else {
            try {
                affinityRouterRule = AffinityRuleParser.parse(event.getContent());
                generateConditions(affinityRouterRule);
            } catch (Exception e) {
                logger.error(
                        CLUSTER_FAILED_RULE_PARSING,
                        "Failed to parse the raw affinity rule",
                        "",
                        "Failed to parse the raw affinity rule and it will not take effect, please check "
                                + "if the affinity rule matches with the template, the raw rule is:\n "
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
        if (CollectionUtils.isEmpty(invokers) || affinityRouter == null) {
            if (needToPrintMessage) {
                messageHolder.set(
                        "Directly return. Reason: Invokers from previous router is empty or affinityRouter is null.");
            }
            return invokers;
        }

        // We will check enabled status inside each router.
        StringBuilder resultMessage = null;
        if (needToPrintMessage) {
            resultMessage = new StringBuilder();
        }
        invokers = affinityRouter.route(invokers, url, invocation, needToPrintMessage, nodeHolder);
        if (needToPrintMessage) {
            resultMessage.append(messageHolder.get());
        }

        if (needToPrintMessage) {
            messageHolder.set(resultMessage.toString());
        }

        return invokers;
    }

    @Override
    public boolean isForce() {
        return (affinityRouterRule != null && affinityRouterRule.isForce());
    }

    private boolean isRuleRuntime() {
        return affinityRouterRule != null && affinityRouterRule.isValid() && affinityRouterRule.isRuntime();
    }

    private void generateConditions(AbstractRouterRule rule) {
        if (rule == null || !rule.isValid()) {
            return;
        }
        AffinityRouterRule affinityRule = (AffinityRouterRule) rule;
        affinityRouter = new AffinityStateRouter<>(
                getUrl(), affinityRule.getAffinityKey(), affinityRule.getRatio(), affinityRule.isEnabled());
        affinityRouter.setNextRouter(TailStateRouter.getInstance());
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

    public AffinityStateRouter<T> getAffinityRouter() {
        return affinityRouter;
    }

    @Override
    public void stop() {
        this.getRuleRepository().removeListener(ruleKey + RULE_SUFFIX, this);
    }
}
