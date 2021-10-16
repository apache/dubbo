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
package org.apache.dubbo.registry.client.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.client.AbstractServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;

import java.util.Set;

public class MockServiceDiscovery extends AbstractServiceDiscovery {
    @Override
    public void doInitialize(URL registryURL) throws Exception {

    }

    @Override
    public void doRegister(ServiceInstance serviceInstance) throws RuntimeException {

    }

    @Override
    public void doUpdate(ServiceInstance serviceInstance) throws RuntimeException {

    }

    @Override
    public void doUnregister(ServiceInstance serviceInstance) {

    }

    @Override
    public void doDestroy() throws Exception {

    }

    @Override
    public Set<String> getServices() {
        return null;
    }
}
