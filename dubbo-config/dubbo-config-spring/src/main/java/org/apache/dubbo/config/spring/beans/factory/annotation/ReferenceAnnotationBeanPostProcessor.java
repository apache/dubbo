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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.annotation.Service;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.ReferenceBeanManager;
import org.apache.dubbo.config.spring.ServiceBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.Assert;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
        implements ApplicationContextAware, BeanFactoryPostProcessor {

    /**
     * The bean name of {@link ReferenceAnnotationBeanPostProcessor}
     */
    public static final String BEAN_NAME = "referenceAnnotationBeanPostProcessor";

    /**
     * Cache size
     */
    private static final int CACHE_SIZE = Integer.getInteger(BEAN_NAME + ".cache.size", 32);

    private final Log logger = LogFactory.getLog(getClass());

    private final ConcurrentMap<InjectionMetadata.InjectedElement, String> injectedFieldReferenceBeanCache =
            new ConcurrentHashMap<>(CACHE_SIZE);

    private final ConcurrentMap<InjectionMetadata.InjectedElement, String> injectedMethodReferenceBeanCache =
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
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

        String[] beanNames = beanFactory.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Class<?> beanType;
            if (beanFactory.isFactoryBean(beanName)){
                BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
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
                } catch (BeansException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException("Prepare dubbo reference injection element failed", e);
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
                    throw new RuntimeException("Prepare dubbo reference injection element failed", e);
                }
            }
        }
    }

    @Override
    public PropertyValues postProcessPropertyValues(
            PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {

        try {
            AnnotatedInjectionMetadata metadata = findInjectionMetadata(beanName, bean.getClass(), pvs);
            prepareInjection(metadata);
            metadata.inject(bean, beanName, pvs);
        } catch (BeansException ex) {
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

    protected void prepareInjection(AnnotatedInjectionMetadata metadata) throws BeansException {
        //find and registry bean definition for @DubboReference/@Reference
        for (AnnotatedFieldElement fieldElement : metadata.getFieldElements()) {
            if (fieldElement.injectedObject != null) {
                continue;
            }
            Class<?> injectedType = fieldElement.field.getType();
            AnnotationAttributes attributes = fieldElement.attributes;
            String referenceBeanName = registerReferenceBean(fieldElement.getPropertyName(), injectedType, attributes);

            //associate fieldElement and reference bean
            fieldElement.injectedObject = referenceBeanName;
            injectedFieldReferenceBeanCache.put(fieldElement, referenceBeanName);

        }

        for (AnnotatedMethodElement methodElement : metadata.getMethodElements()) {
            if (methodElement.injectedObject != null) {
                continue;
            }
            Class<?> injectedType = methodElement.getInjectedType();
            AnnotationAttributes attributes = methodElement.attributes;
            String referenceBeanName = registerReferenceBean(methodElement.getPropertyName(), injectedType, attributes);

            //associate fieldElement and reference bean
            methodElement.injectedObject = referenceBeanName;
            injectedMethodReferenceBeanCache.put(methodElement, referenceBeanName);
        }
    }

    private String registerReferenceBean(String propertyName, Class<?> injectedType, AnnotationAttributes attributes) throws BeansException {

        // set default value of interfaceClass: injectedType
        if (!attributes.containsKey("interfaceClass") && !attributes.containsKey("interfaceName")) {
            Assert.isTrue(injectedType.isInterface(),
                    "The class of field or method that was annotated @DubboReference is not an interface!");
            attributes.put("interfaceClass", injectedType);
        }

        // referenceBeanName
        boolean fixedBeanNameFromId = true;
        String referenceBeanName = getAttribute(attributes, "id");
        if (!hasText(referenceBeanName)) {
            referenceBeanName = propertyName;
            fixedBeanNameFromId = false;
        }

        // convert annotation props
        ReferenceBeanManager.convertReferenceProps(attributes);

        // get interface
        String interfaceName = (String) attributes.get("interface");
        if (StringUtils.isBlank(interfaceName)) {
            throw new BeanCreationException("Need to specify the 'interfaceName' or 'interfaceClass' attribute of '@DubboReference'");
        }

        // check reference key
        String referenceKey = referenceBeanManager.generateReferenceKey(attributes);

        // check registered reference beans in referenceBeanManager
        List<String> registeredReferenceBeanNames = referenceBeanManager.getByKey(referenceKey);
        if (registeredReferenceBeanNames.contains(referenceBeanName)) {
            return referenceBeanName;
        }

        //check bean definition
        if (beanDefinitionRegistry.containsBeanDefinition(referenceBeanName)) {
            BeanDefinition prevBeanDefinition = beanDefinitionRegistry.getBeanDefinition(referenceBeanName);
            String prevBeanType = prevBeanDefinition.getBeanClassName();
            String prevBeanDesc = referenceBeanName + "[" + prevBeanType + "]";
            String newBeanDesc = referenceBeanName + "[" + referenceKey + "]";

            if (isReferenceBean(prevBeanDefinition)) {
                //check reference key
                String prevReferenceKey = referenceBeanManager.generateReferenceKey(prevBeanDefinition);
                if (StringUtils.isEquals(prevReferenceKey, referenceKey)) {
                    //found matched dubbo reference bean, ignore register
                    return referenceBeanName;
                }
                //get interfaceName from attribute
                prevBeanType = (String) prevBeanDefinition.getAttribute("interfaceName");
                prevBeanDesc = referenceBeanName + "[" + prevReferenceKey + "]";
                //check bean type
                if (StringUtils.isEquals(prevBeanType, interfaceName)) {
                    throw new BeanCreationException("Already exists another reference bean with the same bean name and type but difference attributes. " +
                            "In order to avoid injection confusion, please modify the name of one of the beans: " +
                            "prev: " + prevBeanDesc + ", new: " + newBeanDesc);
                }
            } else {
                //check bean type
                if (StringUtils.isEquals(prevBeanType, interfaceName)) {
                    throw new BeanCreationException("Already exists another bean definition with the same bean name and type. " +
                            "In order to avoid injection confusion, please modify the name of one of the beans: " +
                            "prev: " + prevBeanDesc + ", new: " + newBeanDesc);
                }
            }

            // bean name from attribute 'id', cannot be renamed
            if (fixedBeanNameFromId) {
                throw new BeanCreationException("Already exists another bean definition with the same bean name, " +
                        "but cannot rename the reference bean name (from the id attribute), please modify the name of one of the beans: " +
                        "prev: " + prevBeanDesc + ", new: " + newBeanDesc);
            }

            // the prev bean type is different, rename the new reference bean
            int index = 2;
            String newReferenceBeanName = null;
            while (newReferenceBeanName == null || beanDefinitionRegistry.containsBeanDefinition(newReferenceBeanName)) {
                newReferenceBeanName = referenceBeanName + "#" + index;
                index++;
            }
            newBeanDesc = newReferenceBeanName + "[" + referenceKey + "]";

            logger.warn("Already exists another bean definition with the same bean name but difference type, " +
                    "rename dubbo reference bean to: " + newReferenceBeanName + ". " +
                    "It is recommended to modify the name of one of the beans to avoid injection problems. " +
                    "prev: " + prevBeanDesc + ", new: " + newBeanDesc);
            referenceBeanName = newReferenceBeanName;
        }
        attributes.put("id", referenceBeanName);
        //save cache, map reference key to referenceBeanName
        referenceBeanManager.registerReferenceBean(referenceKey, referenceBeanName);

        // If registered matched reference before, add alias
        if (registeredReferenceBeanNames.size() > 0) {
            beanDefinitionRegistry.registerAlias(registeredReferenceBeanNames.get(0), referenceBeanName);
            return referenceBeanName;
        }

        //get generic
        Object genericValue = attributes.get("generic");
        String generic = genericValue != null ? genericValue.toString() : null;
        String consumer = (String) attributes.get("consumer");
        if (StringUtils.isBlank(generic) && consumer != null) {
            // get generic from consumerConfig
            BeanDefinition consumerBeanDefinition = getBeanFactory().getBeanDefinition(consumer);
            if (consumerBeanDefinition != null) {
                generic = (String) consumerBeanDefinition.getPropertyValues().get("generic");
            }
        }
        Class interfaceClass = ReferenceConfig.determineInterfaceClass(generic, interfaceName);

        // Register the reference bean definition to the beanFactory
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClassName(ReferenceBean.class.getName());
        beanDefinition.getPropertyValues().add("id", referenceBeanName);

        // set attribute instead of property values
        beanDefinition.setAttribute("referenceProps", attributes);
        beanDefinition.setAttribute("generic", generic);
        beanDefinition.setAttribute("interfaceName", interfaceName);
        beanDefinition.setAttribute("interfaceClass", interfaceClass);

        // create decorated definition for reference bean, Avoid being instantiated when getting the beanType of ReferenceBean
        // refer to org.springframework.beans.factory.support.AbstractBeanFactory#getType()
        GenericBeanDefinition targetDefinition = new GenericBeanDefinition();
        targetDefinition.setBeanClass(interfaceClass);
        String id = (String) beanDefinition.getPropertyValues().get("id");
        beanDefinition.setDecoratedDefinition(new BeanDefinitionHolder(targetDefinition, id+"_decorated"));

        //mark property value as optional
        List<PropertyValue> propertyValues = beanDefinition.getPropertyValues().getPropertyValueList();
        for (PropertyValue propertyValue : propertyValues) {
            propertyValue.setOptional(true);
        }

        beanDefinitionRegistry.registerBeanDefinition(referenceBeanName, beanDefinition);
        logger.info("Register dubbo reference bean: "+referenceBeanName+" = "+referenceKey);
        return referenceBeanName;
    }

    @Override
    protected Object doGetInjectedBean(AnnotationAttributes attributes, Object bean, String beanName, Class<?> injectedType,
                                       AnnotatedInjectElement injectedElement) throws Exception {

        if (injectedElement.injectedObject == null) {
            throw new IllegalStateException("The AnnotatedInjectElement of @DubboReference should be inited before injection");
        }

        return getBeanFactory().getBean((String) injectedElement.injectedObject);
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

//    @Override
//    protected String buildInjectedObjectCacheKey(AnnotationAttributes attributes, Object bean, String beanName,
//                                                 Class<?> injectedType, AnnotatedInjectElement injectedElement) {
//        return generateReferenceBeanKey(attributes, injectedType);
//    }

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
        this.beanDefinitionRegistry = (BeanDefinitionRegistry) applicationContext.getAutowireCapableBeanFactory();
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
        Map<InjectionMetadata.InjectedElement, ReferenceBean<?>> map = new HashMap<>();
        for (Map.Entry<InjectionMetadata.InjectedElement, String> entry : injectedFieldReferenceBeanCache.entrySet()) {
            map.put(entry.getKey(), referenceBeanManager.getById(entry.getValue()));
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * Get {@link ReferenceBean} {@link Map} in injected method.
     *
     * @return non-null {@link Map}
     * @since 2.5.11
     */
    public Map<InjectionMetadata.InjectedElement, ReferenceBean<?>> getInjectedMethodReferenceBeanMap() {
        Map<InjectionMetadata.InjectedElement, ReferenceBean<?>> map = new HashMap<>();
        for (Map.Entry<InjectionMetadata.InjectedElement, String> entry : injectedMethodReferenceBeanCache.entrySet()) {
            map.put(entry.getKey(), referenceBeanManager.getById(entry.getValue()));
        }
        return Collections.unmodifiableMap(map);
    }
}
