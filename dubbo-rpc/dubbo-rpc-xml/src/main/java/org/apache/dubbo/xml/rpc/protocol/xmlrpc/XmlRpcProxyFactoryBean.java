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
package org.apache.dubbo.xml.rpc.protocol.xmlrpc;

import org.apache.dubbo.rpc.RpcException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.remoting.support.UrlBasedRemoteAccessor;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;

public class XmlRpcProxyFactoryBean extends UrlBasedRemoteAccessor
        implements MethodInterceptor,
        InitializingBean,
        FactoryBean<Object>,
        ApplicationContextAware {

    private Object				proxyObject			= null;
    private XmlRpcClient xmlRpcClient	= null;
//    private Map<String, String> extraHttpHeaders	= new HashMap<String, String>();


    private ApplicationContext applicationContext;

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public void afterPropertiesSet() {
        super.afterPropertiesSet();

        // create proxy
        proxyObject = ProxyFactory.getProxy(getServiceInterface(), this);

        // create XmlRpcHttpClient
        try {
            xmlRpcClient = new XmlRpcClient();

            XmlRpcClientConfigImpl xmlRpcClientConfig = new XmlRpcClientConfigImpl();
            xmlRpcClientConfig.setServerURL(new URL(getServiceUrl()));
            xmlRpcClient.setConfig(xmlRpcClientConfig);

        } catch (MalformedURLException mue) {
            throw new RpcException(mue);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object invoke(MethodInvocation invocation)
            throws Throwable {

        // handle toString()
        Method method = invocation.getMethod();
        if (method.getDeclaringClass() == Object.class && method.getName().equals("toString")) {
            return proxyObject.getClass().getName() + "@" + System.identityHashCode(proxyObject);
        }

        // get return type
        Type retType = (invocation.getMethod().getGenericReturnType() != null)
                ? invocation.getMethod().getGenericReturnType()
                : invocation.getMethod().getReturnType();

        return xmlRpcClient.execute(replace(method.getDeclaringClass().getName())+"."
                +invocation.getMethod().getName(),invocation.getArguments());

//        // get arguments
//        Object arguments = ReflectionUtil.parseArguments(
//                invocation.getMethod(), invocation.getArguments(), useNamedParams);
//
//        // invoke it
//        return jsonRpcHttpClient.invoke(
//                invocation.getMethod().getName(),
//                arguments,
//                retType, extraHttpHeaders);
    }

    /**
     * {@inheritDoc}
     */
    public Object getObject() {
        return proxyObject;
    }

    /**
     * {@inheritDoc}
     */
    public Class<?> getObjectType() {
        return getServiceInterface();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSingleton() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public static final String replace(String name) {
        return name.replaceAll("\\.","_");
    }

}
