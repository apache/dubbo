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


import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.directory.StaticDirectory;

import static org.apache.dubbo.rpc.cluster.Cluster.getCluster;

public class DefaultClusterInvokerFactory implements ClusterInvokerFactory {

    /**
     * Build an Invoker with StaticDirectory.
     *
     * @param config config
     * @return ClusterInvoker
     */
    @Override
    public Invoker<?> getInvoker(ClusterInvokerConfig config) {
        return getCluster(config.getScopeModel(), config.getClusterName(), config.isWrappedCluster())
                .join(new StaticDirectory(config.getServiceUrl(), config.getInvokersToJoin()), config.isWrappedDirectory());
    }

}
