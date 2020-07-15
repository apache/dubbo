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
package org.apache.dubbo.config.spring;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.spring.extension.SpringExtensionFactory;
import org.apache.dubbo.config.support.Parameter;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import static org.springframework.beans.factory.BeanFactoryUtils.beansOfTypeIncludingAncestors;

/**
 * ReferenceFactoryBean
 */
public class ReferenceBean<T> extends ReferenceConfig<T> implements FactoryBean,
        ApplicationContextAware, InitializingBean, DisposableBean {

    private static final long serialVersionUID = 213195494150089726L;

    private transient ApplicationContext applicationContext;

    public ReferenceBean() {
        super();
    }

    public ReferenceBean(Reference reference) {
        super(reference);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        SpringExtensionFactory.addApplicationContext(applicationContext);
    }

    @Override
    public Object getObject() {
        return get();
    }

    @Override
    public Class<?> getObjectType() {
        return getInterfaceClass();
    }

    @Override
    @Parameter(excluded = true)
    public boolean isSingleton() {
        return true;
    }

    /**
     * Initializes there Dubbo's Config Beans before @Reference bean autowiring
     */
    private void prepareDubboConfigBeans() {
        beansOfTypeIncludingAncestors(applicationContext, ApplicationConfig.class);
        beansOfTypeIncludingAncestors(applicationContext, ModuleConfig.class);
        beansOfTypeIncludingAncestors(applicationContext, RegistryConfig.class);
        beansOfTypeIncludingAncestors(applicationContext, ProtocolConfig.class);
        beansOfTypeIncludingAncestors(applicationContext, MonitorConfig.class);
        beansOfTypeIncludingAncestors(applicationContext, ProviderConfig.class);
        beansOfTypeIncludingAncestors(applicationContext, ConsumerConfig.class);
        beansOfTypeIncludingAncestors(applicationContext, ConfigCenterBean.class);
        beansOfTypeIncludingAncestors(applicationContext, MetadataReportConfig.class);
        beansOfTypeIncludingAncestors(applicationContext, MetricsConfig.class);
        beansOfTypeIncludingAncestors(applicationContext, SslConfig.class);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void afterPropertiesSet() throws Exception {

        // Initializes Dubbo's Config Beans before @Reference bean autowiring
        prepareDubboConfigBeans();

        // lazy init by default.
        if (init == null) {
            init = false;
        }

        // eager init if necessary.
        if (shouldInit()) {
            getObject();
        }
    }

    @Override
    public void destroy() {
        // do nothing
    }
}
