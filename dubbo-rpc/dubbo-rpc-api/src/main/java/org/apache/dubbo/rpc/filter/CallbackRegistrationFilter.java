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

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ListenableFilter;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import java.util.List;

@Activate(group = {Constants.PROVIDER, Constants.CONSUMER}, order = -999999)
public class CallbackRegistrationFilter implements Filter {

    private List<Filter> filters;

    public CallbackRegistrationFilter() {
    }

    public void setFilters(List<Filter> filters) {
        this.filters = filters;
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Result asyncResult = invoker.invoke(invocation);

        asyncResult.thenApplyWithContext(r -> {
            for (int i = filters.size() - 1; i >= 0; i--) {
                Filter filter = filters.get(i);
                // onResponse callback
                if (filter instanceof ListenableFilter) {
                    Listener listener = ((ListenableFilter) filter).listener();
                    if (listener != null) {
                        listener.onResponse(r, invoker, invocation);
                    }
                } else {
                    filter.onResponse(r, invoker, invocation);
                }
            }
            return r;
        });

        return asyncResult;
    }

}
