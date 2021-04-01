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
package org.apache.dubbo.config.spring;

import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.config.utils.ReferenceConfigCache;
import org.apache.dubbo.rpc.proxy.AbstractProxyFactory;
import org.apache.dubbo.rpc.support.ProtocolUtils;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.AbstractLazyCreationTargetSource;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;


/**
 * ReferenceFactoryBean
 */
public class ReferenceBean<T> implements FactoryBean,
        ApplicationContextAware, BeanClassLoaderAware, InitializingBean, DisposableBean {

    private transient ApplicationContext applicationContext;
    private ClassLoader beanClassLoader;
    private DubboReferenceLazyInitTargetSource lazyTargetSource;
    private Object lazyProxy;
    /**
     * The interface class of the reference service
     */
    protected Class<?> interfaceClass;

    // beanName
    protected String id;
    // unique cache key
    private String key;
    //from annotation attributes
    private Map<String, Object> referenceProps;
    //from bean definition
    private MutablePropertyValues propertyValues;
    //actual reference config
    private ReferenceConfig referenceConfig;
    private String generic;
    private String interfaceName;

    public ReferenceBean() {
        super();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.beanClassLoader = classLoader;
    }

    @Override
    public Object getObject() {
        if (lazyProxy == null) {
            createLazyProxy();
        }
        return lazyProxy;
    }

    @Override
    public Class<?> getObjectType() {
        return getInterfaceClass();
    }

    @Override
    @Parameter(excluded = true)
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notEmptyString(getId(), "The id of ReferenceBean cannot be empty");
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        BeanDefinition beanDefinition = beanFactory.getBeanDefinition(getId());
        if (beanDefinition.hasAttribute("referenceProps")) {
            referenceProps = (Map<String, Object>) beanDefinition.getAttribute("referenceProps");
        } else {
            propertyValues = beanDefinition.getPropertyValues();
        }
        this.generic = (String) beanDefinition.getAttribute("generic");
        this.interfaceName = (String) beanDefinition.getAttribute("interfaceName");
        this.interfaceClass = (Class<?>) beanDefinition.getAttribute("interfaceClass");

        ReferenceBeanManager referenceBeanManager = beanFactory.getBean(ReferenceBeanManager.BEAN_NAME, ReferenceBeanManager.class);
        referenceBeanManager.addReference(this);
    }

    private ConfigurableListableBeanFactory getBeanFactory() {
        return (ConfigurableListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
    }

    @Override
    public void destroy() {
        // do nothing
    }

    @Deprecated
    public Object get() {
        return referenceConfig.get();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGeneric() {
        return generic;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    /* Compatible with seata: io.seata.rm.tcc.remoting.parser.DubboRemotingParser#getServiceDesc() */
    public String getGroup() {
        return referenceConfig.getGroup();
    }

    public String getVersion() {
        return referenceConfig.getVersion();
    }

    public String getKey() {
        return key;
    }

    public Map<String, Object> getReferenceProps() {
        return referenceProps;
    }

    public MutablePropertyValues getPropertyValues() {
        return propertyValues;
    }

    public ReferenceConfig getReferenceConfig() {
        return referenceConfig;
    }

    public void setKeyAndReferenceConfig(String key, ReferenceConfig referenceConfig) {
        this.key = key;
        this.referenceConfig = referenceConfig;

        //If the 'init' attribute is not set, the default value is false
        Object init = referenceConfig.isInit();
        if (init == null) {
            referenceConfig.setInit(false);
        }
    }

    public Class<?> getInterfaceClass() {
        return interfaceClass;
    }

    /**
     * create lazy proxy for reference
     */
    private void createLazyProxy() {
        this.lazyTargetSource = new DubboReferenceLazyInitTargetSource();

        //set proxy interfaces
        //see also: org.apache.dubbo.rpc.proxy.AbstractProxyFactory.getProxy(org.apache.dubbo.rpc.Invoker<T>, boolean)
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTargetSource(lazyTargetSource);
        proxyFactory.addInterface(getInterfaceClass());
        Class<?>[] internalInterfaces = AbstractProxyFactory.getInternalInterfaces();
        for (Class<?> anInterface : internalInterfaces) {
            proxyFactory.addInterface(anInterface);
        }
        if (ProtocolUtils.isGeneric(generic)){
            //add actual interface
            proxyFactory.addInterface(ReflectUtils.forName(interfaceName));
        }

        this.lazyProxy = proxyFactory.getProxy(this.beanClassLoader);
    }

    private Object getCallProxy() throws Exception {

        if (referenceConfig == null) {
            throw new IllegalStateException("ReferenceBean is not ready yet, maybe dubbo engine is not started");
        }
        //get reference proxy
        return ReferenceConfigCache.getCache().get(referenceConfig);
    }

    private class DubboReferenceLazyInitTargetSource extends AbstractLazyCreationTargetSource {

        @Override
        protected Object createObject() throws Exception {
            return getCallProxy();
        }

        @Override
        public synchronized Class<?> getTargetClass() {
            return getInterfaceClass();
        }
    }

}
