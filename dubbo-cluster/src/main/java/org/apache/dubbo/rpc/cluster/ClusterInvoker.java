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
package org.apache.dubbo.rpc.cluster;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;

/**
 * This is the final Invoker type referenced by the RPC proxy on Consumer side.
 * <p>
 * A ClusterInvoker holds a group of normal invokers, stored in a Directory, mapping to one Registry.
 * The ClusterInvoker implementation usually provides LB or HA policies, like FailoverClusterInvoker.
 * <p>
 * In multi-registry subscription scenario, the final ClusterInvoker will referr to several sub ClusterInvokers, with each
 * sub ClusterInvoker representing one Registry. Take ZoneAwareClusterInvoker as an example, it is specially customized for
 * multi-registry use cases: first, pick up one ClusterInvoker, then do LB inside the chose ClusterInvoker.
 *
 * @param <T>
 */
public interface ClusterInvoker<T> extends Invoker<T> {

    URL getRegistryUrl();

    Directory<T> getDirectory();

    boolean isDestroyed();

    default boolean isServiceDiscovery() {
        Directory<T> directory = getDirectory();
        if (directory == null) {
            return false;
        }
        return directory.isServiceDiscovery();
    }

    default boolean hasProxyInvokers() {
        Directory<T> directory = getDirectory();
        if (directory == null) {
            return false;
        }
        return !directory.isEmpty();
    }
}
