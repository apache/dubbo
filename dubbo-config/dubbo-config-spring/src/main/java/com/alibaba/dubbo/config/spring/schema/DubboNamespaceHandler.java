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
package com.alibaba.dubbo.config.spring.schema;

import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ConsumerConfig;
import com.alibaba.dubbo.config.ModuleConfig;
import com.alibaba.dubbo.config.MonitorConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.ProviderConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.spring.AnnotationBean;
import com.alibaba.dubbo.config.spring.ReferenceBean;
import com.alibaba.dubbo.config.spring.ServiceBean;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * DubboNamespaceHandler
 *
 * @author william.liangf
 * @export
 */
public class DubboNamespaceHandler extends NamespaceHandlerSupport {

    static {
        Version.checkDuplicate(DubboNamespaceHandler.class);
    }

    public void init() {
        registerBeanDefinitionParser("application", new ApplicationBeanDefinitionParser(ApplicationConfig.class));
        registerBeanDefinitionParser("module", new ModuleBeanDefinitionParser(ModuleConfig.class));
        registerBeanDefinitionParser("registry", new RegistryBeanDefinitionParser(RegistryConfig.class));
        registerBeanDefinitionParser("monitor", new MonitorBeanDefinitionParser(MonitorConfig.class));
        registerBeanDefinitionParser("provider", new ProviderBeanDefinitionParser(ProviderConfig.class));
        registerBeanDefinitionParser("consumer", new ConsumerBeanDefinitionParser(ConsumerConfig.class));
        registerBeanDefinitionParser("protocol", new ProtocolBeanDefinitionParser(ProtocolConfig.class));
        registerBeanDefinitionParser("service", new ServiceBeanDefinitionParser(ServiceBean.class));
        registerBeanDefinitionParser("reference", new ReferenceBeanDefinitionParser(ReferenceBean.class, false));
        registerBeanDefinitionParser("annotation", new AnnotationBeanDefinitionParser(AnnotationBean.class));
    }

}