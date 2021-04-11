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

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.annotation.Method;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;

import java.beans.PropertyEditorSupport;
import java.util.List;
import java.util.Map;

import static com.alibaba.spring.util.AnnotationUtils.getAttribute;
import static com.alibaba.spring.util.AnnotationUtils.getAttributes;
import static com.alibaba.spring.util.ObjectUtils.of;
import static org.apache.dubbo.config.spring.util.DubboAnnotationUtils.resolveServiceInterfaceClass;
import static org.apache.dubbo.config.spring.util.DubboBeanUtils.getOptionalBean;
import static org.springframework.core.annotation.AnnotationAttributes.fromMap;
import static org.springframework.util.StringUtils.commaDelimitedListToStringArray;

/**
 * {@link ReferenceBean} Builder
 *
 * @since 2.5.7
 */
class ReferenceBeanBuilder extends AnnotatedInterfaceConfigBeanBuilder<ReferenceBean> {

    // Ignore those fields
    static final String[] IGNORE_FIELD_NAMES = of("application", "module", "consumer", "monitor", "registry");

    private ReferenceBeanBuilder(AnnotationAttributes attributes, ApplicationContext applicationContext) {
        super(attributes, applicationContext);
    }

    private void configureInterface(AnnotationAttributes attributes, ReferenceBean referenceBean) {
        Boolean generic = getAttribute(attributes, "generic");
        if (generic != null && generic) {
            // it's a generic reference
            String interfaceClassName = getAttribute(attributes, "interfaceName");
            Assert.hasText(interfaceClassName,
                    "@Reference interfaceName() must be present when reference a generic service!");
            referenceBean.setInterface(interfaceClassName);
            return;
        }

        Class<?> serviceInterfaceClass = resolveServiceInterfaceClass(attributes, interfaceClass);

        Assert.isTrue(serviceInterfaceClass.isInterface(),
                "The class of field or method that was annotated @Reference is not an interface!");

        referenceBean.setInterface(serviceInterfaceClass);

    }


    private void configureConsumerConfig(AnnotationAttributes attributes, ReferenceBean<?> referenceBean) {

        String consumerBeanName = getAttribute(attributes, "consumer");

        ConsumerConfig consumerConfig = getOptionalBean(applicationContext, consumerBeanName, ConsumerConfig.class);

        referenceBean.setConsumer(consumerConfig);

    }

    void configureMethodConfig(AnnotationAttributes attributes, ReferenceBean<?> referenceBean) {
        Method[] methods = (Method[]) attributes.get("methods");
        List<MethodConfig> methodConfigs = MethodConfig.constructMethodConfig(methods);
        if (!methodConfigs.isEmpty()) {
            referenceBean.setMethods(methodConfigs);
        }
    }

    @Override
    protected ReferenceBean doBuild() {
        return new ReferenceBean<Object>();
    }

    @Override
    protected void preConfigureBean(AnnotationAttributes attributes, ReferenceBean referenceBean) {
        Assert.notNull(interfaceClass, "The interface class must set first!");
        DataBinder dataBinder = new DataBinder(referenceBean);
        // Register CustomEditors for special fields
        dataBinder.registerCustomEditor(String.class, "filter", new StringTrimmerEditor(true));
        dataBinder.registerCustomEditor(String.class, "listener", new StringTrimmerEditor(true));
        dataBinder.registerCustomEditor(Map.class, "parameters", new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws java.lang.IllegalArgumentException {
                // Trim all whitespace
                String content = StringUtils.trimAllWhitespace(text);
                if (!StringUtils.hasText(content)) { // No content , ignore directly
                    return;
                }
                // replace "=" to ","
                content = StringUtils.replace(content, "=", ",");
                // replace ":" to ","
                content = StringUtils.replace(content, ":", ",");
                // String[] to Map
                Map<String, String> parameters = CollectionUtils.toStringMap(commaDelimitedListToStringArray(content));
                setValue(parameters);
            }
        });

        // Bind annotation attributes
        dataBinder.bind(new AnnotationPropertyValuesAdapter(attributes, applicationContext.getEnvironment(), IGNORE_FIELD_NAMES));

    }


    @Override
    protected String resolveModuleConfigBeanName(AnnotationAttributes attributes) {
        return getAttribute(attributes, "module");
    }

    @Override
    protected String resolveApplicationConfigBeanName(AnnotationAttributes attributes) {
        return getAttribute(attributes, "application");
    }

    @Override
    protected String[] resolveRegistryConfigBeanNames(AnnotationAttributes attributes) {
        return getAttribute(attributes, "registry");
    }

    @Override
    protected String resolveMonitorConfigBeanName(AnnotationAttributes attributes) {
        return getAttribute(attributes, "monitor");
    }

    @Override
    protected void postConfigureBean(AnnotationAttributes attributes, ReferenceBean bean) throws Exception {

        bean.setApplicationContext(applicationContext);

        configureInterface(attributes, bean);

        configureConsumerConfig(attributes, bean);

        configureMethodConfig(attributes, bean);

        bean.afterPropertiesSet();

    }

    @Deprecated
    public static ReferenceBeanBuilder create(Reference reference, ClassLoader classLoader,
                                              ApplicationContext applicationContext) {
        return create(fromMap(getAttributes(reference, applicationContext.getEnvironment(), true)), applicationContext);
    }

    public static ReferenceBeanBuilder create(AnnotationAttributes attributes, ApplicationContext applicationContext) {
        return new ReferenceBeanBuilder(attributes, applicationContext);
    }
}
