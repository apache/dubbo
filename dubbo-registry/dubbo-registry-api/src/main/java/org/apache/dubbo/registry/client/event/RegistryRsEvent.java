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
package org.apache.dubbo.registry.client.event;

import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.List;

public class RegistryRsEvent extends RegistryEvent {

    private final List<String> registryClusterNames;

    private final String serviceKey;

    private final int size;

    public RegistryRsEvent(
            ApplicationModel applicationModel, String serviceKey, int size, List<String> serviceDiscoveryNames) {
        super(applicationModel);
        this.registryClusterNames = serviceDiscoveryNames;
        this.serviceKey = serviceKey;
        this.size = size;
    }

    public List<String> getRegistryClusterNames() {
        return registryClusterNames;
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public int getSize() {
        return size;
    }
}
