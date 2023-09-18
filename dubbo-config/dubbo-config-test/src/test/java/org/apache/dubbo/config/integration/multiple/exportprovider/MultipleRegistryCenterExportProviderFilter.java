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
package org.apache.dubbo.config.integration.multiple.exportprovider;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

@Activate(group = CommonConstants.PROVIDER, order = 10001)
public class MultipleRegistryCenterExportProviderFilter implements Filter,Filter.Listener {

    /**
     * The filter is called or not
     */
    private boolean called = false;

    /**
     * There has error after invoked
     */
    private boolean error = false;

    /**
     * The returned result
     */
    private String response;
    /**
     * Always call invoker.invoke() in the implementation to hand over the request to the next filter node.
     *
     * @param invoker
     * @param invocation
     */
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        called = true;
        return invoker.invoke(invocation);
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        response = String.valueOf(appResponse.getValue());
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        error = true;
    }

    /**
     * Returns if the filter has called.
     */
    public boolean hasCalled() {
        return called;
    }

    /**
     * Returns if there exists error.
     */
    public boolean hasError() {
        return error;
    }

    /**
     * Returns the response.
     */
    public String getResponse() {
        return response;
    }
}
