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
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.Method;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;

import java.beans.PropertyEditorSupport;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.alibaba.spring.util.AnnotationUtils.getAttribute;
import static com.alibaba.spring.util.BeanFactoryUtils.getBeans;
import static com.alibaba.spring.util.BeanFactoryUtils.getOptionalBean;
import static com.alibaba.spring.util.ObjectUtils.of;
import static org.springframework.util.StringUtils.commaDelimitedListToStringArray;

/**
 * {@link ReferenceConfig} Builder for @{@link DubboReference}
 *
 * @since 3.0
 */
public class ReferenceBeanBuilder {

    // Ignore those fields
    static final String[] IGNORE_FIELD_NAMES = of("application", "module", "consumer", "monitor", "registry", "interfaceClass");

    protected final Log logger = LogFactory.getLog(getClass());

    protected final AnnotationAttributes attributes;

    protected final ApplicationContext applicationContext;

    protected final ClassLoader classLoader;

    protected Class<?> defaultInterfaceClass;

    private ReferenceBeanBuilder(AnnotationAttributes attributes, ApplicationContext applicationContext) {
        Assert.notNull(attributes, "The Annotation attributes must not be null!");
        Assert.notNull(applicationContext, "The ApplicationContext must not be null!");
        this.attributes = attributes;
        this.applicationContext = applicationContext;
        this.classLoader = applicationContext.getClassLoader() != null ?
                applicationContext.getClassLoader() : Thread.currentThread().getContextClassLoader();
    }

    public final ReferenceConfig build() throws Exception {

        ReferenceConfig configBean = new ReferenceConfig();

        configureBean(configBean);

        if (logger.isInfoEnabled()) {
            logger.info("The configBean[type:" + configBean.getClass().getSimpleName() + "] has been built.");
        }

        return configBean;

    }

    protected void configureBean(ReferenceConfig configBean) throws Exception {

        populateBean(attributes, configBean);

        configureRegistryConfigs(configBean);

        configureMonitorConfig(configBean);

        configureApplicationConfig(configBean);

        configureModuleConfig(configBean);

        //interfaceClass
        configureInterface(attributes, configBean);

        configureConsumerConfig(attributes, configBean);

        configureMethodConfig(attributes, configBean);

        //bean.setApplicationContext(applicationContext);
        //bean.afterPropertiesSet();

    }

    private void configureRegistryConfigs(ReferenceConfig configBean) {

        String[] registryConfigBeanIds = getAttribute(attributes, "registry");

        List<RegistryConfig> registryConfigs = getBeans(applicationContext, registryConfigBeanIds, RegistryConfig.class);

        configBean.setRegistries(registryConfigs);

    }

    private void configureMonitorConfig(ReferenceConfig configBean) {

        String monitorBeanName = getAttribute(attributes, "monitor");

        MonitorConfig monitorConfig = getOptionalBean(applicationContext, monitorBeanName, MonitorConfig.class);

        configBean.setMonitor(monitorConfig);

    }

    private void configureApplicationConfig(ReferenceConfig configBean) {

        String applicationConfigBeanName = getAttribute(attributes, "application");

        ApplicationConfig applicationConfig =
                getOptionalBean(applicationContext, applicationConfigBeanName, ApplicationConfig.class);

        configBean.setApplication(applicationConfig);

    }

    private void configureModuleConfig(ReferenceConfig configBean) {

        String moduleConfigBeanName = getAttribute(attributes, "module");

        ModuleConfig moduleConfig =
                getOptionalBean(applicationContext, moduleConfigBeanName, ModuleConfig.class);

        configBean.setModule(moduleConfig);

    }

    private void configureInterface(AnnotationAttributes attributes, ReferenceConfig referenceBean) {
        if (referenceBean.getInterface() == null) {

            Object genericValue = getAttribute(attributes, "generic");
            String generic = (genericValue != null) ? genericValue.toString() : null;
            referenceBean.setGeneric(generic);

            String interfaceClassName = getAttribute(attributes, "interfaceName");
            if (StringUtils.hasText(interfaceClassName)) {
                referenceBean.setInterface(interfaceClassName);
            } else {
                Class<?> interfaceClass = getAttribute(attributes, "interfaceClass");
                if (void.class.equals(interfaceClass)) { // default or set void.class for purpose.
                    interfaceClass = null;
                }
                if (interfaceClass != null) {
                    Assert.isTrue(interfaceClass.isInterface(),
                            "The interfaceClass of @DubboReference is not an interface: "+interfaceClass.getName());
                }
                // Not present 'interfaceClass' attribute, use default injection type of annotated
                if (interfaceClass == null && defaultInterfaceClass != null) {
                    interfaceClass = defaultInterfaceClass;
                    Assert.isTrue(interfaceClass.isInterface(),
                            "The class of field or method that was annotated @DubboReference is not an interface!");
                }
                // Convert to interface class name, InterfaceClass will be determined later
                referenceBean.setInterface(interfaceClass.getName());
            }
        }
    }


    private void configureConsumerConfig(AnnotationAttributes attributes, ReferenceConfig<?> referenceBean) {
        ConsumerConfig consumerConfig = null;
        Object consumer = getAttribute(attributes, "consumer");
        if (consumer != null) {
            if (consumer instanceof String) {
                consumerConfig = getOptionalBean(applicationContext, (String) consumer, ConsumerConfig.class);
            } else if (consumer instanceof ConsumerConfig) {
                consumerConfig = (ConsumerConfig) consumer;
            } else {
                throw new IllegalArgumentException("Unexpected 'consumer' attribute value: "+consumer);
            }
            referenceBean.setConsumer(consumerConfig);
        }
    }

    void configureMethodConfig(AnnotationAttributes attributes, ReferenceConfig<?> referenceBean) {
        Object value = attributes.get("methods");
        if (value instanceof Method[]) {
            Method[] methods = (Method[]) value;
            List<MethodConfig> methodConfigs = MethodConfig.constructMethodConfig(methods);
            if (!methodConfigs.isEmpty()) {
                referenceBean.setMethods(methodConfigs);
            }
        } else if (value instanceof MethodConfig[]) {
            MethodConfig[] methodConfigs = (MethodConfig[]) value;
            referenceBean.setMethods(Arrays.asList(methodConfigs));
        }
    }

    protected void populateBean(AnnotationAttributes attributes, ReferenceConfig referenceBean) {
        Assert.notNull(defaultInterfaceClass, "The default interface class must set first!");
        DataBinder dataBinder = new DataBinder(referenceBean);
        // Register CustomEditors for special fields
        dataBinder.registerCustomEditor(String.class, "filter", new StringTrimmerEditor(true));
        dataBinder.registerCustomEditor(String.class, "listener", new StringTrimmerEditor(true));
        dataBinder.registerCustomEditor(Map.class, "parameters", new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
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

    public static ReferenceBeanBuilder create(AnnotationAttributes attributes, ApplicationContext applicationContext) {
        return new ReferenceBeanBuilder(attributes, applicationContext);
    }

    public ReferenceBeanBuilder defaultInterfaceClass(Class<?> interfaceClass) {
        this.defaultInterfaceClass = interfaceClass;
        return this;
    }

}
