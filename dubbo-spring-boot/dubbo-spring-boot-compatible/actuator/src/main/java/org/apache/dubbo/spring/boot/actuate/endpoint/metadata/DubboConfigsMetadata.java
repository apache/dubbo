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
package org.apache.dubbo.spring.boot.actuate.endpoint.metadata;

import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.springframework.beans.factory.BeanFactoryUtils.beansOfTypeIncludingAncestors;

/**
 * Dubbo Configs Metadata
 *
 * @since 2.7.0
 */
@Component
public class DubboConfigsMetadata extends AbstractDubboMetadata {

    public Map<String, Map<String, Map<String, Object>>> configs() {

        Map<String, Map<String, Map<String, Object>>> configsMap = new LinkedHashMap<>();

        addDubboConfigBeans(ApplicationConfig.class, configsMap);
        addDubboConfigBeans(ConsumerConfig.class, configsMap);
        addDubboConfigBeans(MethodConfig.class, configsMap);
        addDubboConfigBeans(ModuleConfig.class, configsMap);
        addDubboConfigBeans(MonitorConfig.class, configsMap);
        addDubboConfigBeans(ProtocolConfig.class, configsMap);
        addDubboConfigBeans(ProviderConfig.class, configsMap);
        addDubboConfigBeans(ReferenceConfig.class, configsMap);
        addDubboConfigBeans(RegistryConfig.class, configsMap);
        addDubboConfigBeans(ServiceConfig.class, configsMap);

        return configsMap;

    }

    private void addDubboConfigBeans(Class<? extends AbstractConfig> dubboConfigClass,
                                     Map<String, Map<String, Map<String, Object>>> configsMap) {

        Map<String, ? extends AbstractConfig> dubboConfigBeans = beansOfTypeIncludingAncestors(applicationContext, dubboConfigClass);

        String name = dubboConfigClass.getSimpleName();

        Map<String, Map<String, Object>> beansMetadata = new TreeMap<>();

        for (Map.Entry<String, ? extends AbstractConfig> entry : dubboConfigBeans.entrySet()) {

            String beanName = entry.getKey();
            AbstractConfig configBean = entry.getValue();
            Map<String, Object> configBeanMeta = resolveBeanMetadata(configBean);
            beansMetadata.put(beanName, configBeanMeta);

        }

        configsMap.put(name, beansMetadata);

    }
}
