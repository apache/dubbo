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
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotNode;

public class TailStateRouter<T> implements StateRouter<T> {
    private static final TailStateRouter INSTANCE = new TailStateRouter();

    @SuppressWarnings("unchecked")
    public static <T> TailStateRouter<T> getInstance() {
        return INSTANCE;
    }

    private TailStateRouter() {

    }

    @Override
    public void setNextRouter(StateRouter<T> nextRouter) {

    }

    @Override
    public URL getUrl() {
        return null;
    }

    @Override
    public BitList<Invoker<T>> route(BitList<Invoker<T>> invokers, URL url, Invocation invocation, boolean needToPrintMessage, Holder<RouterSnapshotNode<T>> nodeHolder) throws RpcException {
        return invokers;
    }

    @Override
    public boolean isRuntime() {
        return false;
    }

    @Override
    public boolean isForce() {
        return false;
    }

    @Override
    public void notify(BitList<Invoker<T>> invokers) {

    }

    @Override
    public String buildSnapshot() {
        return "TailStateRouter End";
    }
}
