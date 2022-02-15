/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.config.mock;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.client.AbstractServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MockServiceDiscovery extends AbstractServiceDiscovery {
    private URL registryURL;

    public MockServiceDiscovery(ApplicationModel applicationModel, URL registryURL) {
        super(applicationModel, registryURL);
    }

    public MockServiceDiscovery(String serviceName, URL registryURL) {
        super(serviceName, registryURL);
    }

    @Override
    public void doDestroy() throws Exception {

    }

    @Override
    public void doRegister(ServiceInstance serviceInstance) throws RuntimeException {
        this.serviceInstance = serviceInstance;
    }

    @Override
    public void doUpdate(ServiceInstance serviceInstance) throws RuntimeException {
        this.serviceInstance = serviceInstance;
    }

    @Override
    public void doUnregister(ServiceInstance serviceInstance) throws RuntimeException {
        this.serviceInstance = null;
    }

    @Override
    public Set<String> getServices() {
        return new HashSet<>();
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) throws NullPointerException {
        return Collections.emptyList();
    }

    @Override
    public URL getUrl() {
        return registryURL;
    }
}
