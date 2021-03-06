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
package org.apache.dubbo.rpc.cluster.support.migration;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;

import java.util.concurrent.atomic.AtomicBoolean;

public interface MigrationClusterInvoker<T> extends ClusterInvoker<T> {

    boolean isServiceInvoker();

    MigrationRule getMigrationRule();

    void setMigrationRule(MigrationRule rule);

    void destroyServiceDiscoveryInvoker(ClusterInvoker<?> invoker);

    void discardServiceDiscoveryInvokerAddress(ClusterInvoker<?> invoker);

    void discardInterfaceInvokerAddress(ClusterInvoker<T> invoker);

    void refreshServiceDiscoveryInvoker();

    void refreshInterfaceInvoker();

    void destroyInterfaceInvoker(ClusterInvoker<T> invoker);

    boolean isMigrationMultiRegsitry();

    void migrateToServiceDiscoveryInvoker(boolean forceMigrate);

    void reRefer(URL newSubscribeUrl);

    void fallbackToInterfaceInvoker();

    AtomicBoolean invokersChanged();

}