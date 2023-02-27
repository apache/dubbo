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

<<<<<<< HEAD
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
=======

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.StringUtils;
>>>>>>> origin/3.2
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.spring.Constants;
import org.apache.dubbo.config.spring.ReferenceBean;
<<<<<<< HEAD
import org.apache.dubbo.config.spring.ServiceBean;

import com.alibaba.spring.beans.factory.annotation.AbstractAnnotationBeanPostProcessor;
import com.alibaba.spring.util.AnnotationUtils;
=======
import org.apache.dubbo.config.spring.context.event.DubboConfigInitEvent;
import org.apache.dubbo.config.spring.reference.ReferenceAttributes;
import org.apache.dubbo.config.spring.reference.ReferenceBeanManager;
import org.apache.dubbo.config.spring.reference.ReferenceBeanSupport;
import org.apache.dubbo.config.spring.util.SpringCompatUtils;
import org.apache.dubbo.rpc.service.GenericService;
>>>>>>> origin/3.2
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotationAttributes;
<<<<<<< HEAD
import org.springframework.util.ObjectUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
=======
import org.springframework.core.type.MethodMetadata;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
>>>>>>> origin/3.2
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static com.alibaba.spring.util.AnnotationUtils.getAttribute;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_DUBBO_BEAN_INITIALIZER;
import static org.apache.dubbo.common.utils.AnnotationUtils.filterDefaultValues;
import static org.springframework.util.StringUtils.hasText;

/**
 * <p>
 * Step 1:
 * The purpose of implementing {@link BeanFactoryPostProcessor} is to scan the registration reference bean definition earlier,
 * so that it can be shared with the xml bean configuration.
 * </p>
 *
 * <p>
 * Step 2:
 * By implementing {@link org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor},
 * inject the reference bean instance into the fields and setter methods which annotated with {@link DubboReference}.
 * </p>
 *
 * @see DubboReference
 * @see Reference
 * @see com.alibaba.dubbo.config.annotation.Reference
 * @since 2.5.7
 */
<<<<<<< HEAD
public class ReferenceAnnotationBeanPostProcessor extends AbstractAnnotationBeanPostProcessor implements
        ApplicationContextAware, ApplicationListener {

    private static final Logger logger = LoggerFactory.getLogger(ReferenceAnnotationBeanPostProcessor.class);
=======
public class ReferenceAnnotationBeanPostProcessor extends AbstractAnnotationBeanPostProcessor
    implements ApplicationContextAware, BeanFactoryPostProcessor {
>>>>>>> origin/3.2

    /**
     * The bean name of {@link ReferenceAnnotationBeanPostProcessor}
     */
    public static final String BEAN_NAME = "referenceAnnotationBeanPostProcessor";

    /**
     * Cache size
     */
    private static final int CACHE_SIZE = Integer.getInteger(BEAN_NAME + ".cache.size", 32);

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    private final ConcurrentMap<InjectionMetadata.InjectedElement, String> injectedFieldReferenceBeanCache =
        new ConcurrentHashMap<>(CACHE_SIZE);

    private final ConcurrentMap<InjectionMetadata.InjectedElement, String> injectedMethodReferenceBeanCache =
        new ConcurrentHashMap<>(CACHE_SIZE);

    private ApplicationContext applicationContext;

<<<<<<< HEAD
    private static Map<String, TreeSet<String>> referencedBeanNameIdx = new HashMap<>();
=======
    private ReferenceBeanManager referenceBeanManager;
    private BeanDefinitionRegistry beanDefinitionRegistry;
>>>>>>> origin/3.2

    /**
     * {@link com.alibaba.dubbo.config.annotation.Reference @com.alibaba.dubbo.config.annotation.Reference} has been supported since 2.7.3
     * <p>
     * {@link DubboReference @DubboReference} has been supported since 2.7.7
     */
    public ReferenceAnnotationBeanPostProcessor() {
        super(DubboReference.class, Reference.class, com.alibaba.dubbo.config.annotation.Reference.class);
        setClassValuesAsString(false);
        setNestedAnnotationsAsMap(false);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

        String[] beanNames = beanFactory.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Class<?> beanType;
            if (beanFactory.isFactoryBean(beanName)) {
                BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
                if (isReferenceBean(beanDefinition)) {
                    continue;
                }
                if (isAnnotatedReferenceBean(beanDefinition)) {
                    // process @DubboReference at java-config @bean method
                    processReferenceAnnotatedBeanDefinition(beanName, (AnnotatedBeanDefinition) beanDefinition);
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
                    throw new IllegalStateException("Prepare dubbo reference injection element failed", e);
                }
            }
        }

        if (beanFactory instanceof AbstractBeanFactory) {
            List<BeanPostProcessor> beanPostProcessors = ((AbstractBeanFactory) beanFactory).getBeanPostProcessors();
            for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
                if (beanPostProcessor == this) {
                    // This bean has been registered as BeanPostProcessor at org.apache.dubbo.config.spring.context.DubboInfraBeanRegisterPostProcessor.postProcessBeanFactory()
                    // so destroy this bean here, prevent register it as BeanPostProcessor again, avoid cause BeanPostProcessorChecker detection error
                    beanDefinitionRegistry.removeBeanDefinition(BEAN_NAME);
                    break;
                }
            }
        }

        try {
            // this is an early event, it will be notified at org.springframework.context.support.AbstractApplicationContext.registerListeners()
            applicationContext.publishEvent(new DubboConfigInitEvent(applicationContext));
        } catch (Exception e) {
            // if spring version is less than 4.2, it does not support early application event
            logger.warn(CONFIG_DUBBO_BEAN_INITIALIZER, "", "", "publish early application event failed, please upgrade spring version to 4.2.x or later: " + e);
        }
    }

    /**
     * check whether is @DubboReference at java-config @bean method
     */
    private boolean isAnnotatedReferenceBean(BeanDefinition beanDefinition) {
        if (beanDefinition instanceof AnnotatedBeanDefinition) {
            AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) beanDefinition;
            String beanClassName = SpringCompatUtils.getFactoryMethodReturnType(annotatedBeanDefinition);
            if (beanClassName != null && ReferenceBean.class.getName().equals(beanClassName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * process @DubboReference at java-config @bean method
     * <pre class="code">
     * &#064;Configuration
     * public class ConsumerConfig {
     *
     *      &#064;Bean
     *      &#064;DubboReference(group="demo", version="1.2.3")
     *      public ReferenceBean&lt;DemoService&gt; demoService() {
     *          return new ReferenceBean();
     *      }
     *
     * }
     * </pre>
     *
     * @param beanName
     * @param beanDefinition
     */
    private void processReferenceAnnotatedBeanDefinition(String beanName, AnnotatedBeanDefinition beanDefinition) {

        MethodMetadata factoryMethodMetadata = SpringCompatUtils.getFactoryMethodMetadata(beanDefinition);

<<<<<<< HEAD
        referencedBeanNameIdx.computeIfAbsent(referencedBeanName, k -> new TreeSet<String>()).add(referenceBeanName);

        ReferenceBean referenceBean = buildReferenceBeanIfAbsent(referenceBeanName, attributes, injectedType);
=======
        // Extract beanClass from generic return type of java-config bean method: ReferenceBean<DemoService>
        // see org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.getTypeForFactoryBeanFromMethod
        Class beanClass = getBeanFactory().getType(beanName);
        if (beanClass == Object.class) {
            beanClass = SpringCompatUtils.getGenericTypeOfReturnType(factoryMethodMetadata);
        }
        if (beanClass == Object.class) {
            // bean class is invalid, ignore it
            return;
        }
>>>>>>> origin/3.2

        if (beanClass == null) {
            String beanMethodSignature = factoryMethodMetadata.getDeclaringClassName() + "#" + factoryMethodMetadata.getMethodName() + "()";
            throw new BeanCreationException("The ReferenceBean is missing necessary generic type, which returned by the @Bean method of Java-config class. " +
                "The generic type of the returned ReferenceBean must be specified as the referenced interface type, " +
                "such as ReferenceBean<DemoService>. Please check bean method: " + beanMethodSignature);
        }

        // get dubbo reference annotation attributes
        Map<String, Object> annotationAttributes = null;
        // try all dubbo reference annotation types
        for (Class<? extends Annotation> annotationType : getAnnotationTypes()) {
            if (factoryMethodMetadata.isAnnotated(annotationType.getName())) {
                // Since Spring 5.2
                // return factoryMethodMetadata.getAnnotations().get(annotationType).filterDefaultValues().asMap();
                // Compatible with Spring 4.x
                annotationAttributes = factoryMethodMetadata.getAnnotationAttributes(annotationType.getName());
                annotationAttributes = filterDefaultValues(annotationType, annotationAttributes);
                break;
            }
        }

<<<<<<< HEAD
        registerReferenceBean(referencedBeanName, referenceBean, localServiceBean, referenceBeanName);
=======
        if (annotationAttributes != null) {
            // @DubboReference on @Bean method
            LinkedHashMap<String, Object> attributes = new LinkedHashMap<>(annotationAttributes);
            // reset id attribute
            attributes.put(ReferenceAttributes.ID, beanName);
            // convert annotation props
            ReferenceBeanSupport.convertReferenceProps(attributes, beanClass);

            // get interface
            String interfaceName = (String) attributes.get(ReferenceAttributes.INTERFACE);

            // check beanClass and reference interface class
            if (!StringUtils.isEquals(interfaceName, beanClass.getName()) && beanClass != GenericService.class) {
                String beanMethodSignature = factoryMethodMetadata.getDeclaringClassName() + "#" + factoryMethodMetadata.getMethodName() + "()";
                throw new BeanCreationException("The 'interfaceClass' or 'interfaceName' attribute value of @DubboReference annotation " +
                    "is inconsistent with the generic type of the ReferenceBean returned by the bean method. " +
                    "The interface class of @DubboReference is: " + interfaceName + ", but return ReferenceBean<" + beanClass.getName() + ">. " +
                    "Please remove the 'interfaceClass' and 'interfaceName' attributes from @DubboReference annotation. " +
                    "Please check bean method: " + beanMethodSignature);
            }
>>>>>>> origin/3.2

            Class interfaceClass = beanClass;

            // set attribute instead of property values
            beanDefinition.setAttribute(Constants.REFERENCE_PROPS, attributes);
            beanDefinition.setAttribute(ReferenceAttributes.INTERFACE_CLASS, interfaceClass);
            beanDefinition.setAttribute(ReferenceAttributes.INTERFACE_NAME, interfaceName);
        } else {
            // raw reference bean
            // the ReferenceBean is not yet initialized
            beanDefinition.setAttribute(ReferenceAttributes.INTERFACE_CLASS, beanClass);
            if (beanClass != GenericService.class) {
                beanDefinition.setAttribute(ReferenceAttributes.INTERFACE_NAME, beanClass.getName());
            }
        }

<<<<<<< HEAD
        return getBeanFactory().applyBeanPostProcessorsAfterInitialization(referenceBean.get(), referenceBeanName);
    }

    /**
     * Register an instance of {@link ReferenceBean} as a Spring Bean
     *
     * @param referencedBeanName The name of bean that annotated Dubbo's {@link Service @Service} in the Spring {@link ApplicationContext}
     * @param referenceBean      the instance of {@link ReferenceBean} is about to register into the Spring {@link ApplicationContext}
     * @param attributes         the {@link AnnotationAttributes attributes} of {@link Reference @Reference}
     * @param localServiceBean   Is Local Service bean or not
     * @param interfaceClass     the {@link Class class} of Service interface
     * @since 2.7.3
     */
    private void registerReferenceBean(String referencedBeanName, ReferenceBean referenceBean,
                                       boolean localServiceBean, String beanName) {

        ConfigurableListableBeanFactory beanFactory = getBeanFactory();

        if (localServiceBean) {  // If @Service bean is local one
            /**
             * Get  the @Service's BeanDefinition from {@link BeanFactory}
             * Refer to {@link ServiceClassPostProcessor#buildServiceBeanDefinition}
             */
            AbstractBeanDefinition beanDefinition = (AbstractBeanDefinition) beanFactory.getBeanDefinition(referencedBeanName);
            RuntimeBeanReference runtimeBeanReference = (RuntimeBeanReference) beanDefinition.getPropertyValues().get("ref");
            // The name of bean annotated @Service
            String serviceBeanName = runtimeBeanReference.getBeanName();
            // register Alias rather than a new bean name, in order to reduce duplicated beans
            beanFactory.registerAlias(serviceBeanName, beanName);
        } else { // Remote @Service Bean
            if (!beanFactory.containsBean(beanName)) {
                beanFactory.registerSingleton(beanName, referenceBean);
=======
        // set id
        beanDefinition.getPropertyValues().add(ReferenceAttributes.ID, beanName);
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
            } else if (isAnnotatedReferenceBean(beanDefinition)) {
                // extract beanClass from java-config bean method generic return type: ReferenceBean<DemoService>
                //Class beanClass = getBeanFactory().getType(beanName);
            } else {
                AnnotatedInjectionMetadata metadata = findInjectionMetadata(beanName, beanType, null);
                metadata.checkConfigMembers(beanDefinition);
                try {
                    prepareInjection(metadata);
                } catch (Exception e) {
                    throw new IllegalStateException("Prepare dubbo reference injection element failed", e);
                }
>>>>>>> origin/3.2
            }
        }
    }

    /**
     * Alternatives to the {@link #postProcessProperties(PropertyValues, Object, String)}, that removed as of Spring
     * Framework 6.0.0, and in favor of {@link #postProcessProperties(PropertyValues, Object, String)}.
     * <p>In order to be compatible with the lower version of Spring, it is still retained.
     * @see #postProcessProperties
     */
    public PropertyValues postProcessPropertyValues(
        PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {
        return postProcessProperties(pvs, bean, beanName);
    }

    /**
     * Alternatives to the {@link #postProcessPropertyValues(PropertyValues, PropertyDescriptor[], Object, String)}.
     * @see #postProcessPropertyValues
     */
<<<<<<< HEAD
    private String generateReferenceBeanName(AnnotationAttributes attributes, Class<?> interfaceClass) {
        StringBuilder beanNameBuilder = new StringBuilder("@Reference");

        if (!attributes.isEmpty()) {
            beanNameBuilder.append('(');
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                String value;
                if ("parameters".equals(entry.getKey())) {
                    ArrayList<String> pairs = getParameterPairs(entry);
                    value = convertAttribute(pairs.stream().sorted().toArray());
                } else {
                    value = convertAttribute(entry.getValue());
                }
                beanNameBuilder.append(entry.getKey())
                        .append('=')
                        .append(value)
                        .append(',');
            }
            // replace the latest "," to be ")"
            beanNameBuilder.setCharAt(beanNameBuilder.lastIndexOf(","), ')');
=======
    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName)
        throws BeansException {
        try {
            AnnotatedInjectionMetadata metadata = findInjectionMetadata(beanName, bean.getClass(), pvs);
            prepareInjection(metadata);
            metadata.inject(bean, beanName, pvs);
        } catch (BeansException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new BeanCreationException(beanName, "Injection of @" + getAnnotationType().getSimpleName()
                + " dependencies is failed", ex);
>>>>>>> origin/3.2
        }
        return pvs;
    }

<<<<<<< HEAD
    private ArrayList<String> getParameterPairs(Map.Entry<String, Object> entry) {
        String[] entryValues = (String[]) entry.getValue();
        ArrayList<String> pairs = new ArrayList<>();
        // parameters spec is {key1,value1,key2,value2}
        for (int i = 0; i < entryValues.length / 2 * 2; i = i + 2) {
            pairs.add(entryValues[i] + "=" + entryValues[i + 1]);
        }
        return pairs;
    }

    private String convertAttribute(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Annotation) {
            AnnotationAttributes attributes = AnnotationUtils.getAnnotationAttributes((Annotation) obj, true);
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                entry.setValue(convertAttribute(entry.getValue()));
            }
            return String.valueOf(attributes);
        } else if (obj.getClass().isArray()) {
            Object[] array = ObjectUtils.toObjectArray(obj);
            String[] newArray = new String[array.length];
            for (int i = 0; i < array.length; i++) {
                newArray[i] = convertAttribute(array[i]);
            }
            return Arrays.toString(Arrays.stream(newArray).sorted().toArray());
        } else {
            return String.valueOf(obj);
        }
    }

    /**
     * Is Local Service bean or not?
     *
     * @param referencedBeanName the bean name to the referenced bean
     * @return If the target referenced bean is existed, return <code>true</code>, or <code>false</code>
     * @since 2.7.6
     */
    private boolean isLocalServiceBean(String referencedBeanName, ReferenceBean referenceBean, AnnotationAttributes attributes) {
        return existsServiceBean(referencedBeanName) && !isRemoteReferenceBean(referenceBean, attributes);
=======
    private boolean isReferenceBean(BeanDefinition beanDefinition) {
        return ReferenceBean.class.getName().equals(beanDefinition.getBeanClassName());
>>>>>>> origin/3.2
    }

    protected void prepareInjection(AnnotatedInjectionMetadata metadata) throws BeansException {
        try {
            //find and register bean definition for @DubboReference/@Reference
            for (AnnotatedFieldElement fieldElement : metadata.getFieldElements()) {
                if (fieldElement.injectedObject != null) {
                    continue;
                }
                Class<?> injectedType = fieldElement.field.getType();
                AnnotationAttributes attributes = fieldElement.attributes;
                String referenceBeanName = registerReferenceBean(fieldElement.getPropertyName(), injectedType, attributes, fieldElement.field);

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
                String referenceBeanName = registerReferenceBean(methodElement.getPropertyName(), injectedType, attributes, methodElement.method);

                //associate methodElement and reference bean
                methodElement.injectedObject = referenceBeanName;
                injectedMethodReferenceBeanCache.put(methodElement, referenceBeanName);
            }
        } catch (ClassNotFoundException e) {
            throw new BeanCreationException("prepare reference annotation failed", e);
        }
    }

    public String registerReferenceBean(String propertyName, Class<?> injectedType, Map<String, Object> attributes, Member member) throws BeansException {

        boolean renameable = true;
        // referenceBeanName
        String referenceBeanName = getAttribute(attributes, ReferenceAttributes.ID);
        if (hasText(referenceBeanName)) {
            renameable = false;
        } else {
            referenceBeanName = propertyName;
        }

        String checkLocation = "Please check " + member.toString();

        // convert annotation props
        ReferenceBeanSupport.convertReferenceProps(attributes, injectedType);

        // get interface
        String interfaceName = (String) attributes.get(ReferenceAttributes.INTERFACE);
        if (StringUtils.isBlank(interfaceName)) {
            throw new BeanCreationException("Need to specify the 'interfaceName' or 'interfaceClass' attribute of '@DubboReference' if enable generic. " + checkLocation);
        }

        // check reference key
        String referenceKey = ReferenceBeanSupport.generateReferenceKey(attributes, applicationContext);

        // find reference bean name by reference key
        List<String> registeredReferenceBeanNames = referenceBeanManager.getBeanNamesByKey(referenceKey);
        if (registeredReferenceBeanNames.size() > 0) {
            // found same name and reference key
            if (registeredReferenceBeanNames.contains(referenceBeanName)) {
                return referenceBeanName;
            }
        }

        //check bean definition
        if (beanDefinitionRegistry.containsBeanDefinition(referenceBeanName)) {
            BeanDefinition prevBeanDefinition = beanDefinitionRegistry.getBeanDefinition(referenceBeanName);
            String prevBeanType = prevBeanDefinition.getBeanClassName();
            String prevBeanDesc = referenceBeanName + "[" + prevBeanType + "]";
            String newBeanDesc = referenceBeanName + "[" + referenceKey + "]";

            if (isReferenceBean(prevBeanDefinition)) {
                //check reference key
                String prevReferenceKey = ReferenceBeanSupport.generateReferenceKey(prevBeanDefinition, applicationContext);
                if (StringUtils.isEquals(prevReferenceKey, referenceKey)) {
                    //found matched dubbo reference bean, ignore register
                    return referenceBeanName;
                }
                //get interfaceName from attribute
                Assert.notNull(prevBeanDefinition, "The interface class of ReferenceBean is not initialized");
                prevBeanDesc = referenceBeanName + "[" + prevReferenceKey + "]";
            }

            // bean name from attribute 'id' or java-config bean, cannot be renamed
            if (!renameable) {
                throw new BeanCreationException("Already exists another bean definition with the same bean name [" + referenceBeanName + "], " +
                    "but cannot rename the reference bean name (specify the id attribute or java-config bean), " +
                    "please modify the name of one of the beans: " +
                    "prev: " + prevBeanDesc + ", new: " + newBeanDesc + ". " + checkLocation);
            }

            // the prev bean type is different, rename the new reference bean
            int index = 2;
            String newReferenceBeanName = null;
            while (newReferenceBeanName == null || beanDefinitionRegistry.containsBeanDefinition(newReferenceBeanName)
                || beanDefinitionRegistry.isAlias(newReferenceBeanName)) {
                newReferenceBeanName = referenceBeanName + "#" + index;
                index++;
                // double check found same name and reference key
                if (registeredReferenceBeanNames.contains(newReferenceBeanName)) {
                    return newReferenceBeanName;
                }
            }
            newBeanDesc = newReferenceBeanName + "[" + referenceKey + "]";

            logger.warn(CONFIG_DUBBO_BEAN_INITIALIZER, "", "", "Already exists another bean definition with the same bean name [" + referenceBeanName + "], " +
                "rename dubbo reference bean to [" + newReferenceBeanName + "]. " +
                "It is recommended to modify the name of one of the beans to avoid injection problems. " +
                "prev: " + prevBeanDesc + ", new: " + newBeanDesc + ". " + checkLocation);
            referenceBeanName = newReferenceBeanName;
        }
        attributes.put(ReferenceAttributes.ID, referenceBeanName);

<<<<<<< HEAD
    private ReferenceBean buildReferenceBeanIfAbsent(String referenceBeanName, AnnotationAttributes attributes,
                                                     Class<?> referencedType)
            throws Exception {

        ReferenceBean<?> referenceBean = referenceBeanCache.get(referenceBeanName);

        if (referenceBean == null) {
            ReferenceBeanBuilder beanBuilder = ReferenceBeanBuilder
                    .create(attributes, applicationContext)
                    .interfaceClass(referencedType)
                    .beanName(referenceBeanName);
            referenceBean = beanBuilder.build();
            referenceBeanCache.put(referenceBeanName, referenceBean);
        } else if (!referencedType.isAssignableFrom(referenceBean.getInterfaceClass())) {
            throw new IllegalArgumentException("reference bean name " + referenceBeanName + " has been duplicated, but interfaceClass " +
                    referenceBean.getInterfaceClass().getName() + " cannot be assigned to " + referencedType.getName());
=======
        // If registered matched reference before, just register alias
        if (registeredReferenceBeanNames.size() > 0) {
            beanDefinitionRegistry.registerAlias(registeredReferenceBeanNames.get(0), referenceBeanName);
            referenceBeanManager.registerReferenceKeyAndBeanName(referenceKey, referenceBeanName);
            return referenceBeanName;
>>>>>>> origin/3.2
        }

        Class interfaceClass = injectedType;

        // TODO Only register one reference bean for same (group, interface, version)

        // Register the reference bean definition to the beanFactory
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClassName(ReferenceBean.class.getName());
        beanDefinition.getPropertyValues().add(ReferenceAttributes.ID, referenceBeanName);

        // set attribute instead of property values
        beanDefinition.setAttribute(Constants.REFERENCE_PROPS, attributes);
        beanDefinition.setAttribute(ReferenceAttributes.INTERFACE_CLASS, interfaceClass);
        beanDefinition.setAttribute(ReferenceAttributes.INTERFACE_NAME, interfaceName);

        // create decorated definition for reference bean, Avoid being instantiated when getting the beanType of ReferenceBean
        // see org.springframework.beans.factory.support.AbstractBeanFactory#getTypeForFactoryBean()
        GenericBeanDefinition targetDefinition = new GenericBeanDefinition();
        targetDefinition.setBeanClass(interfaceClass);
        beanDefinition.setDecoratedDefinition(new BeanDefinitionHolder(targetDefinition, referenceBeanName + "_decorated"));

        // signal object type since Spring 5.2
        beanDefinition.setAttribute(Constants.OBJECT_TYPE_ATTRIBUTE, interfaceClass);

        beanDefinitionRegistry.registerBeanDefinition(referenceBeanName, beanDefinition);
        referenceBeanManager.registerReferenceKeyAndBeanName(referenceKey, referenceBeanName);
        logger.info("Register dubbo reference bean: " + referenceBeanName + " = " + referenceKey + " at " + member);
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

<<<<<<< HEAD
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            referencedBeanNameIdx.entrySet().stream().filter(e -> e.getValue().size() > 1).forEach(e -> {
                String logPrefix = e.getKey() + " has " + e.getValue().size() + " reference instances, there are: ";
                logger.warn(e.getValue().stream().collect(Collectors.joining(", ", logPrefix, "")));
            });
            referencedBeanNameIdx.clear();
        }
=======
    /**
     * Gets all beans of {@link ReferenceBean}
     *
     * @deprecated use {@link ReferenceBeanManager#getReferences()} instead
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
>>>>>>> origin/3.2
    }
}
