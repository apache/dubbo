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
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;

import java.beans.PropertyEditorSupport;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.config.spring.util.BeanFactoryUtils.getOptionalBean;
import static org.apache.dubbo.config.spring.util.ObjectUtils.of;
import static org.springframework.util.StringUtils.commaDelimitedListToStringArray;

/**
 * {@link ReferenceBean} Builder
 *
 * @since 2.5.7
 */
class ReferenceBeanBuilder extends AbstractAnnotationConfigBeanBuilder<Reference, ReferenceBean> {

    // Ignore those fields
    static final String[] IGNORE_FIELD_NAMES = of("application", "module", "consumer", "monitor", "registry");

    private ReferenceBeanBuilder(Reference annotation, ClassLoader classLoader, ApplicationContext applicationContext) {
        super(annotation, classLoader, applicationContext);
    }

    private void configureInterface(Reference reference, ReferenceBean referenceBean) {

        Class<?> interfaceClass = reference.interfaceClass();

        if (void.class.equals(interfaceClass)) {

            interfaceClass = null;

            String interfaceClassName = reference.interfaceName();

            if (StringUtils.hasText(interfaceClassName)) {
                if (ClassUtils.isPresent(interfaceClassName, classLoader)) {
                    interfaceClass = ClassUtils.resolveClassName(interfaceClassName, classLoader);
                }
            }

        }

        if (interfaceClass == null) {
            interfaceClass = this.interfaceClass;
        }

        Assert.isTrue(interfaceClass.isInterface(),
                "The class of field or method that was annotated @Reference is not an interface!");

        referenceBean.setInterface(interfaceClass);

    }


    private void configureConsumerConfig(Reference reference, ReferenceBean<?> referenceBean) {

        String consumerBeanName = reference.consumer();

        ConsumerConfig consumerConfig = getOptionalBean(applicationContext, consumerBeanName, ConsumerConfig.class);

        referenceBean.setConsumer(consumerConfig);

    }

    void configureMethodConfig(Reference reference, ReferenceBean<?> referenceBean){
        Method[] methods = reference.methods();
        List<MethodConfig> methodConfigs = MethodConfig.constructMethodConfig(methods);
        if(!methodConfigs.isEmpty()){
            referenceBean.setMethods(methodConfigs);
        }
    }

    @Override
    protected ReferenceBean doBuild() {
        return new ReferenceBean<Object>();
    }

    @Override
    protected void preConfigureBean(Reference reference, ReferenceBean referenceBean) {
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
        dataBinder.bind(new AnnotationPropertyValuesAdapter(reference, applicationContext.getEnvironment(), IGNORE_FIELD_NAMES));

    }


    @Override
    protected String resolveModuleConfigBeanName(Reference annotation) {
        return annotation.module();
    }

    @Override
    protected String resolveApplicationConfigBeanName(Reference annotation) {
        return annotation.application();
    }

    @Override
    protected String[] resolveRegistryConfigBeanNames(Reference annotation) {
        return annotation.registry();
    }

    @Override
    protected String resolveMonitorConfigBeanName(Reference annotation) {
        return annotation.monitor();
    }

    @Override
    protected void postConfigureBean(Reference annotation, ReferenceBean bean) throws Exception {

        bean.setApplicationContext(applicationContext);

        configureInterface(annotation, bean);

        configureConsumerConfig(annotation, bean);

        configureMethodConfig(annotation, bean);

        bean.afterPropertiesSet();

    }

    public static ReferenceBeanBuilder create(Reference annotation, ClassLoader classLoader,
                                              ApplicationContext applicationContext) {
        return new ReferenceBeanBuilder(annotation, classLoader, applicationContext);
    }

}
