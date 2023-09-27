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
import org.apache.dubbo.rpc.model.ScopeModel;

import java.util.List;

/**
 * Provides an easy way to create ClusterInvoker.
 */
public interface ClusterInvokerFactory {

    Invoker<?> getInvoker(ClusterInvokerConfig config);

    class ClusterInvokerConfig{
        private ScopeModel scopeModel;
        private String clusterName;
        private URL serviceUrl;
        private List<Invoker<?>> invokersToJoin;
        boolean wrappedCluster;
        boolean wrappedDirectory;

        public ClusterInvokerConfig(ScopeModel scopeModel, String clusterName, URL serviceUrl, List<Invoker<?>> invokersToJoin) {
            this.scopeModel = scopeModel;
            this.clusterName = clusterName;
            this.serviceUrl = serviceUrl;
            this.invokersToJoin = invokersToJoin;
        }

        public ClusterInvokerConfig(ScopeModel scopeModel, String clusterName, URL serviceUrl, List<Invoker<?>> invokersToJoin, boolean wrappedCluster, boolean wrappedDirectory) {
            this.scopeModel = scopeModel;
            this.clusterName = clusterName;
            this.serviceUrl = serviceUrl;
            this.invokersToJoin = invokersToJoin;
            this.wrappedCluster = wrappedCluster;
            this.wrappedDirectory = wrappedDirectory;
        }

        public ScopeModel getScopeModel() {
            return scopeModel;
        }

        public String getClusterName() {
            return clusterName;
        }

        public URL getServiceUrl() {
            return serviceUrl;
        }

        public List<Invoker<?>> getInvokersToJoin() {
            return invokersToJoin;
        }

        public boolean isWrappedCluster() {
            return wrappedCluster;
        }

        public boolean isWrappedDirectory() {
            return wrappedDirectory;
        }
    }
}
