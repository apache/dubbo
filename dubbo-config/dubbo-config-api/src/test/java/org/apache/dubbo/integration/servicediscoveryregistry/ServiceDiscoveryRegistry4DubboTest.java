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
package org.apache.dubbo.integration.servicediscoveryregistry;

/**
 * Checks if there exists problems between
 * {@link org.apache.dubbo.registry.integration.RegistryProtocol} and
 * {@link org.apache.dubbo.registry.client.ServiceDiscoveryRegistry}
 * using dubbo protocol.
 */
public class ServiceDiscoveryRegistry4DubboTest extends AbstractServiceDiscoveryRegistryTest{
    /**
     * Returns the protocol's name.
     */
    @Override
    protected String getProtocolName() {
        return "dubbo";
    }

    /**
     * Returns the application name.
     */
    @Override
    protected String getApplicationName() {
        return "integration-service-discovery-registry-dubbo";
    }

    /**
     * Returns the protocol's port.
     */
    @Override
    protected int getProtocolPort() {
        return 20880;
    }
}
