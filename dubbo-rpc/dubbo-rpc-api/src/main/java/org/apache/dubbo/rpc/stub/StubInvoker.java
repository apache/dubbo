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

package org.apache.dubbo.rpc.stub;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.proxy.AbstractProxyInvoker;

import java.util.Map;

public class StubInvoker<T> extends AbstractProxyInvoker<T> {

    private final Map<String, StubMethodHandler<?, ?>> handlers;

    public StubInvoker(T proxy, URL url, Class<T> type,
        Map<String, StubMethodHandler<?, ?>> handlers) {
        super(proxy, type, url);
        this.handlers = handlers;
    }

    @Override
    protected Object doInvoke(T proxy, String methodName, Class<?>[] parameterTypes,
        Object[] arguments) throws Throwable {
        return handlers.get(methodName).invoke(arguments);
    }
}
