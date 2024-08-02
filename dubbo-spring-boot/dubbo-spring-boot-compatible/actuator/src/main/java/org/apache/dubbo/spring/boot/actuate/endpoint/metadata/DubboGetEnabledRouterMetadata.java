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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Dubbo Get Enabled Router Snapshot
 *
 * @since 3.3.0
 */
@Component
public class DubboGetEnabledRouterMetadata {

    @Autowired
    private ApplicationModel applicationModel;

    public Map<String, Object> getEnabledRouter() {
        Map<String, Object> routerMap = new LinkedHashMap<>();
        RouterSnapshotSwitcher routerSnapshotSwitcher =
                applicationModel.getBeanFactory().getBean(RouterSnapshotSwitcher.class);
        routerMap.put("Enabled router snapshot", routerSnapshotSwitcher.getEnabledService());
        return routerMap;
    }
}
