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
package org.apache.dubbo.config.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.registry.client.metadata.MetadataServiceDelegation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.rpc.protocol.tri.TripleProtocol;

public class DefaultRequestMappingRegistryTest {

    public static void main(String[] args) throws InterruptedException {
        IGreeter serviceImpl = new IGreeterImpl();

        ApplicationModel applicationModel = ApplicationModel.defaultModel();

        URL providerUrl = URL.valueOf("tri://127.0.0.1:" + 8081 + "/" + IGreeter.class.getName());

        ModuleServiceRepository serviceRepository =
                applicationModel.getDefaultModule().getServiceRepository();
        ServiceDescriptor serviceDescriptor = serviceRepository.registerService(IGreeter.class);

        ProviderModel providerModel = new ProviderModel(
                providerUrl.getServiceKey(),
                serviceImpl,
                serviceDescriptor,
                new ServiceMetadata(),
                ClassUtils.getClassLoader(IGreeter.class));
        serviceRepository.registerProvider(providerModel);
        providerUrl = providerUrl.setServiceModel(providerModel);

        Protocol protocol = new TripleProtocol(providerUrl.getOrDefaultFrameworkModel());
        ProxyFactory proxyFactory =
                applicationModel.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Invoker<IGreeter> invoker = proxyFactory.getInvoker(serviceImpl, IGreeter.class, providerUrl);
        protocol.export(invoker);
        MetadataServiceDelegation metadataServiceDelegation =
                applicationModel.getDefaultModule().getBeanFactory().getBean(MetadataServiceDelegation.class);

        ApplicationConfig config = new ApplicationConfig();
        config.setProtocol("tri");
        config.setMetadataServicePort(9000);
        applicationModel.getApplicationConfigManager().setApplication(config);

        ConfigurableMetadataServiceExporter metadataServiceExporter =
                new ConfigurableMetadataServiceExporter(applicationModel, metadataServiceDelegation);
        metadataServiceExporter.export();
        // 检查元数据服务代理的OpenAPI

        System.out.println(metadataServiceDelegation.getOpenAPISchema());
    }
}
