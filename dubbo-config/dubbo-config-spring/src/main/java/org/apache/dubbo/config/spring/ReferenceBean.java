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
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.ReferenceConfigBase;
import org.apache.dubbo.config.spring.reference.ReferenceBeanManager;
import org.apache.dubbo.config.spring.reference.ReferenceBeanSupport;
import org.apache.dubbo.config.spring.reference.ReferenceAttributes;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.config.utils.ReferenceConfigCache;
import org.apache.dubbo.rpc.proxy.AbstractProxyFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.AbstractLazyCreationTargetSource;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * <p>
 * Spring FactoryBean for {@link ReferenceConfig}.
 * </p>
 *
 *
 * <p></p>
 * Step 1: Register ReferenceBean in Java-config class:
 * <pre class="code">
 * &#64;Configuration
 * public class ReferenceConfiguration {
 *     &#64;Bean
 *     &#64;DubboReference(group = "demo")
 *     public ReferenceBean&lt;HelloService&gt; helloService() {
 *         return new ReferenceBean();
 *     }
 *
 *     // As GenericService
 *     &#64;Bean
 *     &#64;DubboReference(group = "demo", interfaceClass = HelloService.class)
 *     public ReferenceBean&lt;GenericService&gt; genericHelloService() {
 *         return new ReferenceBean();
 *     }
 * }
 * </pre>
 *
 * Or register ReferenceBean in xml:
 * <pre class="code">
 * &lt;dubbo:reference id="helloService" interface="org.apache.dubbo.config.spring.api.HelloService"/&gt;
 * &lt;!-- As GenericService --&gt;
 * &lt;dubbo:reference id="genericHelloService" interface="org.apache.dubbo.config.spring.api.HelloService" generic="true"/&gt;
 * </pre>
 *
 * Step 2: Inject ReferenceBean by @Autowired
 * <pre class="code">
 * public class FooController {
 *     &#64;Autowired
 *     private HelloService helloService;
 *
 *     &#64;Autowired
 *     private GenericService genericHelloService;
 * }
 * </pre>
 *
 *
 * @see org.apache.dubbo.config.annotation.DubboReference
 * @see org.apache.dubbo.config.spring.reference.ReferenceBeanBuilder
 */
public class ReferenceBean<T> implements FactoryBean,
        ApplicationContextAware, BeanClassLoaderAware, BeanNameAware, InitializingBean, DisposableBean {

    private transient ApplicationContext applicationContext;

    private ClassLoader beanClassLoader;

    // lazy proxy of reference
    private Object lazyProxy;

    // beanName
    protected String id;

    // reference key
    private String key;

    /**
     * The interface class of the reference service
     */
    private Class<?> interfaceClass;

    /**
     * Actual interface class of this reference.
     * The actual service type of remote provider.
     * see {@link ReferenceConfigBase#getActualInterface()}
     */
    private Class actualInterface;

    /*
     * actual interface class name
     */
    // Compatible with seata-1.4.0: io.seata.rm.tcc.remoting.parser.DubboRemotingParser#getServiceDesc()
    private String interfaceName;

    //from annotation attributes
    private Map<String, Object> referenceProps;

    //from xml bean definition
    private MutablePropertyValues propertyValues;

    //actual reference config
    private ReferenceConfig referenceConfig;

    public ReferenceBean() {
        super();
    }

    public ReferenceBean(Map<String, Object> referenceProps) {
        this.referenceProps = referenceProps;
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
    public void setBeanName(String name) {
        this.setId(name);
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
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();

        // pre init xml reference bean or @DubboReference annotation
        Assert.notEmptyString(getId(), "The id of ReferenceBean cannot be empty");
        BeanDefinition beanDefinition = beanFactory.getBeanDefinition(getId());
        this.interfaceClass = (Class<?>) beanDefinition.getAttribute(ReferenceAttributes.INTERFACE_CLASS);
        this.actualInterface = (Class) beanDefinition.getAttribute(ReferenceAttributes.ACTUAL_INTERFACE);
        Assert.notNull(this.interfaceClass, "The interface class of ReferenceBean is not initialized");

        if (beanDefinition.hasAttribute(Constants.REFERENCE_PROPS)) {
            // @DubboReference annotation at java-config class @Bean method
            // @DubboReference annotation at reference field or setter method
            referenceProps = (Map<String, Object>) beanDefinition.getAttribute(Constants.REFERENCE_PROPS);
        } else {
            if (beanDefinition instanceof AnnotatedBeanDefinition) {
                // Return ReferenceBean in java-config class @Bean method
                if (referenceProps == null) {
                    referenceProps = new LinkedHashMap<>();
                }
                ReferenceBeanSupport.convertReferenceProps(referenceProps, interfaceClass);
                if (this.actualInterface == null) {
                    try {
                        this.actualInterface = ClassUtils.forName((String) referenceProps.get(ReferenceAttributes.INTERFACE));
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException(e.getMessage(), e);
                    }
                }
            } else {
                // xml reference bean
                propertyValues = beanDefinition.getPropertyValues();
            }
        }
        Assert.notNull(this.actualInterface, "The actual interface of ReferenceBean is not initialized");
        this.interfaceName = actualInterface.getName();

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

    /* Compatible with seata-1.4.0: io.seata.rm.tcc.remoting.parser.DubboRemotingParser#getServiceDesc() */
    public Class<?> getInterfaceClass() {
        return interfaceClass;
    }

    public Class getActualInterface() {
        return actualInterface;
    }

    /* Compatible with seata-1.4.0: io.seata.rm.tcc.remoting.parser.DubboRemotingParser#getServiceDesc() */
    public String getGroup() {
        return referenceConfig.getGroup();
    }

    /* Compatible with seata-1.4.0: io.seata.rm.tcc.remoting.parser.DubboRemotingParser#getServiceDesc() */
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
    }

    /**
     * create lazy proxy for reference
     */
    private void createLazyProxy() {

        //set proxy interfaces
        //see also: org.apache.dubbo.rpc.proxy.AbstractProxyFactory.getProxy(org.apache.dubbo.rpc.Invoker<T>, boolean)
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTargetSource(new DubboReferenceLazyInitTargetSource());
        proxyFactory.addInterface(getInterfaceClass());
        Class<?>[] internalInterfaces = AbstractProxyFactory.getInternalInterfaces();
        for (Class<?> anInterface : internalInterfaces) {
            proxyFactory.addInterface(anInterface);
        }
        if (actualInterface != interfaceClass){
            //add actual interface
            proxyFactory.addInterface(actualInterface);
        }

        this.lazyProxy = proxyFactory.getProxy(this.beanClassLoader);
    }

    private Object getCallProxy() throws Exception {
        if (referenceConfig == null) {
            throw new IllegalStateException("ReferenceBean is not ready yet, please make sure to call reference interface method after dubbo is started.");
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
