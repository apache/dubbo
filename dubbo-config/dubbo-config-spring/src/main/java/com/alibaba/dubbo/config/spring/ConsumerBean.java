/*
 * Copyright 1999-2012 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.config.spring;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ConsumerConfig;
import com.alibaba.dubbo.config.ModuleConfig;
import com.alibaba.dubbo.config.MonitorConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.annotation.Reference;

/**
 * ConsumerBean
 * 
 * @author william.liangf
 */
public class ConsumerBean extends ConsumerConfig implements DisposableBean, BeanPostProcessor, ApplicationContextAware {

    private static final long serialVersionUID = 1036505745144610573L;

    private static final Logger           logger           = LoggerFactory.getLogger(Logger.class);

    private String                        annotationPackage;

    private String[]                      annotationPackages;

    private final ConcurrentMap<String, ReferenceBean<?>> referenceConfigs = new ConcurrentHashMap<String, ReferenceBean<?>>();

    public String getPackage() {
        return annotationPackage;
    }

    public void setPackage(String annotationPackage) {
        this.annotationPackage = annotationPackage;
        this.annotationPackages = (annotationPackage == null || annotationPackage.length() == 0) ? null
                : Constants.COMMA_SPLIT_PATTERN.split(annotationPackage);
    }

    private ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void destroy() throws Exception {
        for (ReferenceConfig<?> referenceConfig : referenceConfigs.values()) {
            try {
                referenceConfig.destroy();
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
    
    private Object refer(Reference reference, Class<?> referenceClass) { //method.getParameterTypes()[0]
        if (reference != null) {
            String interfaceName;
            if (! "".equals(reference.interfaceName())) {
                interfaceName = reference.interfaceName();
            } else if (! void.class.equals(reference.interfaceClass())) {
                interfaceName = reference.interfaceClass().getName();
            } else if (referenceClass.isInterface()) {
                interfaceName = referenceClass.getName();
            } else {
                throw new IllegalStateException("The @Reference undefined interfaceClass or interfaceName, and the property type " + referenceClass.getName() + " is not a interface.");
            }
            String key = reference.group() + "/" + interfaceName + ":" + reference.version();
            ReferenceBean<?> referenceConfig = referenceConfigs.get(key);
            if (referenceConfig == null) {
                referenceConfig = new ReferenceBean<Object>(reference);
                if (void.class.equals(reference.interfaceClass())
                        && "".equals(reference.interfaceName())
                        && referenceClass.isInterface()) {
                    referenceConfig.setInterface(referenceClass);
                }
                referenceConfig.setConsumer(this);
                if (applicationContext != null) {
                    referenceConfig.setApplicationContext(applicationContext);
                    if (reference.registry() != null && reference.registry().length > 0) {
                        List<RegistryConfig> registryConfigs = new ArrayList<RegistryConfig>();
                        for (String registryId : reference.registry()) {
                            if (registryId != null && registryId.length() > 0) {
                                registryConfigs.add((RegistryConfig)applicationContext.getBean(registryId, RegistryConfig.class));
                            }
                        }
                        referenceConfig.setRegistries(registryConfigs);
                    }
                    if (reference.monitor() != null && reference.monitor().length() > 0) {
                        referenceConfig.setMonitor((MonitorConfig)applicationContext.getBean(reference.monitor(), MonitorConfig.class));
                    }
                    if (reference.application() != null && reference.application().length() > 0) {
                        referenceConfig.setApplication((ApplicationConfig)applicationContext.getBean(reference.application(), ApplicationConfig.class));
                    }
                    if (reference.module() != null && reference.module().length() > 0) {
                        referenceConfig.setModule((ModuleConfig)applicationContext.getBean(reference.module(), ModuleConfig.class));
                    }
                    if (reference.consumer() != null && reference.consumer().length() > 0) {
                        referenceConfig.setConsumer((ConsumerConfig)applicationContext.getBean(reference.consumer(), ConsumerConfig.class));
                    }
                    try {
                        referenceConfig.afterPropertiesSet();
                    } catch (RuntimeException e) {
                        throw (RuntimeException) e;
                    } catch (Exception e) {
                        throw new IllegalStateException(e.getMessage(), e);
                    }
                }
                referenceConfigs.putIfAbsent(key, referenceConfig);
                referenceConfig = referenceConfigs.get(key);
            }
            return referenceConfig.get();
        }
        return null;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName)
            throws BeansException {
        if (annotationPackages == null || annotationPackages.length == 0) {
            return bean;
        }
        String beanClassName = bean.getClass().getName();
        boolean match = false;
        for (String pkg : annotationPackages) {
            if (beanClassName.startsWith(pkg)) {
                match = true;
                break;
            }
        }
        if (! match) {
            return bean;
        }
        Method[] methods = bean.getClass().getMethods();
        for (Method method : methods) {
            String name = method.getName();
            if (name.length() > 3 && name.startsWith("set")
                    && method.getParameterTypes().length == 1
                    && Modifier.isPublic(method.getModifiers())
                    && ! Modifier.isStatic(method.getModifiers())) {
                try {
                    method.invoke(bean, new Object[] { refer(method.getAnnotation(Reference.class), method.getParameterTypes()[0]) });
                } catch (Throwable e) {
                    throw new IllegalStateException("Failed to init remote service reference at method " + name + " in class " + beanClassName + ", cause: " + e.getMessage(), e);
                }
            }
        }
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                if (! field.isAccessible()) {
                    field.setAccessible(true);
                }
                field.set(bean, refer(field.getAnnotation(Reference.class), field.getType()));
            } catch (Throwable e) {
                throw new IllegalStateException("Failed to init remote service reference at filed " + field.getName() + " in class " + beanClassName + ", cause: " + e.getMessage(), e);
            }
        }
        return bean;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName)
            throws BeansException {
        return bean;
    }
}
