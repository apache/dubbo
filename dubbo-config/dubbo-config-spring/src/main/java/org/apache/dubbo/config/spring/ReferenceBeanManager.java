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

import com.alibaba.spring.util.AnnotationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.StringUtils;
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
import org.springframework.util.ObjectUtils;
import org.springframework.validation.DataBinder;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.common.utils.StringUtils.join;

public class ReferenceBeanManager implements ApplicationContextAware {
    public static final String BEAN_NAME = "dubboReferenceBeanManager";
    private final Log logger = LogFactory.getLog(getClass());
    //reference bean id/name ->
    private Map<String, ReferenceBean> referenceIdMap = new ConcurrentHashMap<>();

    //reference key -> [ reference bean names ]
    private Map<String, List<String>> referenceKeyMap = new ConcurrentHashMap<>();

    //reference key -> ReferenceConfig instance
    private Map<String, ReferenceConfig> referenceConfigMap = new ConcurrentHashMap<>();
    private ApplicationContext applicationContext;
    private volatile boolean initialized = false;

    public static void convertReferenceProps(Map<String, Object> attributes) {

        // interface class
        String interfaceClass = (String) attributes.get("interface");
        if (interfaceClass == null) {
            interfaceClass = (String) attributes.get("interfaceName");
        }
        if (interfaceClass == null) {
            Class clazz = (Class) attributes.get("interfaceClass");
            interfaceClass = clazz != null ? clazz.getName() : null;
        }
        Assert.notEmptyString(interfaceClass, "No interface class or name found from attributes");
        attributes.put("interface", interfaceClass);
        attributes.remove("interfaceName");
        attributes.remove("interfaceClass");

        //Specially convert @DubboReference attribute name/value to ReferenceConfig property
        // String[] registry => String registryIds
        String[] registryIds = (String[]) attributes.get("registry");
        if (registryIds != null) {
            String value = join((String[]) registryIds, ",");
            attributes.remove("registry");
            attributes.put("registryIds", value);
        }

    }

    public void addReference(ReferenceBean referenceBean) throws Exception {
        Assert.notEmptyString(referenceBean.getId(), "The id of ReferenceBean cannot be empty");

        if (!initialized) {
            //TODO add issue url to describe early initialization
            logger.warn("Early initialize reference bean before DubboConfigInitializationPostProcessor," +
                    " the BeanPostProcessor has not been loaded at this time, which may cause abnormalities in some components (such as seata): " +
                    referenceBean.getId() + " = " + generateReferenceKey(referenceBean));
        }

        ReferenceBean oldReferenceBean = referenceIdMap.get(referenceBean.getId());
        if (oldReferenceBean != null) {
            if (referenceBean != oldReferenceBean) {
                String oldReferenceKey = generateReferenceKey(oldReferenceBean);
                String newReferenceKey = generateReferenceKey(referenceBean);
                throw new IllegalStateException("Found duplicated ReferenceBean with id: " + referenceBean.getId() +
                        ", old: " + oldReferenceKey + ", new: " + newReferenceKey);
            }
            return;
        }
        referenceIdMap.put(referenceBean.getId(), referenceBean);

        // if add reference after prepareReferenceBeans(), should init it immediately.
        if (initialized) {
            initReferenceBean(referenceBean);
        }
    }

    public void registerReferenceBean(String referenceKey, String referenceBeanName) {
        referenceKeyMap.getOrDefault(referenceKey, new ArrayList<>()).add(referenceBeanName);
    }

    public ReferenceBean getById(String key) {
        return referenceIdMap.get(key);
    }

    public List<String> getByKey(String key) {
        return Collections.unmodifiableList(referenceKeyMap.getOrDefault(key, new ArrayList<>()));
    }

    public Collection<ReferenceBean> getReferences() {
        return referenceIdMap.values();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Initialize all reference beans, call at Dubbo starting
     *
     * @throws Exception
     */
    public void prepareReferenceBeans() throws Exception {
        initialized = true;
        for (ReferenceBean referenceBean : getReferences()) {
            initReferenceBean(referenceBean);
        }

        // prepare all reference beans, including those loaded very early that are dependent on some BeanFactoryPostProcessor
//        Map<String, ReferenceBean> referenceBeanMap = applicationContext.getBeansOfType(ReferenceBean.class, true, false);
//        for (ReferenceBean referenceBean : referenceBeanMap.values()) {
//            addReference(referenceBean);
//        }
    }

    public String generateReferenceKey(Map<String, Object> attributes) {

        String interfaceClass = (String) attributes.get("interface");
        Assert.notEmptyString(interfaceClass, "No interface class or name found from attributes");
        String group = (String) attributes.get("group");
        String version = (String) attributes.get("version");

        //ReferenceBean:group/interface:version
        StringBuilder beanNameBuilder = new StringBuilder("ReferenceBean:");
        if (StringUtils.isNotEmpty(group)) {
            beanNameBuilder.append(group).append("/");
        }
        beanNameBuilder.append(interfaceClass);
        if (StringUtils.isNotEmpty(version)) {
            beanNameBuilder.append(":").append(version);
        }

        // append attributes
        beanNameBuilder.append('(');
        //sort attributes keys
        List<String> sortedAttrKeys = new ArrayList<>(attributes.keySet());
        Collections.sort(sortedAttrKeys);
        List<String> ignoredAttrs = Arrays.asList("id", "group", "version", "interface", "interfaceName", "interfaceClass");
        for (String key : sortedAttrKeys) {
            if (ignoredAttrs.contains(key)) {
                continue;
            }
            Object value = attributes.get(key);

            //Specially convert @DubboReference attribute name/value to ReferenceConfig property
            // String[] registry => String registryIds
//            if ("registry".equals(key)) {
//                key = "registryIds";
//                value = StringUtils.join((String[]) value, ",");
//            }

            value = convertAttribute(key, value);

            beanNameBuilder.append(key)
                    .append('=')
                    .append(value)
                    .append(',');
        }

        // replace the latest "," to be ")"
        if (beanNameBuilder.charAt(beanNameBuilder.length() - 1) == ',') {
            beanNameBuilder.setCharAt(beanNameBuilder.length() - 1, ')');
        } else {
            beanNameBuilder.append(')');
        }

        return beanNameBuilder.toString();
    }

    private Map<String, String> convertParameterPairs(String[] pairArray) {
        Map<String, String> map = new TreeMap<>();
        // parameters spec is {key1,value1,key2,value2}
        for (int i = 0; i < pairArray.length / 2 * 2; i = i + 2) {
            map.put(pairArray[i], pairArray[i + 1]);
        }
        return map;
    }

    private String convertAttribute(String key, Object obj) {
        if (obj == null) {
            return null;
        }
        if ("parameters".equals(key) && obj instanceof String[]) {
            //convert parameters array pairs to map
            obj = convertParameterPairs((String[]) obj);
        }
        //convert bean ref to bean name
        if (obj instanceof RuntimeBeanReference) {
            RuntimeBeanReference beanReference = (RuntimeBeanReference) obj;
            obj = beanReference.getBeanName();
        }

        //to string
        if (obj instanceof Annotation) {
            AnnotationAttributes attributes = AnnotationUtils.getAnnotationAttributes((Annotation) obj, true);
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                entry.setValue(convertAttribute(entry.getKey(), entry.getValue()));
            }
            return String.valueOf(attributes);
        } else if (obj.getClass().isArray()) {
            Object[] array = ObjectUtils.toObjectArray(obj);
            String[] newArray = new String[array.length];
            for (int i = 0; i < array.length; i++) {
                newArray[i] = convertAttribute(null, array[i]);
            }
            Arrays.sort(newArray);
            return Arrays.toString(newArray);
        } else if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            TreeMap newMap = new TreeMap();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                newMap.put(entry.getKey(), convertAttribute(entry.getKey(), entry.getValue()));
            }
            return String.valueOf(newMap);
        } else {
            return String.valueOf(obj);
        }
    }

    public String generateReferenceKey(MutablePropertyValues propertyValues) {
        return generateReferenceKey(convertPropertyValues(propertyValues, applicationContext.getEnvironment()));
    }

    public String generateReferenceKey(ReferenceBean referenceBean) {
        if (referenceBean.getReferenceProps() != null) {
            return generateReferenceKey(referenceBean.getReferenceProps());
        } else {
            return generateReferenceKey(referenceBean.getPropertyValues());
        }
    }

    public String generateReferenceKey(BeanDefinition beanDefinition) {
        if (beanDefinition.hasAttribute("referenceProps")) {
            Map<String, Object> referenceProps = (Map<String, Object>) beanDefinition.getAttribute("referenceProps");
            return generateReferenceKey(referenceProps);
        } else {
            return generateReferenceKey(beanDefinition.getPropertyValues());
        }
    }

    /**
     * NOTE: This method should only call after all dubbo config beans and all property resolvers is loaded.
     *
     * @param referenceBean
     * @throws Exception
     */
    private synchronized void initReferenceBean(ReferenceBean referenceBean) throws Exception {

        if (referenceBean.getReferenceConfig() != null) {
            return;
        }

        // reference key
        String referenceKey = generateReferenceKey(referenceBean);

        ReferenceConfig referenceConfig = referenceConfigMap.get(referenceKey);
        if (referenceConfig == null) {
            //create real ReferenceConfig
            referenceConfig = ReferenceBeanBuilder.create(new AnnotationAttributes(new LinkedHashMap<>(getReferenceAttributes(referenceBean))), applicationContext)
                    .defaultInterfaceClass(referenceBean.getObjectType())
                    .build();
            //cache referenceConfig
            referenceConfigMap.put(referenceKey, referenceConfig);
            // register ReferenceConfig
            DubboBootstrap.getInstance().reference(referenceConfig);
        }

        // associate referenceConfig to referenceBean
        referenceBean.setKeyAndReferenceConfig(referenceKey, referenceConfig);
    }

    private Map<String, Object> getReferenceAttributes(ReferenceBean referenceBean) {
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
        return referenceProps;
    }

    private void resolvePlaceholders(Map<String, Object> referenceProps, PropertyResolver propertyResolver) {
        for (Map.Entry<String, Object> entry : referenceProps.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                String valueToResovle = (String) value;
                entry.setValue(propertyResolver.resolvePlaceholders(valueToResovle));
            } else if (value instanceof String[]) {
                String[] strings = (String[]) value;
                for (int i = 0; i < strings.length; i++) {
                    strings[i] = propertyResolver.resolvePlaceholders(strings[i]);
                }
                entry.setValue(strings);
            }
        }
    }

    /**
     * Convert to raw props, without parsing nested config objects
     */
    private Map<String, Object> convertPropertyValues(MutablePropertyValues propertyValues, PropertyResolver propertyResolver) {
        Map<String, Object> referenceProps = new LinkedHashMap<>();
        for (PropertyValue propertyValue : propertyValues.getPropertyValueList()) {
            String propertyName = propertyValue.getName();
            Object value = propertyValue.getValue();
            if ("methods".equals(propertyName) || "arguments".equals(propertyName)) {
                ManagedList managedList = (ManagedList) value;
                List<Map<String, Object>> elementList = new ArrayList<>();
                for (Object el : managedList) {
                    Map<String, Object> element = convertPropertyValues(((BeanDefinitionHolder) el).getBeanDefinition().getPropertyValues(), propertyResolver);
                    element.remove("id");
                    elementList.add(element);
                }
                value = elementList.toArray(new Object[0]);
            } else if ("parameters".equals(propertyName)) {
                value = createParameterMap((ManagedMap) value, propertyResolver);
            }
            //convert ref
            if (value instanceof RuntimeBeanReference) {
                RuntimeBeanReference beanReference = (RuntimeBeanReference) value;
                value = beanReference.getBeanName();
            }

            if (value == null ||
                    (value instanceof String && StringUtils.isBlank((String) value))
            ) {
                //ignore null or blank string
                continue;
            }

            referenceProps.put(propertyName, value);
        }

        return referenceProps;
    }

    /**
     * Convert to reference props, parsing nested config objects
     */
    private Map<String, Object> toReferenceProps(MutablePropertyValues propertyValues, PropertyResolver propertyResolver) {
        //TODO ReferenceConfig Boolean type (maybe null) cannot mapping to @DubboReference boolean type（not null）
        Map<String, Object> referenceProps = new LinkedHashMap<>();
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
            if (value == null ||
                    (value instanceof String && StringUtils.isBlank((String) value))
            ) {
                //ignore null or blank string
                continue;
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
