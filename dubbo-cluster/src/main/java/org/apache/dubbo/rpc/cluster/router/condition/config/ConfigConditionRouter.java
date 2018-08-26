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
import org.apache.dubbo.config.dynamic.ConfigChangeEvent;
import org.apache.dubbo.config.dynamic.ConfigurationListener;
import org.apache.dubbo.config.dynamic.DynamicConfiguration;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.TreeNode;
import org.apache.dubbo.rpc.cluster.router.condition.ConditionRouter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class ConfigConditionRouter extends ConditionRouter implements ConfigurationListener {
    private static final Logger logger = LoggerFactory.getLogger(ConfigConditionRouter.class);
    private DynamicConfiguration configuration;
    private ConditionRouterRule routerRule;

    public ConfigConditionRouter(URL url) {
        super(url);
    }

    public ConfigConditionRouter(DynamicConfiguration configuration) {
        this.configuration = configuration;
        this.priority = -2;
        this.force = false;
        try {
            String app = configuration.getUrl().getParameter(Constants.APPLICATION_KEY);
            String rawRule = configuration.getConfig(app + Constants.ROUTERS_SUFFIX, "dubbo", this);
            ConditionRouterRule routerRule = ConditionRuleParser.parse(rawRule);
            init(routerRule.getRuleBody());
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public void process(ConfigChangeEvent event) {
        String rawRule = event.getNewValue();
        ConditionRouterRule routerRule = ConditionRuleParser.parse(rawRule);
        init(routerRule.getRuleBody());
    }

    @Override
    public <T> Map<String, List<Invoker<T>>> preRoute(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        Map<String, List<Invoker<T>>> map = new HashMap<>();

        if (CollectionUtils.isEmpty(invokers)) {
            return map;
        }

        if (isRuntime()) {
            map.put(TreeNode.FAILOVER_KEY, invokers);
            return map;
        }

        map.put(TreeNode.FAILOVER_KEY, route(invokers, url, invocation));

        return map;
    }

    @Override
    public boolean isRuntime() {
        return routerRule.isRuntime();
    }

    @Override
    public String getKey() {
        return "";
    }

    @Override
    public boolean isForce() {
        return routerRule.isForce();
    }
}
