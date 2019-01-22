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
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;

import java.util.List;

/**
 * Router. (SPI, Prototype, ThreadSafe)
 * <p>
 * <a href="http://en.wikipedia.org/wiki/Routing">Routing</a>
 *
 * @see org.apache.dubbo.rpc.cluster.Cluster#join(Directory)
 * @see org.apache.dubbo.rpc.cluster.Directory#list(Invocation)
 */
public interface Router extends Comparable<Router> {
    /**
     * Get the router url.
     *
     * @return url
     */
    URL getUrl();

    /**
     * Filter invokers with current routing rule and only return the invokers that comply with the rule.
     *
     * @param invokers   invoker list
     * @param url        refer url
     * @param invocation invocation
     * @return routed invokers
     * @throws RpcException
     */
    <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException;


    /**
     * Notify the router the invoker list. Invoker list may change from time to time. This method gives the router a
     * chance to prepare before {@link Router#route(List, URL, Invocation)} gets called.
     *
     * @param invokers invoker list
     * @param <T>      invoker's type
     */
    default <T> void notify(List<Invoker<T>> invokers) {

    }

    /**
     * To decide whether this router need to execute every time an RPC comes or should only execute when addresses or
     * rule change.
     *
     * @return true if the router need to execute every time.
     */
    boolean isRuntime();

    /**
     * To decide whether this router should take effect when none of the invoker can match the router rule, which
     * means the {@link #route(List, URL, Invocation)} would be empty. Most of time, most router implementation would
     * default this value to false.
     *
     * @return true to execute if none of invokers matches the current router
     */
    boolean isForce();

    /**
     * Router's priority, used to sort routers.
     *
     * @return router's priority
     */
    int getPriority();

    @Override
    default int compareTo(Router o) {
        if (o == null) {
            throw new IllegalArgumentException();
        }
        if (this.getPriority() == o.getPriority()) {
            if (o.getUrl() == null) {
                return 1;
            }
            if (getUrl() == null) {
                return -1;
            }
            return getUrl().toFullString().compareTo(o.getUrl().toFullString());
        } else {
            return getPriority() > o.getPriority() ? 1 : -1;
        }
    }
}
