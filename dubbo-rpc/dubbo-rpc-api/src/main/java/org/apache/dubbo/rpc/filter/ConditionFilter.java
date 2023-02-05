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

package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.rpc.BaseFilter;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.filter.condition.FilterConditionMatcher;

public interface ConditionFilter extends FilterConditionMatcher, Filter {

    @Override
    default Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {

        boolean isMatched = this.match(invoker, invocation);

        if (isMatched) {
            return this.doInvoke(invoker, invocation);
        }

        return invoker.invoke(invocation);
    }

    Result doInvoke(Invoker<?> invoker, Invocation invocation);

    interface Listener extends BaseFilter.Listener, FilterConditionMatcher{

        default void onResponse(Result result, Invoker<?> invoker, Invocation invocation){
            if (this.match(invoker, invocation)) {
                this.doCompleted(result, invoker, invocation);
            }
        }

        default void doCompleted(Result result, Invoker<?> invoker, Invocation invocation){

        }

        default void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
            if (this.match(invoker, invocation)) {
                this.doError(t, invoker, invocation);
            }
        }

        default void doError(Throwable t, Invoker<?> invoker, Invocation invocation){}
    }

}
