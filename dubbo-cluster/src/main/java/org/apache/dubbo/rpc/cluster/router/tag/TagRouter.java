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
package org.apache.dubbo.rpc.cluster.router.tag;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.dynamic.ConfigChangeEvent;
import org.apache.dubbo.config.dynamic.ConfigChangeType;
import org.apache.dubbo.config.dynamic.ConfigurationListener;
import org.apache.dubbo.config.dynamic.DynamicConfiguration;
import org.apache.dubbo.config.dynamic.DynamicConfigurationFactory;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Router;
import org.apache.dubbo.rpc.cluster.router.AbstractRouter;
import org.apache.dubbo.rpc.cluster.router.TreeNode;
import org.apache.dubbo.rpc.cluster.router.tag.model.TagRouterRule;
import org.apache.dubbo.rpc.cluster.router.tag.model.TagRuleParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 *
 */
public class TagRouter extends AbstractRouter implements Comparable<Router>, ConfigurationListener {
    public static final String NAME = "TAG_ROUTER";
    private static final Logger logger = LoggerFactory.getLogger(TagRouter.class);
    private static final String TAGROUTERRULES_DATAID = ".tagrouters"; // acts
    private DynamicConfiguration configuration;
    private TagRouterRule tagRouterRule;
    private String application;

    protected int priority = -1; // FIXME A fixed value considering other routers in the chain.

    public TagRouter(URL url) {
        this(ExtensionLoader.getExtensionLoader(DynamicConfigurationFactory.class).getAdaptiveExtension().getDynamicConfiguration(url), url);
    }

    public TagRouter(DynamicConfiguration configuration, URL url) {
        setConfiguration(configuration);
        this.url = url;
    }

    protected TagRouter() {
    }

    public void setConfiguration(DynamicConfiguration configuration) {
        this.configuration = configuration;
    }

    private void init() {
        if (StringUtils.isEmpty(application)) {
            logger.error("TagRouter must getConfig from or subscribe to a specific application, but the application in this TagRouter is not specified.");
        }
        String rawRule = this.configuration.getConfig(application + TAGROUTERRULES_DATAID, "dubbo", this);
        if (StringUtils.isNotEmpty(rawRule)) {
            this.tagRouterRule = TagRuleParser.parse(rawRule);
        }
    }

    @Override
    public void process(ConfigChangeEvent event) {
        try {
            if (event.getChangeType().equals(ConfigChangeType.DELETED)) {
                this.tagRouterRule = null;
            } else {
                this.tagRouterRule = TagRuleParser.parse(event.getNewValue());
            }
            routerChain.notifyRuleChanged();
        } catch (Exception e) {
            // TODO
            logger.error(e);
        }
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

        checkAndInit(invokers.get(0).getUrl());

        if (tagRouterRule == null || !tagRouterRule.isValid()) {
            return invokers;
        }

        List<Invoker<T>> result = invokers;
        String tag = StringUtils.isEmpty(invocation.getAttachment(Constants.TAG_KEY)) ? url.getParameter(Constants.TAG_KEY) : invocation.getAttachment(Constants.TAG_KEY);
        // if we are requesting for a Provider with a specific tag
        if (StringUtils.isNotEmpty(tag)) {
            List<String> addresses = tagRouterRule.getTagnameToAddresses().get(tag);
            // filter by dynamic tag group first
            if (CollectionUtils.isNotEmpty(addresses)) {
                result = filterInvoker(invokers, invoker -> addressMatches(invoker.getUrl(), addresses));
                // if result is not null OR it's null but force=true, return result directly
                if (CollectionUtils.isNotEmpty(result) || tagRouterRule.isForce()) {
                    return result;
                }
            }
            // dynamic tag group doesn't have any item about the requested app OR it's null after filtered by dynamic tag group but force=false.
            // check static tag
            result = filterInvoker(invokers, invoker -> tag.equals(invoker.getUrl().getParameter(Constants.TAG_KEY)));
            if (CollectionUtils.isNotEmpty(result) || url.getParameter(Constants.FORCE_USE_TAG, true)) {
                return result;
            }
            // FAILOVER: return all Providers without any tags.
            else {
                return filterInvoker(invokers, invoker -> StringUtils.isEmpty(invoker.getUrl().getParameter(Constants.TAG_KEY)));
            }
        } else {
            // List<String> addresses = tagRouterRule.filter(providerApp);
            // return all addresses in dynamic tag group.
            List<String> addresses = tagRouterRule.getAddresses();
            if (CollectionUtils.isNotEmpty(addresses)) {
                result = filterInvoker(invokers, invoker -> addressNotMatches(invoker.getUrl(), addresses));
                // 1. all addresses are in dynamic tag group, return empty list.
                if (CollectionUtils.isEmpty(result)) {
                    return result;
                }
                // 2. if there are some addresses that are not in any dynamic tag group, continue to filter using the static tag group.
            }
            return filterInvoker(result, invoker -> {
                String localTag = invoker.getUrl().getParameter(Constants.TAG_KEY);
                if (StringUtils.isEmpty(localTag) || !tagRouterRule.getTagNames().contains(localTag)) {
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    public <T> Map<String, List<Invoker<T>>> preRoute(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        Map<String, List<Invoker<T>>> map = new HashMap<>();

        if (CollectionUtils.isEmpty(invokers)) {
            return map;
        }

        checkAndInit(invokers.get(0).getUrl());

        if (tagRouterRule == null || !tagRouterRule.isValid() || isRuntime()) {
            map.put(TreeNode.FAILOVER_KEY, invokers);
            return map;
        }

        invokers.forEach(invoker -> {
            String address = invoker.getUrl().getAddress();
            List<String> tags = tagRouterRule.getAddressToTagnames().get(address);
            if (CollectionUtils.isEmpty(tags)) {
                String tag = invoker.getUrl().getParameter(Constants.TAG_KEY);
                // we have checked that this address is not included in any of the tag listed in dynamic tag group.
                // so if found this address were grouped into one tag in dynamic tag group, we think it's invalid, which means, dynamic tag group will override static tag group (the dynamic config may have explicitly removed this address from one or another group).
                if (tagRouterRule.getTagNames().contains(tag) || StringUtils.isEmpty(tag)) {
                    tag = TreeNode.FAILOVER_KEY;
                }
                tags = new ArrayList<>();
                tags.add(tag);
            }

            tags.forEach(tag -> {
                List<Invoker<T>> subInvokers = map.computeIfAbsent(tag, k -> new ArrayList<>());
                subInvokers.add(invoker);
            });
        });

        // Now, FAILOVER key is required here.
        map.putIfAbsent(TreeNode.FAILOVER_KEY, Collections.emptyList());

        return map;
    }

    public void checkAndInit(URL providerUrl) {
        if (StringUtils.isEmpty(application)) {
            setApplication(providerUrl.getParameter(Constants.REMOTE_APPLICATION_KEY));
        }
        this.init();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isRuntime() {
        return tagRouterRule != null && tagRouterRule.isRuntime();
    }

    @Override
    public String getKey() {
        if (isRuntime()) {
            return super.getKey();
        }
        return Constants.TAG_KEY;
    }

    @Override
    public boolean isForce() {
        // FIXME
        return tagRouterRule != null && tagRouterRule.isForce();
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

    private boolean addressNotMatches(URL url, List<String> addresses) {
        return !addresses.contains(url.getAddress());
    }

    public void setApplication(String app) {
        this.application = app;
    }

    @Override
    public int compareTo(Router o) {
        return 0;
    }
}
