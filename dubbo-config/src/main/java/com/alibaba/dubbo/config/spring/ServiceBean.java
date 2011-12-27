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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.MonitorConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.ProviderConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;

/**
 * ServiceFactoryBean
 * 
 * @author william.liangf
 */
public class ServiceBean<T> extends ServiceConfig<T> implements InitializingBean, ApplicationContextAware, ApplicationListener, BeanNameAware {

	private static final long serialVersionUID = 213195494150089726L;

    private static transient ApplicationContext SPRING_CONTEXT;
    
	private transient ApplicationContext applicationContext;

    private transient String beanName;

    private transient boolean supportedApplicationListener;
    
	public static ApplicationContext getSpringContext() {
	    return SPRING_CONTEXT;
	}

	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		if (applicationContext != null) {
		    SPRING_CONTEXT = applicationContext;
		    try {
	            Method method = applicationContext.getClass().getMethod("addApplicationListener", new Class<?>[]{ApplicationListener.class}); // 兼容Spring2.0.1
	            method.invoke(applicationContext, new Object[] {this});
	            supportedApplicationListener = true;
	        } catch (Throwable t) {
	        }
		}
	}

    public void setBeanName(String name) {
        this.beanName = name;
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if ("org.springframework.context.event.ContextStartedEvent".equals(event.getClass().getName())) { // 兼容Spring2.0.1
            if (isDelay()) {
                if (logger.isInfoEnabled()) {
                    logger.info("The service ready on spring started. service: " + getInterface());
                }
                export();
            }
        }
    }
    
    private boolean isDelay() {
        Integer delay = getDelay();
        ProviderConfig provider = getProvider();
        if (delay == null && provider != null) {
            delay = provider.getDelay();
        }
        return supportedApplicationListener && delay != null && delay.intValue() == -1;
    }

    @SuppressWarnings({ "unchecked" })
	public void afterPropertiesSet() throws Exception {
        if (getProvider() == null) {
            Map<String, ProviderConfig> providerConfigMap = applicationContext == null ? null  : applicationContext.getBeansOfType(ProviderConfig.class, false, false);
            if (providerConfigMap != null && providerConfigMap.size() > 0) {
                Collection<ProviderConfig> providerConfigs = providerConfigMap.values();
                ProviderConfig providerConfig = providerConfigs.iterator().next();
                if (providerConfigs.size() > 1) {
                    Map<String, ProtocolConfig> protocolConfigMap = applicationContext == null ? null  : applicationContext.getBeansOfType(ProtocolConfig.class, false, false);
                    if (protocolConfigMap != null && protocolConfigMap.size() > 0) {
                        throw new IllegalStateException("Duplicate provider configs: " + providerConfigs);
                    }
                    for (ProviderConfig config : providerConfigs) {
                        if (config.isDefault() != null && config.isDefault()) {
                            providerConfig = config;
                        }
                    }
                }
                setProvider(providerConfig);
            }
        }
        if (getApplication() == null
                && (getProvider() == null || getProvider().getApplication() == null)) {
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
                && (getProvider() == null || getProvider().getRegistries() == null || getProvider().getRegistries().size() == 0)
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
                && (getProvider() == null || getProvider().getMonitor() == null)
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
        if ((getProtocols() == null || getProtocols().size() == 0)
                && (getProvider() == null || getProvider().getProtocols() == null || getProvider().getProtocols().size() == 0)) {
            Map<String, ProtocolConfig> protocolConfigMap = applicationContext == null ? null  : applicationContext.getBeansOfType(ProtocolConfig.class, false, false);
            if (protocolConfigMap != null && protocolConfigMap.size() > 0) {
                if (protocolConfigMap.size() > 1) {
                    throw new IllegalStateException("Found multi-protocols: " + protocolConfigMap.values() + ", You must be set default protocol in: <dubbo:provider protocol=\"dubbo\" />, or set service protocol in: <dubbo:service protocol=\"dubbo\" />");
                }
                ProtocolConfig protocolConfig = protocolConfigMap.values().iterator().next();
                setProtocol(protocolConfig);
            }
        }
        if (getPath() == null || getPath().length() == 0) {
            if (beanName != null && beanName.length() > 0 
                    && getInterface() != null && getInterface().length() > 0
                    && beanName.startsWith(getInterface())) {
                setPath(beanName);
            }
        }
        if (! isDelay()) {
            export();
        }
    }

}