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
package org.apache.dubbo.config.spring.reference;

import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.spring.Constants;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.util.DubboAnnotationUtils;
import org.apache.dubbo.rpc.service.GenericService;

import com.alibaba.spring.util.AnnotationUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.ObjectUtils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.apache.dubbo.common.utils.StringUtils.join;

public class ReferenceBeanSupport {

    public static void convertReferenceProps(Map<String, Object> attributes, Class defaultInterfaceClass) {

        // interface class
        String interfaceName = (String) attributes.get(ReferenceAttributes.INTERFACE);
        if (interfaceName == null) {
            interfaceName = (String) attributes.get(ReferenceAttributes.INTERFACE_NAME);
        }
        if (interfaceName == null) {
            Object interfaceClassValue = attributes.get(ReferenceAttributes.INTERFACE_CLASS);
            if (interfaceClassValue instanceof Class) {
                interfaceName = ((Class) interfaceClassValue).getName();
            } else if (interfaceClassValue instanceof String) {
                if (interfaceClassValue.equals("void")) {
                    attributes.remove(ReferenceAttributes.INTERFACE_CLASS);
                } else {
                    interfaceName = (String) interfaceClassValue;
                }
            }
        }
        if (interfaceName == null && defaultInterfaceClass != GenericService.class) {
            interfaceName = defaultInterfaceClass.getName();
        }
        Assert.notEmptyString(interfaceName, "The interface class or name of reference was not found");
        attributes.put(ReferenceAttributes.INTERFACE, interfaceName);
        attributes.remove(ReferenceAttributes.INTERFACE_NAME);
        attributes.remove(ReferenceAttributes.INTERFACE_CLASS);

        //reset generic value
        String generic = String.valueOf(defaultInterfaceClass == GenericService.class);
        String oldGeneric = attributes.containsValue(ReferenceAttributes.GENERIC) ?
            String.valueOf(attributes.get(ReferenceAttributes.GENERIC)) : "false";
        if (!StringUtils.isEquals(oldGeneric, generic)) {
            attributes.put(ReferenceAttributes.GENERIC, generic);
        }

        //Specially convert @DubboReference attribute name/value to ReferenceConfig property
        // String[] registry => String registryIds
        String[] registryIds = (String[]) attributes.get(ReferenceAttributes.REGISTRY);
        if (registryIds != null) {
            String value = join((String[]) registryIds, ",");
            attributes.remove(ReferenceAttributes.REGISTRY);
            attributes.put(ReferenceAttributes.REGISTRY_IDS, value);
        }

    }

    public static String generateReferenceKey(Map<String, Object> attributes, ApplicationContext applicationContext) {

        String interfaceClass = (String) attributes.get(ReferenceAttributes.INTERFACE);
        Assert.notEmptyString(interfaceClass, "No interface class or name found from attributes");
        String group = (String) attributes.get(ReferenceAttributes.GROUP);
        String version = (String) attributes.get(ReferenceAttributes.VERSION);

        //ReferenceBean:group/interface:version
        StringBuilder beanNameBuilder = new StringBuilder("ReferenceBean:");
        if (StringUtils.isNotEmpty(group)) {
            beanNameBuilder.append(group).append('/');
        }
        beanNameBuilder.append(interfaceClass);
        if (StringUtils.isNotEmpty(version)) {
            beanNameBuilder.append(':').append(version);
        }

        // append attributes
        beanNameBuilder.append('(');
        //sort attributes keys
        List<String> sortedAttrKeys = new ArrayList<>(attributes.keySet());
        Collections.sort(sortedAttrKeys);
        List<String> ignoredAttrs = Arrays.asList(ReferenceAttributes.ID, ReferenceAttributes.GROUP,
            ReferenceAttributes.VERSION, ReferenceAttributes.INTERFACE, ReferenceAttributes.INTERFACE_NAME,
            ReferenceAttributes.INTERFACE_CLASS);
        for (String key : sortedAttrKeys) {
            if (ignoredAttrs.contains(key)) {
                continue;
            }
            Object value = attributes.get(key);
            value = convertToString(key, value);

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

        String referenceKey = beanNameBuilder.toString();
        if (applicationContext != null) {
            // resolve placeholder with Spring Environment
            referenceKey = applicationContext.getEnvironment().resolvePlaceholders(referenceKey);
            // resolve placeholder with Spring BeanFactory ( using PropertyResourceConfigurer/PropertySourcesPlaceholderConfigurer )
            referenceKey = ((AbstractBeanFactory) applicationContext.getAutowireCapableBeanFactory()).resolveEmbeddedValue(referenceKey);
        }
        // The property placeholder maybe not resolved if is early init
        // if (referenceKey != null && referenceKey.contains("${")) {
        //     throw new IllegalStateException("Reference key contains unresolved placeholders ${..} : " + referenceKey);
        // }
        return referenceKey;
    }

    private static String convertToString(String key, Object obj) {
        if (obj == null) {
            return null;
        }
        if (ReferenceAttributes.PARAMETERS.equals(key) && obj instanceof String[]) {
            //convert parameters array pairs to map
            obj = DubboAnnotationUtils.convertParameters((String[]) obj);
        }

        //to string
        if (obj instanceof Annotation) {
            AnnotationAttributes attributes = AnnotationUtils.getAnnotationAttributes((Annotation) obj, true);
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                entry.setValue(convertToString(entry.getKey(), entry.getValue()));
            }
            return String.valueOf(attributes);
        } else if (obj.getClass().isArray()) {
            Object[] array = ObjectUtils.toObjectArray(obj);
            String[] newArray = new String[array.length];
            for (int i = 0; i < array.length; i++) {
                newArray[i] = convertToString(null, array[i]);
            }
            Arrays.sort(newArray);
            return Arrays.toString(newArray);
        } else if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            TreeMap newMap = new TreeMap();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                newMap.put(entry.getKey(), convertToString(entry.getKey(), entry.getValue()));
            }
            return String.valueOf(newMap);
        } else {
            return String.valueOf(obj);
        }
    }

    /**
     * Convert to raw props, without parsing nested config objects
     */
    public static Map<String, Object> convertPropertyValues(MutablePropertyValues propertyValues) {
        Map<String, Object> referenceProps = new LinkedHashMap<>();
        for (PropertyValue propertyValue : propertyValues.getPropertyValueList()) {
            String propertyName = propertyValue.getName();
            Object value = propertyValue.getValue();
            if (ReferenceAttributes.METHODS.equals(propertyName) || ReferenceAttributes.ARGUMENTS.equals(propertyName)) {
                ManagedList managedList = (ManagedList) value;
                List<Map<String, Object>> elementList = new ArrayList<>();
                for (Object el : managedList) {
                    Map<String, Object> element = convertPropertyValues(((BeanDefinitionHolder) el).getBeanDefinition().getPropertyValues());
                    element.remove(ReferenceAttributes.ID);
                    elementList.add(element);
                }
                value = elementList.toArray(new Object[0]);
            } else if (ReferenceAttributes.PARAMETERS.equals(propertyName)) {
                value = createParameterMap((ManagedMap) value);
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

    private static Map<String, String> createParameterMap(ManagedMap managedMap) {
        Map<String, String> map = new LinkedHashMap<>();
        Set<Map.Entry<String, TypedStringValue>> entrySet = managedMap.entrySet();
        for (Map.Entry<String, TypedStringValue> entry : entrySet) {
            map.put(entry.getKey(), entry.getValue().getValue());
        }
        return map;
    }

    public static String generateReferenceKey(ReferenceBean referenceBean, ApplicationContext applicationContext) {
        return generateReferenceKey(getReferenceAttributes(referenceBean), applicationContext);
    }

    public static String generateReferenceKey(BeanDefinition beanDefinition, ApplicationContext applicationContext) {
        return generateReferenceKey(getReferenceAttributes(beanDefinition), applicationContext);
    }

    public static Map<String, Object> getReferenceAttributes(ReferenceBean referenceBean) {
        Map<String, Object> referenceProps = referenceBean.getReferenceProps();
        if (referenceProps == null) {
            MutablePropertyValues propertyValues = referenceBean.getPropertyValues();
            if (propertyValues == null) {
                throw new RuntimeException("ReferenceBean is invalid, 'referenceProps' and 'propertyValues' cannot both be empty.");
            }
            referenceProps = convertPropertyValues(propertyValues);
        }
        return referenceProps;
    }

    public static Map<String, Object> getReferenceAttributes(BeanDefinition beanDefinition) {
        Map<String, Object> referenceProps = null;
        if (beanDefinition.hasAttribute(Constants.REFERENCE_PROPS)) {
            referenceProps = (Map<String, Object>) beanDefinition.getAttribute(Constants.REFERENCE_PROPS);
        } else {
            referenceProps = convertPropertyValues(beanDefinition.getPropertyValues());
        }
        return referenceProps;
    }
}
