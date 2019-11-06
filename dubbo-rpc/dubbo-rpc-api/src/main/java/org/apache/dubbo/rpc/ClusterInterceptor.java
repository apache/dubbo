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
package org.apache.dubbo.rpc;

import org.apache.dubbo.common.extension.SPI;

/**
 * Different from {@link Filter}, ClusterInterceptor works at the outmost layer, before one specific address/invoker is picked.
 */
@SPI
public interface ClusterInterceptor {

    void before(Invoker<?> invoker, Invocation invocation);

    void after(Invoker<?> invoker, Invocation invocation);

    /**
     * Does not need to override this method, override {@link #before(Invoker, Invocation)} and {@link #after(Invoker, Invocation)}
     * methods to add your own logic expected to be executed before and after invoke.
     *
     * @param invoker
     * @param invocation
     * @return
     * @throws RpcException
     */
    default Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        return invoker.invoke(invocation);
    }

    interface Listener {

        void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation);

        void onError(Throwable t, Invoker<?> invoker, Invocation invocation);
    }
}
