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
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotNode;

/**
 * State Router. (SPI, Prototype, ThreadSafe)
 * <p>
 * <a href="http://en.wikipedia.org/wiki/Routing">Routing</a>
 *
 * It is recommended to implement StateRouter by extending {@link AbstractStateRouter}
 *
 * @see org.apache.dubbo.rpc.cluster.Cluster#join(Directory, boolean)
 * @see AbstractStateRouter
 * @see Directory#list(Invocation)
 * @since 3.0
 */
public interface StateRouter<T> {

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
     * @param url        refer url
     * @param invocation invocation
     * @param needToPrintMessage whether to print router state. Such as `use router branch a`.
     * @return state with route result
     * @since 3.0
     */
    BitList<Invoker<T>> route(BitList<Invoker<T>> invokers, URL url, Invocation invocation,
                     boolean needToPrintMessage, Holder<RouterSnapshotNode<T>> nodeHolder) throws RpcException;

    /**
     * To decide whether this router need to execute every time an RPC comes or should only execute when addresses or
     * rule change.
     *
     * @return true if the router need to execute every time.
     */
    boolean isRuntime();

    /**
     * To decide whether this router should take effect when none of the invoker can match the router rule, which
     * means the {@link #route(BitList, URL, Invocation, boolean, Holder)} would be empty. Most of time, most router implementation would
     * default this value to false.
     *
     * @return true to execute if none of invokers matches the current router
     */
    boolean isForce();

    /**
     * Notify the router the invoker list. Invoker list may change from time to time. This method gives the router a
     * chance to prepare before {@link StateRouter#route(BitList, URL, Invocation, boolean, Holder)} gets called.
     * No need to notify next node.
     *
     * @param invokers invoker list
     */
    void notify(BitList<Invoker<T>> invokers);

    /**
     * Build Router's Current State Snapshot for QoS
     *
     * @return Current State
     */
    String buildSnapshot();

    default void stop() {
        //do nothing by default
    }

    /**
     * Notify next router node to current router.
     *
     * @param nextRouter next router node
     */
    void setNextRouter(StateRouter<T> nextRouter);
}
