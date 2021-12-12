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
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.governance.GovernanceRuleRepository;

/***
 * The abstract class of StateRoute.
 * @since 3.0
 */
public abstract class AbstractStateRouter<T> implements StateRouter<T> {
    private volatile int priority = DEFAULT_PRIORITY;
    private volatile boolean force = false;
    private volatile URL url;

    private final GovernanceRuleRepository ruleRepository;

    public AbstractStateRouter(URL url) {
        this.ruleRepository = url.getOrDefaultModuleModel().getExtensionLoader(GovernanceRuleRepository.class).getDefaultExtension();
        this.url = url;
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

    public GovernanceRuleRepository getRuleRepository() {
        return this.ruleRepository;
    }

    @Override
    public void notify(BitList<Invoker<T>> invokers) {

    }
}
