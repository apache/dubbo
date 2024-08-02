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

import org.apache.dubbo.qos.command.util.ServiceCheckUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.ProviderModel;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Dubbo Ls
 *
 * @since 3.3.0
 */
@Component
public class DubboLsMetadata {

    @Autowired
    public ApplicationModel applicationModel;

    public Map<String, Map<String, Object>> ls() {
        Map<String, Map<String, Object>> list = new LinkedHashMap<>();
        list.put("Providers", listProvider());
        list.put("Consumers", listConsumer());
        return list;
    }

    public Map<String, Object> listProvider() {
        Map<String, Object> providersList = new LinkedHashMap<>();
        Collection<ProviderModel> providerModelList =
                applicationModel.getApplicationServiceRepository().allProviderModels();
        providerModelList = providerModelList.stream()
                .sorted(Comparator.comparing(ProviderModel::getServiceKey))
                .collect(Collectors.toList());
        for (ProviderModel providerModel : providerModelList) {
            if (providerModel.getModuleModel().isInternal()) {
                providersList.put(
                        "DubboInternal - " + providerModel.getServiceKey(),
                        ServiceCheckUtils.getRegisterStatus(providerModel));
            } else {
                providersList.put(providerModel.getServiceKey(), ServiceCheckUtils.getRegisterStatus(providerModel));
            }
        }
        return providersList;
    }

    public Map<String, Object> listConsumer() {
        Map<String, Object> consumerList = new LinkedHashMap<>();
        Collection<ConsumerModel> consumerModelList =
                applicationModel.getApplicationServiceRepository().allConsumerModels();
        consumerModelList = consumerModelList.stream()
                .sorted(Comparator.comparing(ConsumerModel::getServiceKey))
                .collect(Collectors.toList());
        for (ConsumerModel consumerModel : consumerModelList) {
            consumerList.put(consumerModel.getServiceKey(), ServiceCheckUtils.getConsumerAddressNum(consumerModel));
        }
        return consumerList;
    }
}
