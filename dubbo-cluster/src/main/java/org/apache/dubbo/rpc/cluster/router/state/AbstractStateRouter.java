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
package org.apache.dubbo.rpc.cluster.router.state;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.RouterChain;
import org.apache.dubbo.rpc.cluster.governance.GovernanceRuleRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/***
 * The abstract class of StateRoute.
 * @since 3.0
 */
public abstract class AbstractStateRouter implements StateRouter {
    final protected RouterChain chain;
    protected int priority = DEFAULT_PRIORITY;
    protected boolean force = false;
    protected URL url;
    protected List<Invoker> invokers;
    protected AtomicReference<AddrCache> cache;
    protected GovernanceRuleRepository ruleRepository;

    public AbstractStateRouter(URL url, RouterChain chain) {
        this.ruleRepository = url.getOrDefaultModuleModel().getExtensionLoader(GovernanceRuleRepository.class).getDefaultExtension();
        this.chain = chain;
        this.url = url;
    }

    @Override
    public <T> void notify(List<Invoker<T>> invokers) {
        this.invokers = (List)invokers;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    @Override
    public boolean isRuntime() {
        return true;
    }

    @Override
    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public <T> StateRouterResult<Invoker<T>> route(BitList<Invoker<T>> invokers, RouterCache<T> cache, URL url,
        Invocation invocation, boolean needToPrintMessage) throws RpcException {

        List<String> tags = getTags(url, invocation);

        if (tags == null) {
            return new StateRouterResult<>(invokers);
        }
        for (String tag : tags) {
            BitList<Invoker<T>> tagInvokers = cache.getAddrPool().get(tag);
            if (tagMatchFail(tagInvokers)) {
                continue;
            }
            if (needToPrintMessage) {
                return new StateRouterResult<>(invokers.and(tagInvokers), "use tag " + tag + " to route");
            } else {
                return new StateRouterResult<>(invokers.and(tagInvokers));
            }
        }

        return new StateRouterResult<>(invokers);
    }

    protected List<String> getTags(URL url, Invocation invocation) {
        return new ArrayList<String>();
    }

    public <T> Boolean tagMatchFail(BitList<Invoker<T>> invokers) {
        return invokers == null || invokers.isEmpty();
    }

    @Override
    public void pool() {
        chain.loop(false);
    }
}
