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
package org.apache.dubbo.rpc.cluster.router.group;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.dynamic.ConfigChangeEvent;
import org.apache.dubbo.config.dynamic.ConfigurationListener;
import org.apache.dubbo.config.dynamic.DynamicConfiguration;
import org.apache.dubbo.config.dynamic.DynamicConfigurationFactory;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Router;
import org.apache.dubbo.rpc.cluster.router.AbstractRouter;
import org.apache.dubbo.rpc.cluster.router.TreeNode;
import org.apache.dubbo.rpc.cluster.router.group.model.GroupRouterRule;
import org.apache.dubbo.rpc.cluster.router.group.model.GroupRuleParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 *
 */
public class GroupRouter extends AbstractRouter implements Comparable<Router>, ConfigurationListener {
    public static final String NAME = "TAG_ROUTER";
    private static final Logger logger = LoggerFactory.getLogger(GroupRouter.class);
    private static final String TAGRULE_DATAID = "global.tag.rules";
    private static final String FAILOVER_TAG = "tag.failover";
    private URL url;
    private DynamicConfiguration configuration;
    private GroupRouterRule groupRouterRule;

    public GroupRouter(URL url) {
        this(ExtensionLoader.getExtensionLoader(DynamicConfigurationFactory.class).getAdaptiveExtension().getDynamicConfiguration(url));
        this.url = url;
    }

    public GroupRouter(DynamicConfiguration configuration) {
        this.priority = -1;
        this.configuration = configuration;
        init();
    }

    public void init() {
        String rawRule = configuration.getConfig(TAGRULE_DATAID, "dubbo", this);
        this.groupRouterRule = GroupRuleParser.parse(rawRule);
    }

    @Override
    public void process(ConfigChangeEvent event) {
        String rawRule = event.getNewValue();
        // remove, set groupRouterRule to null
        // change, update groupRouterRule
        routerChain.notifyRuleChanged();
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        if (CollectionUtils.isEmpty(invokers)) {
            return invokers;
        }
        List<Invoker<T>> result = invokers;
        String routeGroup = StringUtils.isEmpty(invocation.getAttachment(Constants.REQUEST_TAG_KEY)) ? url.getParameter(Constants.TAG_KEY) : invocation.getAttachment(Constants.REQUEST_TAG_KEY);
        if (StringUtils.isNotEmpty(routeGroup)) {
            String providerApp = invokers.get(0).getUrl().getParameter(Constants.APPLICATION_KEY);
            List<String> addresses = groupRouterRule.filter(routeGroup, providerApp);
            if (CollectionUtils.isNotEmpty(addresses)) {
                result = filterInvoker(invokers, invoker -> addressMatches(invoker.getUrl(), addresses));
            } else {
                result = filterInvoker(invokers, invoker -> invoker.getUrl().getParameter(Constants.TAG_KEY).equals(routeGroup));
            }
        }
        if (StringUtils.isEmpty(routeGroup) || (CollectionUtils.isEmpty(result) && url.getParameter(FAILOVER_TAG, true))) {
            result = filterInvoker(invokers, invoker -> StringUtils.isEmpty(invoker.getUrl().getParameter(Constants.TAG_KEY)));
        }
        return result;
    }

    @Override
    public <T> Map<String, List<Invoker<T>>> preRoute(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        Map<String, List<Invoker<T>>> map = new HashMap<>();

        if (CollectionUtils.isEmpty(invokers) || groupRouterRule == null || !groupRouterRule.isValid()) {
            return map;
        }

        if (isRuntime()) {
            map.put(TreeNode.FAILOVER_KEY, invokers);
            return map;
        }

        invokers.forEach(invoker -> {
            String providerApp = invoker.getUrl().getParameter(Constants.APPLICATION_KEY);
            String address = invoker.getUrl().getAddress();
            String routeGroup = groupRouterRule.getIpAppToGroup().get(providerApp + address);
            if (StringUtils.isEmpty(routeGroup)) {
                routeGroup = invoker.getUrl().getParameter(Constants.TAG_KEY);
            }
            if (StringUtils.isEmpty(routeGroup)) {
                routeGroup = TreeNode.FAILOVER_KEY;
            }
            List<Invoker<T>> subInvokers = map.computeIfAbsent(routeGroup, k -> new ArrayList<>());
            subInvokers.add(invoker);
        });

        return map;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isRuntime() {
        return false;
    }

    public String getKey() {
        return Constants.TAG_KEY;
    }

    @Override
    public boolean isForce() {
        return false;
    }

    public boolean isRuntime(Invocation invocation) {
        return true;
    }

    private <T> List<Invoker<T>> filterInvoker(List<Invoker<T>> invokers, Predicate<Invoker<T>> predicate) {
        return invokers.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    private boolean addressMatches(URL url, List<String> addresses) {
        return addresses.contains(url.getAddress());
    }

    @Override
    public int compareTo(Router o) {
        return 0;
    }
}
