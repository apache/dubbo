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
package org.apache.dubbo.config.spring.schema;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.spring.ConfigCenterBean;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.ServiceBean;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * DubboNamespaceHandler
 *
 * @export
 */
public class DubboNamespaceHandler extends NamespaceHandlerSupport {

    static {
        Version.checkDuplicate(DubboNamespaceHandler.class);
    }

    @Override
    public void init() {
        registerBeanDefinitionParser(Constants.DUBBO_NAMESPACE_APPLICATION, new DubboBeanDefinitionParser(ApplicationConfig.class, true));
        registerBeanDefinitionParser(Constants.DUBBO_NAMESPACE_MODULE, new DubboBeanDefinitionParser(ModuleConfig.class, true));
        registerBeanDefinitionParser(Constants.DUBBO_NAMESPACE_REGISTRY, new DubboBeanDefinitionParser(RegistryConfig.class, true));
        registerBeanDefinitionParser(Constants.DUBBO_NAMESPACE_CONFIG_CENTER, new DubboBeanDefinitionParser(ConfigCenterBean.class, true));
        registerBeanDefinitionParser(Constants.DUBBO_NAMESPACE_MONITOR, new DubboBeanDefinitionParser(MonitorConfig.class, true));
        registerBeanDefinitionParser(Constants.DUBBO_NAMESPACE_PROVIDER, new DubboBeanDefinitionParser(ProviderConfig.class, true));
        registerBeanDefinitionParser(Constants.DUBBO_NAMESPACE_CONSUMER, new DubboBeanDefinitionParser(ConsumerConfig.class, true));
        registerBeanDefinitionParser(Constants.DUBBO_NAMESPACE_PROTOCOL, new DubboBeanDefinitionParser(ProtocolConfig.class, true));
        registerBeanDefinitionParser(Constants.DUBBO_NAMESPACE_SERVICE, new DubboBeanDefinitionParser(ServiceBean.class, true));
        registerBeanDefinitionParser(Constants.DUBBO_NAMESPACE_REFERENCE, new DubboBeanDefinitionParser(ReferenceBean.class, false));
        registerBeanDefinitionParser(Constants.DUBBO_NAMESPACE_ANNOTATION, new AnnotationBeanDefinitionParser());
    }

}
