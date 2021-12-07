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

import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.ArgumentConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.annotation.Argument;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.Method;
import org.apache.dubbo.config.spring.beans.factory.annotation.AnnotationPropertyValuesAdapter;
import org.apache.dubbo.config.spring.util.DubboAnnotationUtils;
import org.apache.dubbo.config.spring.util.DubboBeanUtils;
import org.apache.dubbo.rpc.model.ModuleModel;

import com.alibaba.spring.util.AnnotationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;

import java.util.Map;

import static com.alibaba.spring.util.AnnotationUtils.getAttribute;
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
    private final ModuleModel moduleModel;

    private ReferenceCreator(Map<String, Object> attributes, ApplicationContext applicationContext) {
        Assert.notNull(attributes, "The Annotation attributes must not be null!");
        Assert.notNull(applicationContext, "The ApplicationContext must not be null!");
        this.attributes = attributes;
        this.applicationContext = applicationContext;
        this.classLoader = applicationContext.getClassLoader() != null ?
                applicationContext.getClassLoader() : Thread.currentThread().getContextClassLoader();
        moduleModel = DubboBeanUtils.getModuleModel(applicationContext);
        Assert.notNull(moduleModel, "ModuleModel not found in Spring ApplicationContext");
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

        populateBean(configBean);

        // deprecate application reference
        //configureApplicationConfig(configBean);

        configureMonitorConfig(configBean);

        configureModuleConfig(configBean);

        configureConsumerConfig(configBean);

    }

    private void configureMonitorConfig(ReferenceConfig configBean) {
        String monitorConfigId = getAttribute(attributes, "monitor");
        if (StringUtils.hasText(monitorConfigId)) {
            MonitorConfig monitorConfig = getConfig(monitorConfigId, MonitorConfig.class);
            configBean.setMonitor(monitorConfig);
        }
    }

//    private void configureApplicationConfig(ReferenceConfig configBean) {
//        String applicationConfigId = getAttribute(attributes, "application");
//        if (StringUtils.hasText(applicationConfigId)) {
//            ApplicationConfig applicationConfig = getConfig(applicationConfigId, ApplicationConfig.class);
//            configBean.setApplication(applicationConfig);
//        }
//    }

    private void configureModuleConfig(ReferenceConfig configBean) {
        String moduleConfigId = getAttribute(attributes, "module");
        if (StringUtils.hasText(moduleConfigId)) {
            ModuleConfig moduleConfig = getConfig(moduleConfigId, ModuleConfig.class);
            configBean.setModule(moduleConfig);
        }
    }

    private void configureConsumerConfig(ReferenceConfig<?> referenceBean) {
        ConsumerConfig consumerConfig = null;
        Object consumer = getAttribute(attributes, "consumer");
        if (consumer != null) {
            if (consumer instanceof String) {
                consumerConfig = getConfig((String) consumer, ConsumerConfig.class);
            } else if (consumer instanceof ConsumerConfig) {
                consumerConfig = (ConsumerConfig) consumer;
            } else {
                throw new IllegalArgumentException("Unexpected 'consumer' attribute value: "+consumer);
            }
            referenceBean.setConsumer(consumerConfig);
        }
    }

    private <T extends AbstractConfig> T getConfig(String configIdOrName, Class<T> configType) {
        // 1. find in ModuleConfigManager
        T config = moduleModel.getConfigManager().getConfig(configType, configIdOrName).orElse(null);
        if (config == null) {
            // 2. find in Spring ApplicationContext
            if (applicationContext.containsBean(configIdOrName)) {
                config = applicationContext.getBean(configIdOrName, configType);
            }
        }
        if (config == null) {
            throw new IllegalArgumentException(configType.getSimpleName() + " not found: " + configIdOrName);
        }
        return config;
    }

    protected void populateBean(ReferenceConfig referenceBean) {
        Assert.notNull(defaultInterfaceClass, "The default interface class cannot be empty!");
        // convert attributes, e.g. interface, registry
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
