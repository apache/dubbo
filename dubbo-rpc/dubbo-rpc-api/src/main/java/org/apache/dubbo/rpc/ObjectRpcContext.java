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

import org.apache.dubbo.common.threadlocal.InternalThreadLocal;

/**
 * Thread local context. (API, ThreadLocal, ThreadSafe)
 * <p>
 * Note: RpcContext is a temporary state holder. States in RpcContext changes every time when request is sent or received.
 * For example: A invokes B, then B invokes C. On service B, RpcContext saves invocation info from A to B before B
 * starts invoking C, and saves invocation info from B to C after B invokes C.
 *
 * @export
 * @see org.apache.dubbo.rpc.filter.ContextFilter
 */
public class ObjectRpcContext extends GenericRpcContext<Object> {

    /**
     * use internal thread local to improve performance
     */
    // FIXME REQUEST_CONTEXT
    private static final InternalThreadLocal<ObjectRpcContext> LOCAL = new InternalThreadLocal<ObjectRpcContext>() {
        @Override
        protected ObjectRpcContext initialValue() {
            return new ObjectRpcContext();
        }
    };

    // FIXME RESPONSE_CONTEXT
    private static final InternalThreadLocal<ObjectRpcContext> SERVER_LOCAL = new InternalThreadLocal<ObjectRpcContext>() {
        @Override
        protected ObjectRpcContext initialValue() {
            return new ObjectRpcContext();
        }
    };

    protected ObjectRpcContext() {
    }

    /**
     * get server side context.
     *
     * @return server context
     */
    public static ObjectRpcContext getServerContext() {
        return SERVER_LOCAL.get();
    }

    public static void restoreServerContext(ObjectRpcContext oldServerContext) {
        SERVER_LOCAL.set(oldServerContext);
    }

    /**
     * remove server side context.
     *
     * @see org.apache.dubbo.rpc.filter.ContextFilter
     */
    public static void removeServerContext() {
        SERVER_LOCAL.remove();
    }

    /**
     * get context.
     *
     * @return context
     */
    public static ObjectRpcContext getContext() {
        return LOCAL.get();
    }

    public static void restoreContext(ObjectRpcContext oldContext) {
        LOCAL.set(oldContext);
    }

    /**
     * remove context.
     *
     * @see org.apache.dubbo.rpc.filter.ContextFilter
     */
    public static void removeContext() {
        if (LOCAL.get().canRemove()) {
            LOCAL.remove();
        }
    }

    /**
     * @return
     * @throws IllegalStateException
     */
    @SuppressWarnings("unchecked")
    public static AsyncContext startAsync() throws IllegalStateException {
        ObjectRpcContext currentContext = getContext();
        if (currentContext.asyncContext == null) {
            currentContext.asyncContext = new AsyncContextImpl();
        }
        currentContext.asyncContext.start();
        return currentContext.asyncContext;
    }

}
