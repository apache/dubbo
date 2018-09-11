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

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.dynamic.ConfigChangeEvent;
import org.apache.dubbo.config.dynamic.ConfigChangeType;
import org.apache.dubbo.config.dynamic.ConfigurationListener;
import org.apache.dubbo.config.dynamic.DynamicConfiguration;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Router;
import org.apache.dubbo.rpc.cluster.router.AbstractRouter;
import org.apache.dubbo.rpc.cluster.router.TreeNode;
import org.apache.dubbo.rpc.cluster.router.condition.ConditionRouter;
import org.apache.dubbo.rpc.cluster.router.condition.config.model.ConditionRouterRule;
import org.apache.dubbo.rpc.cluster.router.condition.config.model.ConditionRuleParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO only support one router rule =>, it will be inconvenient if we want to add more than one rules.
 */
public class ConfigConditionRouter extends AbstractRouter implements ConfigurationListener {
    public static final String NAME = "CONFIG_CONDITION_OUTER";
    private static final Logger logger = LoggerFactory.getLogger(ConfigConditionRouter.class);
    private DynamicConfiguration configuration;
    private ConditionRouterRule routerRule;
    private ConditionRouterRule appRouterRule;
    private List<ConditionRouter> conditionRouters = new ArrayList<>();
    private List<ConditionRouter> appConditionRouters = new ArrayList<>();

    public ConfigConditionRouter(DynamicConfiguration configuration, URL url) {
        this.configuration = configuration;
        this.priority = -2;
        this.force = false;
        this.url = url;
        try {
            String app = this.url.getParameter(Constants.APPLICATION_KEY);
            String serviceKey = this.url.getServiceKey();
            String rawRule = this.configuration.getConfig(serviceKey + Constants.ROUTERS_SUFFIX, "dubbo", this);
            String appRawRule = this.configuration.getConfig(app + Constants.ROUTERS_SUFFIX, "dubbo", this);
            if (!StringUtils.isEmpty(rawRule)) {
                routerRule = ConditionRuleParser.parse(rawRule);
                generateConditions();
            }
            if (!StringUtils.isEmpty(appRawRule)) {
                appRouterRule = ConditionRuleParser.parse(appRawRule);
                generateAppConditions();
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public void process(ConfigChangeEvent event) {
        try {
            if (event.getChangeType().equals(ConfigChangeType.DELETED)) {
                // Now, we can only recognize if it's a app level or service level change by try to match event key.
                if (event.getKey().endsWith(this.url.getParameter(Constants.APPLICATION_KEY) + Constants.ROUTERS_SUFFIX)) {
                    appRouterRule = null;
                    conditionRouters.clear();
                } else {
                    routerRule = null;
                    appConditionRouters.clear();
                }
            } else {
                if (event.getKey().endsWith(this.url.getParameter(Constants.APPLICATION_KEY) + Constants.ROUTERS_SUFFIX)) {
                    appRouterRule = ConditionRuleParser.parse(event.getNewValue());
                    generateAppConditions();
                } else {
                    routerRule = ConditionRuleParser.parse(event.getNewValue());
                    generateConditions();
                }
            }
            routerChain.notifyRuleChanged();
        } catch (Exception e) {
            logger.error(e);
            // TODO
        }
    }

    @Override
    public <T> Map<String, List<Invoker<T>>> preRoute(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        Map<String, List<Invoker<T>>> map = new HashMap<>();

        if (CollectionUtils.isEmpty(invokers)
                || (conditionRouters.size() == 0 && appConditionRouters.size() == 0)
                || isRuntime()) {
            map.put(TreeNode.FAILOVER_KEY, invokers);
            return map;
        }

        // only one branch, always use the failover key
        if (isAppRuleEnabled()) {
            for (Router router : appConditionRouters) {
                invokers = router.route(invokers, url, invocation);
            }
        }
        if (isRuleEnabled()) {
            for (Router router : conditionRouters) {
                invokers = router.route(invokers, url, invocation);
            }
        }
        map.put(TreeNode.FAILOVER_KEY, invokers);

        return map;
    }

    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        if (CollectionUtils.isEmpty(invokers)
                || (conditionRouters.size() == 0 && appConditionRouters.size() == 0)
                || !isEnabled()) {
            return invokers;
        }


        if (isAppRuleEnabled()) {
            for (Router router : appConditionRouters) {
                invokers = router.route(invokers, url, invocation);
            }
        }
        if (isRuleEnabled()) {
            for (Router router : conditionRouters) {
                invokers = router.route(invokers, url, invocation);
            }
        }
        return invokers;
    }

    @Override
    public boolean isRuntime() {
        return isRuleRuntime() || isAppRuleRuntime();
    }

    @Override
    public boolean isEnabled() {
        return isAppRuleEnabled() || isRuleEnabled();
    }

    @Override
    public String getKey() {
        if (isRuntime()) {
            return super.getKey();
        }
        return TreeNode.FAILOVER_KEY;
    }

    @Override
    public boolean isForce() {
        return (routerRule != null && routerRule.isForce())
                || (appRouterRule != null && appRouterRule.isForce());
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int compareTo(Router o) {
        return 0;
    }

    private boolean isAppRuleEnabled() {
        return appRouterRule != null && appRouterRule.isValid() && appRouterRule.isEnabled();
    }

    private boolean isRuleEnabled() {
        return routerRule != null && routerRule.isValid() && routerRule.isEnabled();
    }

    private boolean isAppRuleRuntime() {
        return appRouterRule != null && appRouterRule.isValid() && appRouterRule.isRuntime();
    }

    private boolean isRuleRuntime() {
        return routerRule != null && routerRule.isValid() && routerRule.isRuntime();
    }

    private void generateConditions() {
        if (routerRule != null && routerRule.isValid()) {
            conditionRouters.clear();
            routerRule.getConditions().forEach(condition -> {
                // All sub rules have the same force, runtime value.
                ConditionRouter subRouter = new ConditionRouter(condition, routerRule.isForce());
                conditionRouters.add(subRouter);
            });
        }
    }

    private void generateAppConditions() {
        if (appRouterRule != null && appRouterRule.isValid()) {
            appConditionRouters.clear();
            appRouterRule.getConditions().forEach(condition -> {
                // All sub rules have the same force, runtime value.
                ConditionRouter subRouter = new ConditionRouter(condition, appRouterRule.isForce());
                appConditionRouters.add(subRouter);
            });
        }
    }
}
