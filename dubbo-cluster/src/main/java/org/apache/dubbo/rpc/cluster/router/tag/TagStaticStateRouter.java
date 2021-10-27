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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.RouterChain;
import org.apache.dubbo.rpc.cluster.router.state.AbstractStateRouter;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.cluster.router.state.RouterCache;
import org.apache.dubbo.rpc.cluster.router.state.StateRouterResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.CommonConstants.TAG_KEY;

/**
 * TagStaticStateRouter, "application.tag-router"
 */
public class TagStaticStateRouter extends AbstractStateRouter {
    public static final String NAME = "TAG_ROUTER";
    private static final int TAG_ROUTER_DEFAULT_PRIORITY = 100;
    private static final String NO_TAG = "noTag";

    public TagStaticStateRouter(URL url, RouterChain chain) {
        super(url, chain);
        this.priority = TAG_ROUTER_DEFAULT_PRIORITY;
    }

    @Override
    public URL getUrl() {
        return url;
    }


    @Override
    public <T> StateRouterResult<Invoker<T>> route(BitList<Invoker<T>> invokers, RouterCache<T> routerCache, URL url,
                                                   Invocation invocation, boolean needToPrintMessage) throws RpcException {

        String tag = isNoTag(invocation.getAttachment(TAG_KEY)) ? url.getParameter(TAG_KEY) :
            invocation.getAttachment(TAG_KEY);
        if (StringUtils.isEmpty(tag)) {
            tag = NO_TAG;
        }

        ConcurrentMap<String, BitList<Invoker<T>>> pool = routerCache.getAddrPool();
        BitList<Invoker<T>> res = pool.get(tag);
        if (res == null) {
            return new StateRouterResult<>(invokers);
        }
        return new StateRouterResult<>(invokers.and(res));
    }

    private boolean isNoTag(String tag) {
        return StringUtils.isEmpty(tag) || NO_TAG.equals(tag);
    }

    @Override
    protected List<String> getTags(URL url, Invocation invocation) {
        List<String> tags = new ArrayList<>();
        String tag = StringUtils.isEmpty(invocation.getAttachment(TAG_KEY)) ? url.getParameter(TAG_KEY) :
            invocation.getAttachment(TAG_KEY);
        if (StringUtils.isEmpty(tag)) {
            tag = NO_TAG;
        }
        tags.add(tag);
        return tags;
    }


    @Override
    public boolean isEnable() {
        return true;
    }

    @Override
    public boolean isForce() {
        // FIXME
        return false;
    }

    @Override
    public String getName() {
        return "TagStatic";
    }

    @Override
    public boolean shouldRePool() {
        return false;
    }

    @Override
    public <T> RouterCache<T> pool(List<Invoker<T>> invokers) {

        RouterCache<T> routerCache = new RouterCache<>();
        ConcurrentHashMap<String, BitList<Invoker<T>>> addrPool = new ConcurrentHashMap<>();

        for (int index = 0; index < invokers.size(); index++) {
            Invoker<T> invoker = invokers.get(index);
            String tag = invoker.getUrl().getParameter(TAG_KEY);
            if (StringUtils.isEmpty(tag)) {
                BitList<Invoker<T>> noTagList = addrPool.computeIfAbsent(NO_TAG, k -> new BitList<>(invokers, true));
                noTagList.addIndex(index);
            } else {
                BitList<Invoker<T>> list = addrPool.computeIfAbsent(tag, k -> new BitList<>(invokers, true));
                list.addIndex(index);
            }
        }

        routerCache.setAddrPool(addrPool);

        return routerCache;
    }


    @Override
    public <T> void notify(List<Invoker<T>> invokers) {
        if (CollectionUtils.isEmpty(invokers)) {
            return;
        }
    }

}
