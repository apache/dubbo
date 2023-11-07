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
package org.apache.dubbo.config;

import org.apache.dubbo.config.context.ModuleConfigManager;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModel;

import java.beans.Transient;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Abstract configuration for the method.
 *
 * @export
 */
public abstract class AbstractMethodConfig extends AbstractConfig {

    private static final long serialVersionUID = 5809761483000878437L;

    /**
     * Timeout for remote invocation in milliseconds.
     */
    protected Integer timeout;

    /**
     * Retry times for failed invocations.
     */
    protected Integer retries;

    /**
     * Maximum concurrent invocations allowed.
     */
    protected Integer actives;

    /**
     * Load balancing strategy for service invocation.
     */
    protected String loadbalance;

    /**
     * Enable asynchronous invocation. Note that it is unreliable asynchronous, ignoring return values and not blocking threads.
     */
    protected Boolean async;

    /**
     * Acknowledge asynchronous-sent invocations.
     */
    protected Boolean sent;

    /**
     * Mock class name to be called when a service fails to execute. The mock doesn't support on the provider side,
     * and it is executed when a non-business exception occurs after a remote service call.
     */
    protected String mock;

    /**
     * Merger for result data.
     */
    protected String merger;

    /**
     * Cache provider for caching return results. available options: lru, threadlocal, jcache etc.
     */
    protected String cache;

    /**
     * Enable JSR303 standard annotation validation for method parameters.
     */
    protected String validation;

    /**
     * Customized parameters for configuration.
     */
    protected Map<String, String> parameters;

    /**
     * Forks for forking cluster.
     */
    protected Integer forks;

    public AbstractMethodConfig() {}

    public AbstractMethodConfig(ModuleModel moduleModel) {
        super(moduleModel);
    }

    @Override
    @Transient
    public ModuleModel getScopeModel() {
        return (ModuleModel) super.getScopeModel();
    }

    @Override
    @Transient
    protected ScopeModel getDefaultModel() {
        return ApplicationModel.defaultModel().getDefaultModule();
    }

    @Override
    protected void checkScopeModel(ScopeModel scopeModel) {
        if (!(scopeModel instanceof ModuleModel)) {
            throw new IllegalArgumentException(
                    "Invalid scope model, expect to be a ModuleModel but got: " + scopeModel);
        }
    }

    @Transient
    protected ModuleConfigManager getModuleConfigManager() {
        return getScopeModel().getConfigManager();
    }

    public Integer getForks() {
        return forks;
    }

    public void setForks(Integer forks) {
        this.forks = forks;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    public String getLoadbalance() {
        return loadbalance;
    }

    public void setLoadbalance(String loadbalance) {
        this.loadbalance = loadbalance;
    }

    public Boolean isAsync() {
        return async;
    }

    public void setAsync(Boolean async) {
        this.async = async;
    }

    public Integer getActives() {
        return actives;
    }

    public void setActives(Integer actives) {
        this.actives = actives;
    }

    public Boolean getSent() {
        return sent;
    }

    public void setSent(Boolean sent) {
        this.sent = sent;
    }

    @Parameter(escaped = true)
    public String getMock() {
        return mock;
    }

    public void setMock(String mock) {
        this.mock = mock;
    }

    /**
     * Set the property "mock"
     *
     * @param mock the value of mock
     * @since 2.7.6
     * @deprecated use {@link #setMock(String)} instead
     */
    @Deprecated
    public void setMock(Object mock) {
        if (mock == null) {
            return;
        }
        this.setMock(String.valueOf(mock));
    }

    public String getMerger() {
        return merger;
    }

    public void setMerger(String merger) {
        this.merger = merger;
    }

    public String getCache() {
        return cache;
    }

    public void setCache(String cache) {
        this.cache = cache;
    }

    public String getValidation() {
        return validation;
    }

    public void setValidation(String validation) {
        this.validation = validation;
    }

    public Map<String, String> getParameters() {
        this.parameters = Optional.ofNullable(this.parameters).orElseGet(HashMap::new);
        return this.parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
}
