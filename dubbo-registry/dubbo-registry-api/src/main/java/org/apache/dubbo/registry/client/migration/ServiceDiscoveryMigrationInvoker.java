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
package org.apache.dubbo.registry.client.migration;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.client.migration.model.MigrationRule;
import org.apache.dubbo.registry.integration.RegistryProtocol;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;

import java.util.concurrent.CountDownLatch;

public class ServiceDiscoveryMigrationInvoker<T> extends MigrationInvoker<T> {
    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscoveryMigrationInvoker.class);

    public ServiceDiscoveryMigrationInvoker(RegistryProtocol registryProtocol, Cluster cluster, Registry registry, Class<T> type, URL url, URL consumerUrl) {
        super(registryProtocol, cluster, registry, type, url, consumerUrl);
    }

    @Override
    public boolean isServiceDiscovery() {
        return true;
    }

    @Override
    public boolean migrateToForceInterfaceInvoker(MigrationRule newRule) {
        CountDownLatch latch = new CountDownLatch(0);
        refreshServiceDiscoveryInvoker(latch);

        setCurrentAvailableInvoker(getServiceDiscoveryInvoker());
        return true;
    }

    @Override
    public void migrateToApplicationFirstInvoker(MigrationRule newRule) {
        CountDownLatch latch = new CountDownLatch(0);
        refreshServiceDiscoveryInvoker(latch);

        setCurrentAvailableInvoker(getServiceDiscoveryInvoker());
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        ClusterInvoker<T> invoker = getServiceDiscoveryInvoker();
        if (invoker == null) {
            throw new IllegalStateException("There's no service discovery invoker available for service " + invocation.getServiceName());
        }
        return invoker.invoke(invocation);
    }
}
