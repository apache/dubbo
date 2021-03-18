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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.config.ArgumentConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.beans.factory.annotation.AnnotationPropertyValuesAdapter;
import org.apache.dubbo.config.spring.beans.factory.annotation.ReferenceBeanBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.validation.DataBinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ReferenceBeanManager implements ApplicationContextAware {
    public static final String BEAN_NAME = "dubboReferenceBeanManager";
    private final Log logger = LogFactory.getLog(getClass());
    private Map<String, ReferenceBean> configMap = new ConcurrentHashMap<>();
    private ApplicationContext applicationContext;
    private volatile boolean initialized = false;


    public void addReference(ReferenceBean referenceBean) throws Exception {
        Assert.notNull(referenceBean.getId(), "The id of ReferenceBean cannot be empty");
        //TODO generate reference bean id and unique cache key
        String key = referenceBean.getId();
        ReferenceBean oldReferenceBean = configMap.get(key);
        if (oldReferenceBean != null) {
            if (referenceBean != oldReferenceBean) {
                logger.warn("Found duplicated ReferenceBean id: " + key);
            }
            return;
        }
        configMap.put(key, referenceBean);

        // if add reference after prepareReferenceBeans(), should init it immediately.
        if (initialized) {
            initReferenceBean(referenceBean);
        }
    }

    public ReferenceBean get(String id) {
        return configMap.get(id);
    }

//    public ReferenceBean getOrCreateReference(Map<String, String> referenceProps) {
//        Integer key = referenceProps.hashCode();
//        return configMap.computeIfAbsent(key, k -> {
//            ReferenceBean referenceBean = new ReferenceBean();
//            referenceBean.setReferenceProps(referenceProps);
//            //referenceBean.setId();
//            return referenceBean;
//        });
//    }

    public Collection<ReferenceBean> getReferences() {
        return configMap.values();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Initialize all reference beans, call at Dubbo starting
     * @throws Exception
     */
    public void prepareReferenceBeans() throws Exception {
        initialized = true;
        for (ReferenceBean referenceBean : getReferences()) {
            initReferenceBean(referenceBean);
        }
    }

    /**
     * NOTE: This method should only call after all dubbo config beans and all property resolvers is loaded.
     *
     * @param referenceBean
     * @throws Exception
     */
    private void initReferenceBean(ReferenceBean referenceBean) throws Exception {

        if (referenceBean.getReferenceConfig() != null) {
            return;
        }

        Environment environment = applicationContext.getEnvironment();
        Map<String, Object> referenceProps = referenceBean.getReferenceProps();
        if (referenceProps == null) {
            MutablePropertyValues propertyValues = referenceBean.getPropertyValues();
            if (propertyValues == null) {
                throw new RuntimeException("ReferenceBean is invalid, missing 'propertyValues'");
            }
            referenceProps = toReferenceProps(propertyValues, environment);
        }

        //resolve placeholders
        resolvePlaceholders(referenceProps, environment);

        //create real ReferenceConfig
        ReferenceConfig referenceConfig = ReferenceBeanBuilder.create(new AnnotationAttributes(new LinkedHashMap<>(referenceProps)), applicationContext)
                .defaultInterfaceClass(referenceBean.getObjectType())
                .build();

        referenceBean.setReferenceConfig(referenceConfig);

        // register ReferenceConfig
        DubboBootstrap.getInstance().reference(referenceConfig);

    }

    private void resolvePlaceholders(Map<String, Object> referenceProps, PropertyResolver propertyResolver) {
        for (Map.Entry<String, Object> entry : referenceProps.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                String valueToResovle = (String) value;
                entry.setValue(propertyResolver.resolveRequiredPlaceholders(valueToResovle));
            } else if (value instanceof String[]) {
                String[] strings = (String[]) value;
                for (int i = 0; i < strings.length; i++) {
                    strings[i] = propertyResolver.resolveRequiredPlaceholders(strings[i]);
                }
                entry.setValue(strings);
            }
        }
    }

    private Map<String, Object> toReferenceProps(MutablePropertyValues propertyValues, PropertyResolver propertyResolver) {
        Map<String, Object> referenceProps;
        referenceProps = new LinkedHashMap<>();
        for (PropertyValue propertyValue : propertyValues.getPropertyValueList()) {
            String propertyName = propertyValue.getName();
            Object value = propertyValue.getValue();
            if ("methods".equals(propertyName)) {
                ManagedList managedList = (ManagedList) value;
                List<MethodConfig> methodConfigs = new ArrayList<>();
                for (Object el : managedList) {
                    MethodConfig methodConfig = createMethodConfig(((BeanDefinitionHolder) el).getBeanDefinition(), propertyResolver);
                    methodConfigs.add(methodConfig);
                }
                value = methodConfigs.toArray(new MethodConfig[0]);
            } else if ("parameters".equals(propertyName)) {
                value = createParameterMap((ManagedMap) value, propertyResolver);
            }
            if (value instanceof RuntimeBeanReference) {
                RuntimeBeanReference beanReference = (RuntimeBeanReference) value;
                value = applicationContext.getBean(beanReference.getBeanName());
            }
            referenceProps.put(propertyName, value);
        }
        return referenceProps;
    }

    private MethodConfig createMethodConfig(BeanDefinition beanDefinition, PropertyResolver propertyResolver) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        MutablePropertyValues pvs = beanDefinition.getPropertyValues();
        for (PropertyValue propertyValue : pvs.getPropertyValueList()) {
            String propertyName = propertyValue.getName();
            Object value = propertyValue.getValue();
            if ("arguments".equals(propertyName)) {
                ManagedList managedList = (ManagedList) value;
                List<ArgumentConfig> argumentConfigs = new ArrayList<>();
                for (Object el : managedList) {
                    ArgumentConfig argumentConfig = createArgumentConfig(((BeanDefinitionHolder) el).getBeanDefinition(), propertyResolver);
                    argumentConfigs.add(argumentConfig);
                }
                value = argumentConfigs.toArray(new ArgumentConfig[0]);
            } else if ("parameters".equals(propertyName)) {
                value = createParameterMap((ManagedMap) value, propertyResolver);
            }

            if (value instanceof RuntimeBeanReference) {
                RuntimeBeanReference beanReference = (RuntimeBeanReference) value;
                value = applicationContext.getBean(beanReference.getBeanName());
            }
            attributes.put(propertyName, value);
        }
        MethodConfig methodConfig = new MethodConfig();
        DataBinder dataBinder = new DataBinder(methodConfig);
        dataBinder.bind(new AnnotationPropertyValuesAdapter(attributes, propertyResolver));
        return methodConfig;
    }

    private ArgumentConfig createArgumentConfig(BeanDefinition beanDefinition, PropertyResolver propertyResolver) {
        ArgumentConfig argumentConfig = new ArgumentConfig();
        DataBinder dataBinder = new DataBinder(argumentConfig);
        dataBinder.bind(beanDefinition.getPropertyValues());
        return argumentConfig;
    }

    private Map<String, String> createParameterMap(ManagedMap managedMap, PropertyResolver propertyResolver) {
        Map<String, String> map = new LinkedHashMap<>();
        Set<Map.Entry<String, TypedStringValue>> entrySet = managedMap.entrySet();
        for (Map.Entry<String, TypedStringValue> entry : entrySet) {
            map.put(entry.getKey(), entry.getValue().getValue());
        }
        return map;
    }


}
