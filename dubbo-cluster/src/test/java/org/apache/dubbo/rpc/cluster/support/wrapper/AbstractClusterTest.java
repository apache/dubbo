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
package org.apache.dubbo.rpc.cluster.support.wrapper;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.cluster.filter.DemoService;
import org.apache.dubbo.rpc.cluster.filter.FilterChainBuilder;
import org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REFERENCE_FILTER_KEY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractClusterTest {

    @Test
    public void testBuildClusterInvokerChain() {

        Map<String, String> parameters = new HashMap<>();
        parameters.put(INTERFACE_KEY, DemoService.class.getName());
        parameters.put("registry", "zookeeper");
        parameters.put(REFERENCE_FILTER_KEY, "demo");
        ServiceConfigURL url = new ServiceConfigURL("registry",
            "127.0.0.1",
            2181,
            "org.apache.dubbo.registry.RegistryService",
            parameters);
        URL consumerUrl = new ServiceConfigURL("dubbo",
            "127.0.0.1",
            20881,
            DemoService.class.getName(),
            parameters);
        consumerUrl = consumerUrl.setScopeModel(ApplicationModel.defaultModel().getInternalModule());
        Directory<?> directory = mock(Directory.class);
        when(directory.getUrl()).thenReturn(url);
        when(directory.getConsumerUrl()).thenReturn(consumerUrl);
        DemoCluster demoCluster = new DemoCluster();
        Invoker<?> invoker = demoCluster.join(directory, true);
        Assertions.assertTrue(invoker instanceof AbstractCluster.ClusterFilterInvoker);
        Assertions.assertTrue(((AbstractCluster.ClusterFilterInvoker<?>) invoker).getFilterInvoker()
            instanceof FilterChainBuilder.ClusterCallbackRegistrationInvoker);


    }

    static class DemoCluster extends AbstractCluster {
        @Override
        public <T> Invoker<T> join(Directory<T> directory, boolean buildFilterChain) throws RpcException {
            return super.join(directory, buildFilterChain);
        }

        @Override
        protected <T> AbstractClusterInvoker<T> doJoin(Directory<T> directory) throws RpcException {
            return new DemoAbstractClusterInvoker<>(directory, directory.getUrl());
        }
    }

    static class DemoAbstractClusterInvoker<T> extends AbstractClusterInvoker<T> {

        @Override
        public URL getUrl() {
            return super.getUrl();
        }

        public DemoAbstractClusterInvoker(Directory<T> directory, URL url) {
            super(directory, url);
        }

        @Override
        protected Result doInvoke(Invocation invocation, List list, LoadBalance loadbalance) throws RpcException {
            return null;
        }
    }


}
