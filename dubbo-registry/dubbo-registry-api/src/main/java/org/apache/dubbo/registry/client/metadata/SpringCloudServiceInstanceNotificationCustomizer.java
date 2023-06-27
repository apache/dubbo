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
package org.apache.dubbo.registry.client.metadata;

import org.apache.dubbo.common.ProtocolServiceKey;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.client.ServiceInstance;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SpringCloudServiceInstanceNotificationCustomizer implements ServiceInstanceNotificationCustomizer {
    @Override
    public void customize(List<ServiceInstance> serviceInstance) {
        if (serviceInstance.isEmpty()) {
            return;
        }

        if (!serviceInstance.stream().allMatch(instance -> "SPRING_CLOUD".equals(instance.getMetadata("preserved.register.source")))) {
            return;
        }

        for (ServiceInstance instance : serviceInstance) {
            MetadataInfo.ServiceInfo serviceInfo = new MetadataInfo.ServiceInfo("*", "*", "*", "rest", instance.getPort(), "*", new HashMap<>());
            String revision = "SPRING_CLOUD-" + instance.getServiceName() + "-" + instance.getAddress() + "-" + instance.getPort();
            MetadataInfo metadataInfo = new MetadataInfo(instance.getServiceName(), revision, new ConcurrentHashMap<>(Collections.singletonMap("*", serviceInfo))) {
                @Override
                public List<ServiceInfo> getMatchedServiceInfos(ProtocolServiceKey consumerProtocolServiceKey) {
                    getServices().putIfAbsent(consumerProtocolServiceKey.getServiceKeyString(),
                        new MetadataInfo.ServiceInfo(consumerProtocolServiceKey.getInterfaceName(),
                            consumerProtocolServiceKey.getGroup(), consumerProtocolServiceKey.getVersion(),
                            consumerProtocolServiceKey.getProtocol(), instance.getPort(), consumerProtocolServiceKey.getInterfaceName(), new HashMap<>()));
                    return super.getMatchedServiceInfos(consumerProtocolServiceKey);
                }
            };


            instance.setServiceMetadata(metadataInfo);
        }
    }
}
