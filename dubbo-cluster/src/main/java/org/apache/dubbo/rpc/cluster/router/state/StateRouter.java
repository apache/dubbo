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
package org.apache.dubbo.rpc.cluster.router.state;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Directory;

import java.util.List;

/**
 * State Router. (SPI, Prototype, ThreadSafe)
 * <p>
 * <a href="http://en.wikipedia.org/wiki/Routing">Routing</a>
 *
 * @see org.apache.dubbo.rpc.cluster.Cluster#join(Directory)
 * @see Directory#list(Invocation)
 * @since 3.0
 */
public interface StateRouter extends Comparable<StateRouter> {

    int DEFAULT_PRIORITY = Integer.MAX_VALUE;

    /**
     * Get the router url.
     *
     * @return url
     */
    URL getUrl();

    /***
     * Filter invokers with current routing rule and only return the invokers that comply with the rule.
     * Caching address lists in BitMap mode improves routing performance.
     * @param invokers  invoker bit list
     * @param cache      router address cache
     * @param url        refer url
     * @param invocation invocation
     * @param <T>
     * @return routed invokers
     * @throws RpcException
     * @Since 3.0
     */
    @Deprecated
    default <T> BitList<Invoker<T>> route(BitList<Invoker<T>> invokers, RouterCache<T> cache, URL url, Invocation invocation)
        throws RpcException {
        return null;
    }

    /***
     * ** This method can return the state of whether routerChain needed to continue route. **
     * Filter invokers with current routing rule and only return the invokers that comply with the rule.
     * Caching address lists in BitMap mode improves routing performance.
     * @param invokers  invoker bit list
     * @param cache      router address cache
     * @param url        refer url
     * @param invocation invocation
     * @param needToPrintMessage whether to print router state. Such as `use router branch a`.
     * @return state with route result
     * @since 3.0
     */
    default <T> StateRouterResult<Invoker<T>> route(BitList<Invoker<T>> invokers, RouterCache<T> cache, URL url, Invocation invocation,
                                                          boolean needToPrintMessage) throws RpcException {
        return new StateRouterResult<>(route(invokers, cache, url, invocation));
    }

    default <T> void notify(List<Invoker<T>> invokers) {

    }

    /**
     * To decide whether this router need to execute every time an RPC comes or should only execute when addresses or
     * rule change.
     *
     * @return true if the router need to execute every time.
     */
    boolean isRuntime();

    boolean isEnable();

    boolean isForce();

    int getPriority();

    @Override
    default int compareTo(StateRouter o) {
        if (o == null) {
            throw new IllegalArgumentException();
        }
        return Integer.compare(this.getPriority(), o.getPriority());
    }

    String getName();

    boolean shouldRePool();

    <T> RouterCache<T> pool(List<Invoker<T>> invokers);

    void pool();

    default void stop() {
        //do nothing by default
    }
}
