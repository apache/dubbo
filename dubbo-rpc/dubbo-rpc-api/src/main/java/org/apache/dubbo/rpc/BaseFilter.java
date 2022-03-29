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

public interface BaseFilter {
    /**
     * Always call invoker.invoke() in the implementation to hand over the request to the next filter node.
     */
    Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException;

    /**
     * This callback listener applies to both synchronous and asynchronous calls, please put logics that need to be executed
     * on return of rpc result in onResponse or onError respectively based on it is normal return or exception return.
     * <p>
     * There's something that needs to pay attention on legacy synchronous style filer refactor, the thing is, try to move logics
     * previously defined in the 'finally block' to both onResponse and onError.
     */
    interface Listener {

        /**
         * This method will only be called on successful remote rpc execution, that means, the service in on remote received
         * the request and the result (normal or exceptional) returned successfully.
         *
         * @param appResponse, the rpc call result, it can represent both normal result and exceptional result
         * @param invoker,     context
         * @param invocation,  context
         */
        void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation);

        /**
         * This method will be called on detection of framework exceptions, for example, TimeoutException, NetworkException
         * Exception raised in Filters, etc.
         *
         * @param t,          framework exception
         * @param invoker,    context
         * @param invocation, context
         */
        void onError(Throwable t, Invoker<?> invoker, Invocation invocation);
    }
}
