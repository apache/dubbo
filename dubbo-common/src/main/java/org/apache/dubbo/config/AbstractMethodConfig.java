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
import java.util.Map;

/**
 * AbstractMethodConfig
 *
 * @export
 */
public abstract class AbstractMethodConfig extends AbstractConfig {

    private static final long serialVersionUID = 5809761483000878437L;

    /**
     * The timeout for remote invocation in milliseconds
     */
    protected Integer timeout;

    /**
     * The retry times
     */
    protected Integer retries;

    /**
     * max concurrent invocations
     */
    protected Integer actives;

    /**
     * The load balance
     */
    protected String loadbalance;

    /**
     * Whether to async
     * note that: it is an unreliable asynchronous that ignores return values and does not block threads.
     */
    protected Boolean async;

    /**
     * Whether to ack async-sent
     */
    protected Boolean sent;

    /**
     * The name of mock class which gets called when a service fails to execute
     * <p>
     * note that: the mock doesn't support on the provider side，and the mock is executed when a non-business exception
     * occurs after a remote service call
     */
    protected String mock;

    /**
     * Merger
     */
    protected String merger;

    /**
     * Cache the return result with the call parameter as key, the following options are available: lru, threadlocal,
     * jcache, etc.
     */
    protected String cache;

    /**
     * Whether JSR303 standard annotation validation is enabled or not, if enabled, annotations on method parameters will
     * be validated
     */
    protected String validation;

    /**
     * The customized parameters
     */
    protected Map<String, String> parameters;

    /**
     * Forks for forking cluster
     */
    protected Integer forks;

    public AbstractMethodConfig() {
        super();
    }

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
        if (scopeModel == null) {
            throw new IllegalArgumentException("scopeModel cannot be null");
        }
        if (!(scopeModel instanceof ModuleModel)) {
            throw new IllegalArgumentException("Invalid scope model, expect to be a ModuleModel but got: " + scopeModel);
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
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

}
