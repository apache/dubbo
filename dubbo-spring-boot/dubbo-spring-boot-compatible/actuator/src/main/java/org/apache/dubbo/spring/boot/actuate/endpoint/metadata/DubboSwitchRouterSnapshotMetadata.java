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

import org.apache.dubbo.rpc.cluster.router.RouterSnapshotSwitcher;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.ServiceMetadata;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Dubbo Switch Router Snapshot
 *
 * @since 3.3.0
 */
@Component
public class DubboSwitchRouterSnapshotMetadata {

    @Autowired
    public ApplicationModel applicationModel;

    public Map<String, Object> disableRouterSnapshot(String servicePattern) {
        return updateRouterSnapshot(servicePattern, false);
    }

    public Map<String, Object> enableRouterSnapshot(String servicePattern) {
        return updateRouterSnapshot(servicePattern, true);
    }

    public Map<String, Object> updateRouterSnapshot(String servicePattern, boolean enable) {
        Map<String, Object> updateInfo = new LinkedHashMap<>();
        RouterSnapshotSwitcher routerSnapshotSwitcher =
                applicationModel.getBeanFactory().getBean(RouterSnapshotSwitcher.class);
        for (ConsumerModel consumerModel :
                applicationModel.getApplicationServiceRepository().allConsumerModels()) {
            try {
                ServiceMetadata metadata = consumerModel.getServiceMetadata();
                if (metadata.getServiceKey().matches(servicePattern)
                        || metadata.getDisplayServiceKey().matches(servicePattern)) {
                    if (enable) {
                        routerSnapshotSwitcher.addEnabledService(metadata.getServiceKey());
                        updateInfo.put(
                                "WARN",
                                "Enable router snapshot will cause performance degradation, please be careful!");
                        updateInfo.put(metadata.getServiceKey(), "Router snapshot enabled");
                    } else {
                        routerSnapshotSwitcher.removeEnabledService(metadata.getServiceKey());
                        updateInfo.put(metadata.getServiceKey(), "Router snapshot disabled");
                    }
                }
            } catch (Throwable ignore) {

            }
        }
        return updateInfo;
    }
}
