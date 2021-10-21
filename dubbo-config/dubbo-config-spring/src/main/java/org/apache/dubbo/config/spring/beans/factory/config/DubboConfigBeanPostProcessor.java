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
package org.apache.dubbo.config.spring.beans.factory.config;

import com.alibaba.spring.beans.factory.config.GenericBeanPostProcessorAdapter;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.AbstractMethodConfig;
import org.apache.dubbo.config.Constants;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.spring.util.DubboBeanUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

import javax.annotation.PostConstruct;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;

import static com.alibaba.spring.util.ObjectUtils.of;
import static org.springframework.aop.support.AopUtils.getTargetClass;
import static org.springframework.beans.BeanUtils.getPropertyDescriptor;
import static org.springframework.util.ReflectionUtils.invokeMethod;

/**
 * The {@link BeanPostProcessor} class for the default property value of {@link AbstractConfig Dubbo's Config Beans}
 *
 * @since 2.7.6
 */
public class DubboConfigBeanPostProcessor extends GenericBeanPostProcessorAdapter<AbstractConfig>
    implements BeanDefinitionRegistryPostProcessor, PriorityOrdered, ApplicationContextAware {

    /**
     * The bean name of {@link DubboConfigBeanPostProcessor}
     */
    public static final String BEAN_NAME = "dubboConfigBeanPostProcessor";

    private ModuleModel moduleModel;
    private ApplicationModel applicationModel;

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        //DO NOTHING
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        String[] beanNames = beanFactory.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            String beanClassName = beanDefinition.getBeanClassName();
            if (beanClassName != null && beanClassName.startsWith("org.apache.dubbo.config.")) {
                try {
                    Class<?> beanClass = ClassUtils.forName(beanClassName);
                    if (AbstractConfig.class.isAssignableFrom(beanClass)) {
                        // add scopeModule constructor args to config bean, the config bean must have a constructor with scope model arg
                        ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
                        ScopeModel scopeModel = isModuleConfig(beanClass) ? moduleModel : applicationModel;
                        constructorArgumentValues.addGenericArgumentValue(scopeModel, scopeModel.getClass().getName());
                        beanDefinition.getConstructorArgumentValues().addArgumentValues(constructorArgumentValues);
                    }
                } catch (ClassNotFoundException e) {
                    // ignore class not found
                }
            }
        }
    }

    private boolean isModuleConfig(Class<?> beanClass) {
        if (AbstractMethodConfig.class.isAssignableFrom(beanClass) || ModuleConfig.class.isAssignableFrom(beanClass)) {
            return true;
        }
        return false;
    }

    protected void processBeforeInitialization(AbstractConfig dubboConfigBean, String beanName) throws BeansException {
        // ignore auto generate bean name
        if (!beanName.contains("#")) {
            // [Feature] https://github.com/apache/dubbo/issues/5721
            setPropertyIfAbsent(dubboConfigBean, Constants.ID, beanName);

            // beanName should not be used as config name, fix https://github.com/apache/dubbo/pull/7624
            //setPropertyIfAbsent(dubboConfigBean, "name", beanName);
        }
    }

    protected void setPropertyIfAbsent(Object bean, String propertyName, String beanName) {

        Class<?> beanClass = getTargetClass(bean);

        PropertyDescriptor propertyDescriptor = getPropertyDescriptor(beanClass, propertyName);

        if (propertyDescriptor != null) { // the property is present

            Method getterMethod = propertyDescriptor.getReadMethod();

            if (getterMethod == null) { // if The getter method is absent
                return;
            }

            Object propertyValue = invokeMethod(getterMethod, bean);

            if (propertyValue != null) { // If The return value of "getName" method is not null
                return;
            }

            Method setterMethod = propertyDescriptor.getWriteMethod();
            if (setterMethod != null) { // the getter and setter methods are present
                if (Arrays.equals(of(String.class), setterMethod.getParameterTypes())) { // the param type is String
                    // set bean name to the value of the the property
                    invokeMethod(setterMethod, bean, beanName);
                }
            }
        }

    }

    /**
     * @return Higher than {@link InitDestroyAnnotationBeanPostProcessor#getOrder()}
     * @see InitDestroyAnnotationBeanPostProcessor
     * @see CommonAnnotationBeanPostProcessor
     * @see PostConstruct
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE + 1;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        moduleModel = DubboBeanUtils.getModuleModel(applicationContext);
        applicationModel = DubboBeanUtils.getApplicationModel(applicationContext);
    }
}
