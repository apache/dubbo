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
package org.apache.dubbo.config.builders;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.AbstractMethodConfig;

import java.util.Map;

/**
 * AbstractBuilder
 *
 * @since 2.7
 */
public abstract class AbstractMethodBuilder<T extends AbstractMethodConfig, B extends AbstractMethodBuilder<T, B>>
        extends AbstractBuilder<T, B>{
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
     * note that: it is an unreliable asynchronism that ignores return values and does not block threads.
     */
    protected Boolean async;

    /**
     * Whether to ack async-sent
     */
    protected Boolean sent;

    /**
     * The name of mock class which gets called when a service fails to execute
     *
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

    public B timeout(Integer timeout) {
        this.timeout = timeout;
        return getThis();
    }

    public B retries(Integer retries) {
        this.retries = retries;
        return getThis();
    }

    public B actives(Integer actives) {
        this.actives = actives;
        return getThis();
    }

    public B loadbalance(String loadbalance) {
        this.loadbalance = loadbalance;
        return getThis();
    }

    public B async(Boolean async) {
        this.async = async;
        return getThis();
    }

    public B sent(Boolean sent) {
        this.sent = sent;
        return getThis();
    }

    public B mock(String mock) {
        this.mock = mock;
        return getThis();
    }

    public B mock(Boolean mock) {
        if (mock != null) {
            this.mock = mock.toString();
        } else {
            this.mock = null;
        }
        return getThis();
    }

    public B merger(String merger) {
        this.merger = merger;
        return getThis();
    }

    public B cache(String cache) {
        this.cache = cache;
        return getThis();
    }

    public B validation(String validation) {
        this.validation = validation;
        return getThis();
    }

    public B appendParameters(Map<String, String> appendParameters) {
        this.parameters = appendParameters(parameters, appendParameters);
        return getThis();
    }

    public B appendParameter(String key, String value) {
        this.parameters = appendParameter(parameters, key, value);
        return getThis();
    }

    public B forks(Integer forks) {
        this.forks = forks;
        return getThis();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void build(T instance) {
        super.build(instance);

        if (actives != null) {
            instance.setActives(actives);
        }
        if (async != null) {
            instance.setAsync(async);
        }
        if (!StringUtils.isEmpty(cache)) {
            instance.setCache(cache);
        }
        if (forks != null) {
            instance.setForks(forks);
        }
        if (!StringUtils.isEmpty(loadbalance)) {
            instance.setLoadbalance(loadbalance);
        }
        if (!StringUtils.isEmpty(merger)) {
            instance.setMerger(merger);
        }
        if(!StringUtils.isEmpty(mock)) {
            instance.setMock(mock);
        }
        if (retries != null) {
            instance.setRetries(retries);
        }
        if (sent != null) {
            instance.setSent(sent);
        }
        if (timeout != null) {
            instance.setTimeout(timeout);
        }
        if (!StringUtils.isEmpty(validation)) {
            instance.setValidation(validation);
        }
        if (parameters != null) {
            instance.setParameters(parameters);
        }
    }
}
