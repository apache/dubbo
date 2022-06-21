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
package org.apache.dubbo.qos.command.util;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.constants.RegistryConstants;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.client.migration.MigrationInvoker;
import org.apache.dubbo.registry.client.migration.model.MigrationStep;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.ProviderModel;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ServiceCheckUtils {

    public static String getRegisterStatus(ProviderModel providerModel) {
        // check all registries status
        List<String> statuses = new LinkedList<>();
        for (ProviderModel.RegisterStatedURL registerStatedURL : providerModel.getStatedUrl()) {
            URL registryUrl = registerStatedURL.getRegistryUrl();
            boolean isServiceDiscovery = UrlUtils.isServiceDiscoveryURL(registryUrl);
            String protocol = isServiceDiscovery ? registryUrl.getParameter(RegistryConstants.REGISTRY_KEY) : registryUrl.getProtocol();
            // e.g. zookeeper-A(Y)
            statuses.add(protocol + "-" + (isServiceDiscovery ? "A" : "I") + "(" + (registerStatedURL.isRegistered() ? "Y" : "N") + ")");
        }
        // e.g. zookeeper-A(Y)/zookeeper-I(Y)
        return String.join("/", statuses.toArray(new String[0]));
    }

    public static String getConsumerAddressNum(ConsumerModel consumerModel) {
        int num = 0;
        Object object = consumerModel.getServiceMetadata().getAttribute(CommonConstants.CURRENT_CLUSTER_INVOKER_KEY);
        Map<Registry, MigrationInvoker<?>> invokerMap;
        List<String> nums = new LinkedList<>();
        if (object instanceof Map) {
            invokerMap = (Map<Registry, MigrationInvoker<?>>) object;
            for (Map.Entry<Registry, MigrationInvoker<?>> entry : invokerMap.entrySet()) {
                URL registryUrl = entry.getKey().getUrl();
                boolean isServiceDiscovery = UrlUtils.isServiceDiscoveryURL(registryUrl);
                String protocol = isServiceDiscovery ? registryUrl.getParameter(RegistryConstants.REGISTRY_KEY) : registryUrl.getProtocol();
                MigrationInvoker<?> migrationInvoker = entry.getValue();
                MigrationStep migrationStep = migrationInvoker.getMigrationStep();
                String interfaceSize = Optional.ofNullable(migrationInvoker.getInvoker())
                    .map(ClusterInvoker::getDirectory)
                    .map(Directory::getAllInvokers)
                    .map(List::size)
                    .map(String::valueOf)
                    .orElse("-");
                String applicationSize = Optional.ofNullable(migrationInvoker.getServiceDiscoveryInvoker())
                    .map(ClusterInvoker::getDirectory)
                    .map(Directory::getAllInvokers)
                    .map(List::size)
                    .map(String::valueOf)
                    .orElse("-");
                String step;
                String size;
                switch (migrationStep) {
                    case APPLICATION_FIRST:
                        step = "AF";
                        size = "I-" + interfaceSize + ",A-" + applicationSize;
                        break;
                    case FORCE_INTERFACE:
                        step = "I";
                        size = interfaceSize;
                        break;
                    default:
                        step = "A";
                        size = applicationSize;
                        break;
                }
                // zookeeper-AF(I-10,A-0)
                // zookeeper-I(10)
                // zookeeper-A(10)
                nums.add(protocol + "-" + step + "(" + size + ")");
            }
        }
        // zookeeper-AF(I-10,A-0)/nacos-I(10)
        return String.join("/", nums.toArray(new String[0]));
    }
}
