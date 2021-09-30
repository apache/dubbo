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

import com.alibaba.spring.util.AnnotationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ArgumentConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.annotation.Argument;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.Method;
import org.apache.dubbo.config.spring.beans.factory.annotation.AnnotationPropertyValuesAdapter;
import org.apache.dubbo.config.spring.util.DubboAnnotationUtils;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.alibaba.spring.util.AnnotationUtils.getAttribute;
import static com.alibaba.spring.util.BeanFactoryUtils.getBeans;
import static com.alibaba.spring.util.BeanFactoryUtils.getOptionalBean;
import static com.alibaba.spring.util.ObjectUtils.of;

/**
 * {@link ReferenceConfig} Creator for @{@link DubboReference}
 *
 * @since 3.0
 */
public class ReferenceCreator {

    // Ignore those fields
    static final String[] IGNORE_FIELD_NAMES = of("application", "module", "consumer", "monitor", "registry", "interfaceClass");

    private static final String ONRETURN = "onreturn";
    private static final String ONTHROW = "onthrow";
    private static final String ONINVOKE = "oninvoke";
    private static final String METHOD = "Method";

    protected final Log logger = LogFactory.getLog(getClass());

    protected final Map<String, Object> attributes;

    protected final ApplicationContext applicationContext;

    protected final ClassLoader classLoader;

    protected Class<?> defaultInterfaceClass;

    private ReferenceCreator(Map<String, Object> attributes, ApplicationContext applicationContext) {
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

        //configureRegistryConfigs(configBean);

        configureMonitorConfig(configBean);

        configureApplicationConfig(configBean);

        configureModuleConfig(configBean);

        //interfaceClass
        //configureInterface(attributes, configBean);

        configureConsumerConfig(attributes, configBean);

        //configureMethodConfig(attributes, configBean);

        //bean.setApplicationContext(applicationContext);
        //bean.afterPropertiesSet();

    }

    private void configureRegistryConfigs(ReferenceConfig configBean) {

        String[] registryConfigBeanIds = getAttribute(attributes, "registry");
        if (registryConfigBeanIds != null) {
            List<RegistryConfig> registryConfigs = getBeans(applicationContext, registryConfigBeanIds, RegistryConfig.class);
            configBean.setRegistries(registryConfigs);
        }

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

    private void configureInterface(Map<String, Object> attributes, ReferenceConfig referenceBean) {
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


    private void configureConsumerConfig(Map<String, Object> attributes, ReferenceConfig<?> referenceBean) {
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

    void configureMethodConfig(Map<String, Object> attributes, ReferenceConfig<?> referenceBean) {
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

    protected void populateBean(Map<String, Object> attributes, ReferenceConfig referenceBean) {
        Assert.notNull(defaultInterfaceClass, "The default interface class cannot be empty!");
        ReferenceBeanSupport.convertReferenceProps(attributes, defaultInterfaceClass);

        DataBinder dataBinder = new DataBinder(referenceBean);
        // Register CustomEditors for special fields
        dataBinder.registerCustomEditor(String.class, "filter", new StringTrimmerEditor(true));
        dataBinder.registerCustomEditor(String.class, "listener", new StringTrimmerEditor(true));

        DefaultConversionService conversionService = new DefaultConversionService();

        // convert String[] to Map (such as @Method.parameters())
        conversionService.addConverter(String[].class, Map.class, DubboAnnotationUtils::convertParameters);

        //convert Map to MethodConfig
        conversionService.addConverter(Map.class, MethodConfig.class, source -> createMethodConfig(source, conversionService));

        //convert @Method to MethodConfig
        conversionService.addConverter(Method.class, MethodConfig.class, source -> {
            Map<String, Object> methodAttributes = AnnotationUtils.getAnnotationAttributes(source, true);
            return createMethodConfig(methodAttributes, conversionService);
        });

        //convert Map to ArgumentConfig
        conversionService.addConverter(Map.class, ArgumentConfig.class, source -> {
            ArgumentConfig argumentConfig = new ArgumentConfig();
            DataBinder argDataBinder = new DataBinder(argumentConfig);
            argDataBinder.setConversionService(conversionService);
            argDataBinder.bind(new AnnotationPropertyValuesAdapter(source, applicationContext.getEnvironment()));
            return argumentConfig;
        });

        //convert @Argument to ArgumentConfig
        conversionService.addConverter(Argument.class, ArgumentConfig.class, source -> {
            ArgumentConfig argumentConfig = new ArgumentConfig();
            DataBinder argDataBinder = new DataBinder(argumentConfig);
            argDataBinder.setConversionService(conversionService);
            argDataBinder.bind(new AnnotationPropertyValuesAdapter(source, applicationContext.getEnvironment()));
            return argumentConfig;
        });

        // Bind annotation attributes
        dataBinder.setConversionService(conversionService);
        dataBinder.bind(new AnnotationPropertyValuesAdapter(attributes, applicationContext.getEnvironment(), IGNORE_FIELD_NAMES));

    }

    private MethodConfig createMethodConfig(Map<String, Object> methodAttributes, DefaultConversionService conversionService) {
        String[] callbacks = new String[]{ONINVOKE, ONRETURN, ONTHROW};
        for (String callbackName : callbacks) {
            Object value = methodAttributes.get(callbackName);
            if (value instanceof String) {
                //parse callback: beanName.methodName
                String strValue = (String) value;
                int index = strValue.lastIndexOf(".");
                if (index != -1) {
                    String beanName = strValue.substring(0, index);
                    String methodName = strValue.substring(index + 1);
                    methodAttributes.put(callbackName, applicationContext.getBean(beanName));
                    methodAttributes.put(callbackName+METHOD, methodName);
                } else {
                    methodAttributes.put(callbackName, applicationContext.getBean(strValue));
                }
            }
        }

        MethodConfig methodConfig = new MethodConfig();
        DataBinder mcDataBinder = new DataBinder(methodConfig);
        mcDataBinder.setConversionService(conversionService);
        AnnotationPropertyValuesAdapter propertyValues = new AnnotationPropertyValuesAdapter(methodAttributes, applicationContext.getEnvironment());
        mcDataBinder.bind(propertyValues);
        return methodConfig;
    }

    public static ReferenceCreator create(Map<String, Object> attributes, ApplicationContext applicationContext) {
        return new ReferenceCreator(attributes, applicationContext);
    }

    public ReferenceCreator defaultInterfaceClass(Class<?> interfaceClass) {
        this.defaultInterfaceClass = interfaceClass;
        return this;
    }

}
