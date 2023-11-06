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

import org.apache.dubbo.common.bytecode.Proxy;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.spring.context.DubboConfigApplicationListener;
import org.apache.dubbo.config.spring.context.DubboConfigBeanInitializer;
import org.apache.dubbo.config.spring.reference.ReferenceAttributes;
import org.apache.dubbo.config.spring.reference.ReferenceBeanManager;
import org.apache.dubbo.config.spring.reference.ReferenceBeanSupport;
import org.apache.dubbo.config.spring.schema.DubboBeanDefinitionParser;
import org.apache.dubbo.config.spring.util.LazyTargetInvocationHandler;
import org.apache.dubbo.config.spring.util.LazyTargetSource;
import org.apache.dubbo.config.spring.util.LockUtils;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.proxy.AbstractProxyFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_DUBBO_BEAN_INITIALIZER;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROXY_FAILED;

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
public class ReferenceBean<T>
        implements FactoryBean<T>,
                ApplicationContextAware,
                BeanClassLoaderAware,
                BeanNameAware,
                InitializingBean,
                DisposableBean {
    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());
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

    /*
     * remote service interface class name
     */
    // 'interfaceName' field for compatible with seata-1.4.0:
    // io.seata.rm.tcc.remoting.parser.DubboRemotingParser#getServiceDesc()
    private String interfaceName;

    // proxy style
    private String proxy;

    // from annotation attributes
    private Map<String, Object> referenceProps;

    // from xml bean definition
    private MutablePropertyValues propertyValues;

    // actual reference config
    private ReferenceConfig referenceConfig;

    // ReferenceBeanManager
    private ReferenceBeanManager referenceBeanManager;

    // Registration sources of this reference, may be xml file or annotation location
    private List<Map<String, Object>> sources = new ArrayList<>();

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

    /**
     * Create bean instance.
     *
     * <p></p>
     * Why we need a lazy proxy?
     *
     * <p/>
     * When Spring searches beans by type, if Spring cannot determine the type of a factory bean, it may try to initialize it.
     * The ReferenceBean is also a FactoryBean.
     * <br/>
     * (This has already been resolved by decorating the BeanDefinition: {@link DubboBeanDefinitionParser#configReferenceBean})
     *
     * <p/>
     * In addition, if some ReferenceBeans are dependent on beans that are initialized very early,
     * and dubbo config beans are not ready yet, there will be many unexpected problems if initializing the dubbo reference immediately.
     *
     * <p/>
     * When it is initialized, only a lazy proxy object will be created,
     * and dubbo reference-related resources will not be initialized.
     * <br/>
     * In this way, the influence of Spring is eliminated, and the dubbo configuration initialization is controllable.
     *
     *
     * @see DubboConfigBeanInitializer
     * @see ReferenceBeanManager#initReferenceBean(ReferenceBean)
     * @see DubboBeanDefinitionParser#configReferenceBean
     */
    @Override
    public T getObject() {
        if (lazyProxy == null) {
            createLazyProxy();
        }
        return (T) lazyProxy;
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
        this.interfaceName = (String) beanDefinition.getAttribute(ReferenceAttributes.INTERFACE_NAME);
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
                if (this.interfaceName == null) {
                    this.interfaceName = (String) referenceProps.get(ReferenceAttributes.INTERFACE);
                }
            } else {
                // xml reference bean
                propertyValues = beanDefinition.getPropertyValues();
            }
        }

        if (referenceProps != null) {
            this.proxy = (String) referenceProps.get(ReferenceAttributes.PROXY);
        }
        Assert.notNull(this.interfaceName, "The interface name of ReferenceBean is not initialized");

        this.referenceBeanManager = beanFactory.getBean(ReferenceBeanManager.BEAN_NAME, ReferenceBeanManager.class);
        referenceBeanManager.addReference(this);
    }

    private ConfigurableListableBeanFactory getBeanFactory() {
        return (ConfigurableListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
    }

    @Override
    public void destroy() {
        // do nothing
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * The interface of this ReferenceBean, for injection purpose
     * @return
     */
    public Class<?> getInterfaceClass() {
        // Compatible with seata-1.4.0: io.seata.rm.tcc.remoting.parser.DubboRemotingParser#getServiceDesc()
        return interfaceClass;
    }

    /**
     * The interface of remote service
     */
    public String getServiceInterface() {
        return interfaceName;
    }

    /**
     * The group of the service
     */
    public String getGroup() {
        // Compatible with seata-1.4.0: io.seata.rm.tcc.remoting.parser.DubboRemotingParser#getServiceDesc()
        return referenceConfig.getGroup();
    }

    /**
     * The version of the service
     */
    public String getVersion() {
        // Compatible with seata-1.4.0: io.seata.rm.tcc.remoting.parser.DubboRemotingParser#getServiceDesc()
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
     * Create lazy proxy for reference.
     */
    private void createLazyProxy() {

        // set proxy interfaces
        // see also: org.apache.dubbo.rpc.proxy.AbstractProxyFactory.getProxy(org.apache.dubbo.rpc.Invoker<T>, boolean)
        List<Class<?>> interfaces = new ArrayList<>();
        interfaces.add(interfaceClass);
        Class<?>[] internalInterfaces = AbstractProxyFactory.getInternalInterfaces();
        Collections.addAll(interfaces, internalInterfaces);
        if (!StringUtils.isEquals(interfaceClass.getName(), interfaceName)) {
            // add service interface
            try {
                Class<?> serviceInterface = ClassUtils.forName(interfaceName, beanClassLoader);
                interfaces.add(serviceInterface);
            } catch (ClassNotFoundException e) {
                // generic call maybe without service interface class locally
            }
        }

        if (StringUtils.isEmpty(this.proxy) || CommonConstants.DEFAULT_PROXY.equalsIgnoreCase(this.proxy)) {
            generateFromJavassistFirst(interfaces);
        }

        if (this.lazyProxy == null) {
            generateFromJdk(interfaces);
        }
    }

    private void generateFromJavassistFirst(List<Class<?>> interfaces) {
        try {
            this.lazyProxy = Proxy.getProxy(interfaces.toArray(new Class[0]))
                    .newInstance(new LazyTargetInvocationHandler(new DubboReferenceLazyInitTargetSource()));
        } catch (Throwable fromJavassist) {
            // try fall back to JDK proxy factory
            try {
                this.lazyProxy = java.lang.reflect.Proxy.newProxyInstance(
                        beanClassLoader,
                        interfaces.toArray(new Class[0]),
                        new LazyTargetInvocationHandler(new DubboReferenceLazyInitTargetSource()));
                logger.error(
                        PROXY_FAILED,
                        "",
                        "",
                        "Failed to generate proxy by Javassist failed. Fallback to use JDK proxy success. "
                                + "Interfaces: " + interfaces,
                        fromJavassist);
            } catch (Throwable fromJdk) {
                logger.error(
                        PROXY_FAILED,
                        "",
                        "",
                        "Failed to generate proxy by Javassist failed. Fallback to use JDK proxy is also failed. "
                                + "Interfaces: " + interfaces + " Javassist Error.",
                        fromJavassist);
                logger.error(
                        PROXY_FAILED,
                        "",
                        "",
                        "Failed to generate proxy by Javassist failed. Fallback to use JDK proxy is also failed. "
                                + "Interfaces: " + interfaces + " JDK Error.",
                        fromJdk);
                throw fromJavassist;
            }
        }
    }

    private void generateFromJdk(List<Class<?>> interfaces) {
        try {
            this.lazyProxy = java.lang.reflect.Proxy.newProxyInstance(
                    beanClassLoader,
                    interfaces.toArray(new Class[0]),
                    new LazyTargetInvocationHandler(new DubboReferenceLazyInitTargetSource()));
        } catch (Throwable fromJdk) {
            logger.error(
                    PROXY_FAILED,
                    "",
                    "",
                    "Failed to generate proxy by Javassist failed. Fallback to use JDK proxy is also failed. "
                            + "Interfaces: " + interfaces + " JDK Error.",
                    fromJdk);
            throw fromJdk;
        }
    }

    private Object getCallProxy() throws Exception {
        if (referenceConfig == null) {
            referenceBeanManager.initReferenceBean(this);
            applicationContext
                    .getBean(DubboConfigApplicationListener.class.getName(), DubboConfigApplicationListener.class)
                    .init();
            logger.warn(
                    CONFIG_DUBBO_BEAN_INITIALIZER,
                    "",
                    "",
                    "ReferenceBean is not ready yet, please make sure to call reference interface method after dubbo is started.");
        }
        // get reference proxy
        // Subclasses should synchronize on the given Object if they perform any sort of extended singleton creation
        // phase.
        // In particular, subclasses should not have their own mutexes involved in singleton creation, to avoid the
        // potential for deadlocks in lazy-init situations.
        // The redundant type cast is to be compatible with earlier than spring-4.2
        if (referenceConfig.configInitialized()) {
            return referenceConfig.get();
        }
        synchronized (LockUtils.getSingletonMutex(applicationContext)) {
            return referenceConfig.get();
        }
    }

    private class DubboReferenceLazyInitTargetSource implements LazyTargetSource {
        @Override
        public Object getTarget() throws Exception {
            return getCallProxy();
        }
    }
}
