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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.BitList;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.state.AbstractStateRouter;
import org.apache.dubbo.rpc.cluster.router.state.RouterCache;
import org.apache.dubbo.rpc.cluster.router.tag.model.TagRouterRule;
import org.apache.dubbo.rpc.cluster.router.tag.model.TagRuleParser;

import static org.apache.dubbo.common.constants.CommonConstants.TAG_KEY;

/**
 * TagRouter, "application.tag-router"
 */
public class TagDynamicStateRouter extends AbstractStateRouter implements ConfigurationListener {
    public static final String NAME = "TAG_ROUTER";
    private static final int TAG_ROUTER_DEFAULT_PRIORITY = 100;
    private static final Logger logger = LoggerFactory.getLogger(TagDynamicStateRouter.class);
    private static final String RULE_SUFFIX = ".tag-router";
    private static final String NO_TAG = "noTag";

    private TagRouterRule tagRouterRule;
    private String application;

    public TagDynamicStateRouter(URL url) {
        super(url);
        this.priority = TAG_ROUTER_DEFAULT_PRIORITY;
    }

    @Override
    public synchronized void process(ConfigChangedEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("Notification of tag rule, change type is: " + event.getChangeType() + ", raw rule is:\n " +
                    event.getContent());
        }

        try {
            if (event.getChangeType().equals(ConfigChangeType.DELETED)) {
                this.tagRouterRule = null;
            } else {
                this.tagRouterRule = TagRuleParser.parse(event.getContent());
            }
        } catch (Exception e) {
            logger.error("Failed to parse the raw tag router rule and it will not take effect, please check if the " +
                    "rule matches with the template, the raw rule is:\n ", e);
        }
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public <T> BitList<Invoker<T>> route(BitList<Invoker<T>> invokers, RouterCache cache, URL url,
        Invocation invocation) throws RpcException {

        final TagRouterRule tagRouterRuleCopy = (TagRouterRule)cache.getAddrMetadata();

        String tag = StringUtils.isEmpty(invocation.getAttachment(TAG_KEY)) ? url.getParameter(TAG_KEY) :
            invocation.getAttachment(TAG_KEY);

        ConcurrentHashMap<String, BitList<Invoker>> addrPool = cache.getAddrPool();

        if (StringUtils.isEmpty(tag)) {
            return invokers.intersect((BitList)addrPool.get(NO_TAG), invokers.getUnmodifiableList());
        } else {
            BitList<Invoker> result = addrPool.get(tag);

            if (CollectionUtils.isNotEmpty(result) || tagRouterRuleCopy.isForce()) {
                return invokers.intersect((BitList)result, invokers.getUnmodifiableList());
            } else {
                invocation.setAttachment(TAG_KEY, NO_TAG);
                return invokers;
            }
        }
    }

    @Override
    public boolean isRuntime() {
        return tagRouterRule != null && tagRouterRule.isRuntime();
    }

    @Override
    public boolean isEnable() {
        return false;
    }

    @Override
    public boolean isForce() {
        // FIXME
        return tagRouterRule != null && tagRouterRule.isForce();
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean shouldRePool() {
        return false;
    }

    @Override
    public <T> RouterCache pool(List<Invoker<T>> invokers) {

        ConcurrentHashMap<String, BitList<Invoker<T>>> addrPool = new ConcurrentHashMap<>();

        final TagRouterRule tagRouterRuleCopy = tagRouterRule;


        if (tagRouterRuleCopy == null || !tagRouterRuleCopy.isValid() || !tagRouterRuleCopy.isEnabled()) {
            return null;
        }

        List<String> tagNames = tagRouterRuleCopy.getTagNames();
        Map<String, List<String>> tagnameToAddresses = tagRouterRuleCopy.getTagnameToAddresses();

        RouterCache routerCache = new RouterCache();



        for (String tag : tagNames) {
            List<String> addresses = tagnameToAddresses.get(tag);
            BitList<Invoker<T>> list = new BitList<>(invokers, true);
            // 地址为空，则动态路由不生效，直接看静态路由
            if (CollectionUtils.isEmpty(addresses)) {
                list.addAll(invokers);
            } else {
                for (Invoker<T> invoker : invokers) {
                    String address = invoker.getUrl().getAddress();
                    if (addresses.contains(address)) {
                        list.add(invoker);
                    }
                }
            }

            addrPool.put(tag, list);
        }

        List<String> addresses = tagRouterRuleCopy.getAddresses();
        BitList<Invoker<T>> noTagList = new BitList<>(invokers, true);

        for (Invoker<T> invoker : invokers) {
            if (!addresses.contains(invoker.getUrl().getAddress())) {
                noTagList.add(invoker);
            }
        }
        addrPool.put(NO_TAG, noTagList);
        routerCache.setAddrPool((ConcurrentHashMap)addrPool);
        routerCache.setAddrMetadata(tagRouterRuleCopy);

        return routerCache;
    }

    public void setApplication(String app) {
        this.application = app;
    }

    @Override
    public <T> void notify(List<Invoker<T>> invokers) {
        if (CollectionUtils.isEmpty(invokers)) {
            return;
        }

        Invoker<T> invoker = invokers.get(0);
        URL url = invoker.getUrl();
        String providerApplication = url.getParameter(CommonConstants.REMOTE_APPLICATION_KEY);

        if (StringUtils.isEmpty(providerApplication)) {
            logger.error("TagRouter must getConfig from or subscribe to a specific application, but the application " +
                    "in this TagRouter is not specified.");
            return;
        }

        synchronized (this) {
            if (!providerApplication.equals(application)) {
                if (!StringUtils.isEmpty(application)) {
                    ruleRepository.removeListener(application + RULE_SUFFIX, this);
                }
                String key = providerApplication + RULE_SUFFIX;
                ruleRepository.addListener(key, this);
                application = providerApplication;
                String rawRule = ruleRepository.getRule(key, DynamicConfiguration.DEFAULT_GROUP);
                if (StringUtils.isNotEmpty(rawRule)) {
                    this.process(new ConfigChangedEvent(key, DynamicConfiguration.DEFAULT_GROUP, rawRule));
                }
            }
        }
        pool(invokers);
    }

}
