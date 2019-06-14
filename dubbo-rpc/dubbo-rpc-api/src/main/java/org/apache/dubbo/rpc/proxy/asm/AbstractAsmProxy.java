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
package org.apache.dubbo.rpc.proxy.asm;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;

public abstract class AbstractAsmProxy {

    private static final Logger logger = LoggerFactory.getLogger(AbstractAsmProxy.class);
    private final Invoker<?> invoker;

    public AbstractAsmProxy(Invoker<?> handler) {
        this.invoker = handler;
    }

    public <T> T invoke(MethodStatement ms) {
        return invoke(ms, null);
    }

    public <T> T invoke(MethodStatement ms, Object[] args) {
        return doInvoke(ms, args);
    }

    @SuppressWarnings("unchecked")
    protected <T> T doInvoke(MethodStatement ms, Object[] args) {
        try {
            return (T) invoker.invoke(createInvocation(ms, args)).recreate();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private RpcInvocation createInvocation(MethodStatement ms, Object[] args) {
        RpcInvocation invocation = new RpcInvocation(ms.getMethod(), ms.getParameterClass(), args);
        if (ms.isFutureReturnType()) {
            invocation.setAttachment(Constants.FUTURE_RETURNTYPE_KEY, "true");
            invocation.setAttachment(Constants.ASYNC_KEY, "true");
        }
        return invocation;
    }
}
