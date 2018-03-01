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
package com.alibaba.dubbo.config;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.config.support.Parameter;
import com.alibaba.dubbo.rpc.cluster.LoadBalance;

import java.util.Map;

/**
 * AbstractMethodConfig
 * TODO 需要改下
 * 方法级配置的抽象类。
 * 属性参见：http://dubbo.io/books/dubbo-user-book/references/xml/dubbo-method.html 。
 *  ps：更多属性，在实现类里。
 *
 * @export
 */
public abstract class AbstractMethodConfig extends AbstractConfig {

    private static final long serialVersionUID = 1L;

    // timeout for remote invocation in milliseconds
    protected Integer timeout;

    // retry times
    protected Integer retries;

    // max concurrent invocations
    protected Integer actives; // TODO 芋艿

    // load balance
    protected String loadbalance;

    // whether to async
    protected Boolean async; // TODO 芋艿

    // whether to ack async-sent
    protected Boolean sent; // TODO 芋艿

    /**
     * the name of mock class which gets called when a service fails to execute
     * 服务接口调用失败 Mock 实现类名。
     *
     * 该 Mock 类必须有一个无参构造函数。
     * 与 Stub 的区别在于，Stub 总是被执行，而Mock只在出现非业务异常(比如超时，网络异常等)时执行，Stub 在远程调用之前执行，Mock 在远程调用后执行。
     *
     * 设为 true，表示使用缺省Mock类名，即：接口名 + Mock 后缀
     *
     * 参见文档 <a href="本地伪装">http://dubbo.io/books/dubbo-user-book/demos/local-mock.html</>
     */
    protected String mock; // TODO 芋艿

    // merger
    protected String merger; // TODO 芋艿

    // cache
    protected String cache; // TODO 芋艿

    // validation
    protected String validation; // TODO 芋艿

    // customized parameters
    protected Map<String, String> parameters;

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
        checkExtension(LoadBalance.class, "loadbalance", loadbalance);
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

    public void setMock(Boolean mock) {
        if (mock == null) {
            setMock((String) null);
        } else {
            setMock(String.valueOf(mock));
        }
    }

    public void setMock(String mock) {
        if (mock != null && mock.startsWith(Constants.RETURN_PREFIX)) {
            checkLength("mock", mock);
        } else {
            checkName("mock", mock);
        }
        this.mock = mock;
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
        checkParameterName(parameters);
        this.parameters = parameters;
    }

}