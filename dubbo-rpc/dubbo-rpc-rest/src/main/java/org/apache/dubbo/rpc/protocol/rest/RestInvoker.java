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
package org.apache.dubbo.rpc.protocol.rest;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.TimeoutException;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.AbstractInvoker;

public class RestInvoker<T> extends AbstractInvoker<T> {

    private final Invoker<T> target;

    public RestInvoker(Class<T> type, URL url, Invoker<T> target) {
        super(type, url);
        this.target = target;
    }

    @Override
    protected Result doInvoke(Invocation inv) throws Throwable {
        try {
            return target.invoke(inv);
        } catch (RpcException e) {
            if (e.getCode() == RpcException.UNKNOWN_EXCEPTION) {
                e.setCode(getErrorCode(e.getCause()));
            }
            throw e;
        } catch (Throwable e) {
            throw getRpcException(getInterface(),inv, e);
        }
    }


    private RpcException getRpcException(Class<?> type,Invocation invocation, Throwable e) {
        RpcException re = new RpcException("Failed to invoke remote service: " + type + ", method: "
                + invocation.getMethodName() + ", cause: " + e.getMessage(), e);
        re.setCode(getErrorCode(e));
        return re;
    }

    private int getErrorCode(Throwable e) {
        if (e instanceof TimeoutException) {
            return RpcException.TIMEOUT_EXCEPTION;
        }
        return RpcException.UNKNOWN_EXCEPTION;
    }
}
