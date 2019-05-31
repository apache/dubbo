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
package org.apache.dubbo.bootstrap;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.metadata.MetadataServiceExporter;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;

import java.util.LinkedList;
import java.util.List;

import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;

/**
 * The bootstrap class of Dubbo
 *
 * @since 2.7.3
 */
public class DubboBootstrap {

    private final List<ServiceConfig<?>> serviceConfigs = new LinkedList<>();

    private final List<ReferenceConfig<?>> referenceConfigs = new LinkedList<>();

    private final MetadataServiceExporter metadataServiceExporter =
            getExtensionLoader(MetadataServiceExporter.class).getExtension("default");

    private ServiceInstance serviceInstance;

    private ApplicationConfig applicationConfig;

    private String serviceDiscoveryType;

    private ServiceDiscovery serviceDiscovery;


    public DubboBootstrap applicationConfig(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
        return this;
    }

    public DubboBootstrap serviceDiscoveryType(String serviceDiscoveryType) {
        this.serviceDiscoveryType = serviceDiscoveryType;
        return this;
    }

    public void start() {


        registerSelfInstance();


        metadataServiceExporter.export();
    }

    private void registerSelfInstance() {
//        serviceInstance = new
    }


    public void stop() {

    }
}
