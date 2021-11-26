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

package org.apache.dubbo.rpc.protocol.http;

import com.googlecode.jsonrpc4j.spring.JsonProxyFactoryBean;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.remoting.support.RemoteInvocationBasedAccessor;

/**
 * JsonRpcProxyFactoryBean
 */
public class JsonRpcProxyFactoryBean extends RemoteInvocationBasedAccessor
        implements MethodInterceptor,
        InitializingBean,
        FactoryBean<Object>,
        ApplicationContextAware {
    private final JsonProxyFactoryBean jsonProxyFactoryBean;

    public JsonRpcProxyFactoryBean(JsonProxyFactoryBean factoryBean) {
        this.jsonProxyFactoryBean = factoryBean;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void afterPropertiesSet() {
        jsonProxyFactoryBean.afterPropertiesSet();
    }

    @Override
    public Object invoke(MethodInvocation invocation)
            throws Throwable {

        return jsonProxyFactoryBean.invoke(invocation);
    }

    @Override
    public Object getObject() {
        return jsonProxyFactoryBean.getObject();
    }

    @Override
    public Class<?> getObjectType() {
        return jsonProxyFactoryBean.getObjectType();
    }

    @Override
    public boolean isSingleton() {
        return jsonProxyFactoryBean.isSingleton();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        jsonProxyFactoryBean.setApplicationContext(applicationContext);
    }

    @Override
    public void setServiceUrl(String serviceUrl) {
        jsonProxyFactoryBean.setServiceUrl(serviceUrl);
    }

    @Override
    public void setServiceInterface(Class<?> serviceInterface) {
        jsonProxyFactoryBean.setServiceInterface(serviceInterface);
    }

}
