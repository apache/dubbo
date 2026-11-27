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
package org.apache.dubbo.xds.traffic;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.Directory;

import java.util.Collections;

/** Native registry references cannot silently retain the default Failover/random combination. */
public final class XdsClusterWrapper implements Cluster {
    private final Cluster delegate;
    private final XdsCluster nativeCluster = new XdsCluster();

    public XdsClusterWrapper(Cluster delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> Invoker<T> join(Directory<T> directory, boolean buildFilterChain) {
        URL registry = directory.getUrl();
        URL reference = directory.getConsumerUrl();
        boolean nativeRegistry =
                "xds".equals(registry.getProtocol()) || "xds".equals(registry.getParameter("registry"));
        String protocol = reference.getParameter("protocol", reference.getProtocol());
        if (!nativeRegistry || "dubbo".equals(protocol)) {
            return delegate.join(directory, buildFilterChain);
        }
        if (directory.getRouterChain() == null) {
            throw new IllegalStateException("Native xDS requires a RouterChain");
        }
        synchronized (directory.getRouterChain()) {
            if (directory.getRouterChain().getRouters().stream().noneMatch(XdsRouter.class::isInstance)) {
                directory.getRouterChain().addRouters(Collections.singletonList(new XdsRouter(reference)));
            }
        }
        return (delegate instanceof XdsCluster ? delegate : nativeCluster).join(directory, buildFilterChain);
    }
}
