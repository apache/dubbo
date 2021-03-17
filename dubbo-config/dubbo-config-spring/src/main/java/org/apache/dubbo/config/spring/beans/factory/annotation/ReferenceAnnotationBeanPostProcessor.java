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
package org.apache.dubbo.config.spring.beans.factory.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.annotation.Service;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.ReferenceBeanManager;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.config.spring.util.DubboBeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.alibaba.spring.util.AnnotationUtils.getAttribute;
import static org.apache.dubbo.config.spring.beans.factory.annotation.ServiceBeanNameBuilder.create;
import static org.springframework.util.StringUtils.hasText;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} implementation
 * that Consumer service {@link Reference} annotated fields
 *
 * @see DubboReference
 * @see Reference
 * @see com.alibaba.dubbo.config.annotation.Reference
 * @since 2.5.7
 */
public class ReferenceAnnotationBeanPostProcessor extends AbstractAnnotationBeanPostProcessor
        implements ApplicationContextAware, BeanDefinitionRegistryPostProcessor {

    /**
     * The bean name of {@link ReferenceAnnotationBeanPostProcessor}
     */
    public static final String BEAN_NAME = "referenceAnnotationBeanPostProcessor";

    /**
     * Cache size
     */
    private static final int CACHE_SIZE = Integer.getInteger(BEAN_NAME + ".cache.size", 32);

    private final Log logger = LogFactory.getLog(getClass());

    private final ConcurrentMap<InjectionMetadata.InjectedElement, ReferenceBean<?>> injectedFieldReferenceBeanCache =
            new ConcurrentHashMap<>(CACHE_SIZE);

    private final ConcurrentMap<InjectionMetadata.InjectedElement, ReferenceBean<?>> injectedMethodReferenceBeanCache =
            new ConcurrentHashMap<>(CACHE_SIZE);

    private ApplicationContext applicationContext;

    private ReferenceBeanManager referenceBeanManager;
    private BeanDefinitionRegistry beanDefinitionRegistry;

    /**
     * {@link com.alibaba.dubbo.config.annotation.Reference @com.alibaba.dubbo.config.annotation.Reference} has been supported since 2.7.3
     * <p>
     * {@link DubboReference @DubboReference} has been supported since 2.7.7
     */
    public ReferenceAnnotationBeanPostProcessor() {
        super(DubboReference.class, Reference.class, com.alibaba.dubbo.config.annotation.Reference.class);
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        this.beanDefinitionRegistry = registry;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        DubboBeanUtils.registerBeansIfNotExists(beanDefinitionRegistry);

        String[] beanNames = beanFactory.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Class<?> beanType;
            if (beanFactory.isFactoryBean(beanName)){
                BeanDefinition beanDefinition = beanFactory.getMergedBeanDefinition(beanName);
                if (isReferenceBean(beanDefinition)) {
                    continue;
                }
                String beanClassName = beanDefinition.getBeanClassName();
                beanType = ClassUtils.resolveClass(beanClassName, getClassLoader());
            } else {
                beanType = beanFactory.getType(beanName);
            }
            if (beanType != null) {
                AnnotatedInjectionMetadata metadata = findInjectionMetadata(beanName, beanType, null);
                try {
                    prepareInjection(metadata);
                } catch (Exception e) {
                    logger.warn("Prepare dubbo reference injection element failed", e);
                }
            }
        }
    }

    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        if (beanType != null) {
            if (isReferenceBean(beanDefinition)) {
                //mark property value as optional
                List<PropertyValue> propertyValues = beanDefinition.getPropertyValues().getPropertyValueList();
                for (PropertyValue propertyValue : propertyValues) {
                    propertyValue.setOptional(true);
                }
            } else {
                AnnotatedInjectionMetadata metadata = findInjectionMetadata(beanName, beanType, null);
                metadata.checkConfigMembers(beanDefinition);
                try {
                    prepareInjection(metadata);
                } catch (Exception e) {
                    logger.warn("Prepare dubbo reference injection element failed", e);
                }
            }
        }
    }

    @Override
    public PropertyValues postProcessPropertyValues(
            PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeanCreationException {

        try {
            AnnotatedInjectionMetadata metadata = findInjectionMetadata(beanName, bean.getClass(), pvs);
            prepareInjection(metadata);
            metadata.inject(bean, beanName, pvs);
        } catch (BeanCreationException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new BeanCreationException(beanName, "Injection of @" + getAnnotationType().getSimpleName()
                    + " dependencies is failed", ex);
        }
        return pvs;
    }

    private boolean isReferenceBean(BeanDefinition beanDefinition) {
        return ReferenceBean.class.getName().equals(beanDefinition.getBeanClassName());
    }

    protected void prepareInjection(AnnotatedInjectionMetadata metadata) throws Exception {
        //find and registry bean definition for @DubboReference/@Reference
        for (AnnotatedFieldElement fieldElement : metadata.getFieldElements()) {
            if (fieldElement.refKey != null) {
                continue;
            }
            Class<?> injectedType = fieldElement.field.getType();
            AnnotationAttributes attributes = fieldElement.attributes;
            ReferenceBean referenceBean = getReferenceBean(injectedType, attributes);

            //associate fieldElement and reference bean
            fieldElement.refKey = referenceBean.getId();
            injectedFieldReferenceBeanCache.put(fieldElement, referenceBean);

        }

        for (AnnotatedMethodElement methodElement : metadata.getMethodElements()) {
            if (methodElement.refKey != null) {
                continue;
            }
            Class<?> injectedType = methodElement.getInjectedType();
            AnnotationAttributes attributes = methodElement.attributes;
            ReferenceBean referenceBean = getReferenceBean(injectedType, attributes);

            //associate fieldElement and reference bean
            methodElement.refKey = referenceBean.getId();
            injectedMethodReferenceBeanCache.put(methodElement, referenceBean);
        }
    }

    private ReferenceBean getReferenceBean(Class<?> injectedType, AnnotationAttributes attributes) throws Exception {
        // referenceBeanName
        String referenceBeanName = getReferenceBeanName(attributes, injectedType);

        // reuse exist reference bean?
        ReferenceBean referenceBean = referenceBeanManager.get(referenceBeanName);

        //create referenceBean
        if (referenceBean == null) {
            //handle injvm/localServiceBean
            /**
             * The name of bean that annotated Dubbo's {@link Service @Service} in local Spring {@link ApplicationContext}
             */
            String localServiceBeanName = buildReferencedBeanName(attributes, injectedType);
            boolean localServiceBean = isLocalServiceBean(localServiceBeanName, attributes);
            if (localServiceBean) { // If the local @Service Bean exists
                attributes.put("injvm", Boolean.TRUE);
                //  Issue : https://github.com/apache/dubbo/issues/6224
                //exportServiceBeanIfNecessary(localServiceBeanName); // If the referenced ServiceBean exits, export it immediately
            }

            //check interfaceClass
            if (attributes.get("interfaceName") == null && attributes.get("interfaceClass") == null) {
                Class<?> interfaceClass = injectedType;
                Assert.isTrue(interfaceClass.isInterface(),
                        "The class of field or method that was annotated @DubboReference is not an interface!");
                attributes.put("interfaceClass", interfaceClass);
            }

            //init reference bean
            try {
                //registry referenceBean
                RootBeanDefinition beanDefinition = new RootBeanDefinition();
                beanDefinition.setBeanClassName(ReferenceBean.class.getName());
                //set autowireCandidate to false for local call, avoiding multiple candidate beans for @Autowire
                beanDefinition.setAutowireCandidate(!localServiceBean);
                //beanDefinition.getPropertyValues()

                referenceBean = new ReferenceBean(attributes);
                referenceBean.setId(referenceBeanName);
                referenceBean.setApplicationContext(applicationContext);
                referenceBean.setBeanClassLoader(getClassLoader());
                referenceBean.afterPropertiesSet();

                beanDefinitionRegistry.registerBeanDefinition(referenceBeanName, beanDefinition);
                getBeanFactory().registerSingleton(referenceBeanName, referenceBean);

                referenceBeanManager.addReference(referenceBean);
            } catch (Exception e) {
                throw new Exception("Create dubbo reference bean failed", e);
            }
        }
        return referenceBean;
    }

    @Override
    protected Object doGetInjectedBean(AnnotationAttributes attributes, Object bean, String beanName, Class<?> injectedType,
                                       AnnotatedInjectElement injectedElement) throws Exception {

        if (injectedElement.refKey == null) {
            throw new IllegalStateException("The AnnotatedInjectElement of @DubboReference should be inited before injection");
        }

        return getBeanFactory().getBean(injectedElement.refKey);
    }

    /**
     * Get the bean name of {@link ReferenceBean} if {@link Reference#id() id attribute} is present,
     * or {@link #generateReferenceBeanName(AnnotationAttributes, Class) generate}.
     *
     * @param attributes     the {@link AnnotationAttributes attributes} of {@link Reference @Reference}
     * @param interfaceClass the {@link Class class} of Service interface
     * @return non-null
     * @since 2.7.3
     */
    private String getReferenceBeanName(AnnotationAttributes attributes, Class<?> interfaceClass) {
        // id attribute appears since 2.7.3
        String beanName = getAttribute(attributes, "id");
        if (!hasText(beanName)) {
            beanName = generateReferenceBeanName(attributes, interfaceClass);
        }
        return beanName;
    }

    /**
     * Build the bean name of {@link ReferenceBean}
     *
     * @param attributes     the {@link AnnotationAttributes attributes} of {@link Reference @Reference}
     * @param interfaceClass the {@link Class class} of Service interface
     * @return
     * @since 2.7.3
     */
    private String generateReferenceBeanName(AnnotationAttributes attributes, Class<?> interfaceClass) {
        StringBuilder beanNameBuilder = new StringBuilder("@Reference");

        if (!attributes.isEmpty()) {
            beanNameBuilder.append('(');
            //sort attributes keys
            List<String> sortedAttrKeys = new ArrayList<>(attributes.keySet());
            Collections.sort(sortedAttrKeys);
            for (String key : sortedAttrKeys) {
                Object value = attributes.get(key);
                //handle method array, generic array
                if (value!=null && value.getClass().isArray()) {
                    Object[] array = ObjectUtils.toObjectArray(value);
                    value = Arrays.toString(array);
                }
                beanNameBuilder.append(key)
                        .append('=')
                        .append(value)
                        .append(',');
            }
            // replace the latest "," to be ")"
            beanNameBuilder.setCharAt(beanNameBuilder.lastIndexOf(","), ')');
        }

        beanNameBuilder.append(" ").append(interfaceClass.getName());

        //TODO remove invalid chars
        //TODO test @DubboReference with Method config
        //.replaceAll("[<>]", "_")
        return beanNameBuilder.toString();
    }

    /**
     * Is Local Service bean or not?
     *
     * @param referencedBeanName the bean name to the referenced bean
     * @return If the target referenced bean is existed, return <code>true</code>, or <code>false</code>
     * @since 2.7.6
     */
    private boolean isLocalServiceBean(String referencedBeanName, AnnotationAttributes attributes) {
        return existsServiceBean(referencedBeanName) && !isRemoteReferenceBean(attributes);
    }

    /**
     * Check the {@link ServiceBean} is exited or not
     *
     * @param referencedBeanName the bean name to the referenced bean
     * @return if exists, return <code>true</code>, or <code>false</code>
     * @revised 2.7.6
     */
    private boolean existsServiceBean(String referencedBeanName) {
        return applicationContext.containsBean(referencedBeanName) &&
                applicationContext.isTypeMatch(referencedBeanName, ServiceBean.class);

    }

    private boolean isRemoteReferenceBean(AnnotationAttributes attributes) {
        //TODO Can the interface be called locally when injvm is empty? https://github.com/apache/dubbo/issues/6842
        boolean remote = Boolean.FALSE.equals(attributes.get("injvm"));
        return remote;
    }

    private void exportServiceBeanIfNecessary(String referencedBeanName) {
        if (existsServiceBean(referencedBeanName)) {
            ServiceBean serviceBean = getServiceBean(referencedBeanName);
            if (!serviceBean.isExported()) {
                serviceBean.export();
            }
        }
    }

    private ServiceBean getServiceBean(String referencedBeanName) {
        return applicationContext.getBean(referencedBeanName, ServiceBean.class);
    }

    @Override
    protected String buildInjectedObjectCacheKey(AnnotationAttributes attributes, Object bean, String beanName,
                                                 Class<?> injectedType, AnnotatedInjectElement injectedElement) {
        return generateReferenceBeanName(attributes, injectedType);
    }

    /**
     * @param attributes           the attributes of {@link Reference @Reference}
     * @param serviceInterfaceType the type of Dubbo's service interface
     * @return The name of bean that annotated Dubbo's {@link Service @Service} in local Spring {@link ApplicationContext}
     */
    private String buildReferencedBeanName(AnnotationAttributes attributes, Class<?> serviceInterfaceType) {
        ServiceBeanNameBuilder serviceBeanNameBuilder = create(attributes, serviceInterfaceType, getEnvironment());
        return serviceBeanNameBuilder.build();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.referenceBeanManager = applicationContext.getBean(ReferenceBeanManager.BEAN_NAME, ReferenceBeanManager.class);
    }

    @Override
    public void destroy() throws Exception {
        super.destroy();
        this.injectedFieldReferenceBeanCache.clear();
        this.injectedMethodReferenceBeanCache.clear();
    }

    /**
     * Gets all beans of {@link ReferenceBean}
     * @deprecated  use {@link ConfigManager#getReferences()} instead
     */
    @Deprecated
    public Collection<ReferenceBean<?>> getReferenceBeans() {
        return Collections.emptyList();
    }

    /**
     * Get {@link ReferenceBean} {@link Map} in injected field.
     *
     * @return non-null {@link Map}
     * @since 2.5.11
     */
    public Map<InjectionMetadata.InjectedElement, ReferenceBean<?>> getInjectedFieldReferenceBeanMap() {
        return Collections.unmodifiableMap(injectedFieldReferenceBeanCache);
    }

    /**
     * Get {@link ReferenceBean} {@link Map} in injected method.
     *
     * @return non-null {@link Map}
     * @since 2.5.11
     */
    public Map<InjectionMetadata.InjectedElement, ReferenceBean<?>> getInjectedMethodReferenceBeanMap() {
        return Collections.unmodifiableMap(injectedMethodReferenceBeanCache);
    }
}
