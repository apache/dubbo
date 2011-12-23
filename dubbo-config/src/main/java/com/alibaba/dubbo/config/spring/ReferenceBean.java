/*
 * Copyright 1999-2011 Alibaba Group.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ConsumerConfig;
import com.alibaba.dubbo.config.MonitorConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;

/**
 * ReferenceFactoryBean
 * 
 * @author william.liangf
 */
public class ReferenceBean<T> extends ReferenceConfig<T> implements FactoryBean, ApplicationContextAware, InitializingBean {

	private static final long serialVersionUID = 213195494150089726L;
	
	private transient ApplicationContext applicationContext;

	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}
    
    public Object getObject() throws Exception {
        return get();
    }

    public Class<?> getObjectType() {
        return getInterfaceClass();
    }

    public boolean isSingleton() {
        return true;
    }

    @SuppressWarnings({ "unchecked"})
    public void afterPropertiesSet() throws Exception {
        if (getConsumer() == null) {
            Map<String, ConsumerConfig> consumerConfigMap = applicationContext == null ? null  : applicationContext.getBeansOfType(ConsumerConfig.class, false, false);
            if (consumerConfigMap != null && consumerConfigMap.size() > 0) {
                ConsumerConfig consumerConfig = null;
                Collection<ConsumerConfig> defaultConfigs = consumerConfigMap.values();
                for (ConsumerConfig config : defaultConfigs) {
                    if (config.getClass() == ConsumerConfig.class) {
                        if (consumerConfig != null) {
                            throw new IllegalStateException("Duplicate consumer configs: " + consumerConfig + " and " + config);
                        }
                        consumerConfig = config;
                    }
                }
                if (consumerConfig != null) {
                    setConsumer(consumerConfig);
                }
            }
        }
        if (getApplication() == null
                && (getConsumer() == null || getConsumer().getApplication() == null)) {
            Map<String, ApplicationConfig> applicationConfigMap = applicationContext == null ? null : applicationContext.getBeansOfType(ApplicationConfig.class, false, false);
            if (applicationConfigMap != null && applicationConfigMap.size() > 0) {
                if (applicationConfigMap.size() > 1) {
                    throw new IllegalStateException("Duplicate application configs: " + applicationConfigMap.values());
                }
                ApplicationConfig applicationConfig = applicationConfigMap.values().iterator().next();
                setApplication(applicationConfig);
            }
        }
        if ((getRegistries() == null || getRegistries().size() == 0)
                && (getConsumer() == null || getConsumer().getRegistries() == null || getConsumer().getRegistries().size() == 0)
                && (getApplication() == null || getApplication().getRegistries() == null || getApplication().getRegistries().size() == 0)) {
            Map<String, RegistryConfig> registryConfigMap = applicationContext == null ? null : applicationContext.getBeansOfType(RegistryConfig.class, false, false);
            if (registryConfigMap != null && registryConfigMap.size() > 0) {
                Collection<RegistryConfig> registryConfigs = registryConfigMap.values();
                if (registryConfigs != null && registryConfigs.size() > 0) {
                    super.setRegistries(new ArrayList<RegistryConfig>(registryConfigs));
                }
            }
        }
        if (getMonitor() == null
                && (getConsumer() == null || getConsumer().getMonitor() == null)
                && (getApplication() == null || getApplication().getMonitor() == null)) {
            Map<String, MonitorConfig> monitorConfigMap = applicationContext == null ? null : applicationContext.getBeansOfType(MonitorConfig.class, false, false);
            if (monitorConfigMap != null && monitorConfigMap.size() > 0) {
                if (monitorConfigMap.size() > 1) {
                    throw new IllegalStateException("Duplicate monitor configs: " + monitorConfigMap.values());
                }
                MonitorConfig monitorConfig = monitorConfigMap.values().iterator().next();
                super.setMonitor(monitorConfig);
            }
        }
        Boolean b = isInit();
        if (b == null && getConsumer() != null) {
            b = getConsumer().isInit();
        }
        if (b != null && b.booleanValue()) {
            getObject();
        }
    }

}