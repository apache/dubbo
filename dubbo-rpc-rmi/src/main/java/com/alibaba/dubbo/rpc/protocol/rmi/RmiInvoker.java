/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.protocol.rmi;

import java.io.IOException;
import java.net.SocketTimeoutException;

import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractInvoker;

/**
 * RmiInvoker.
 * 
 * @author qian.lei
 */
public class RmiInvoker<T> extends AbstractInvoker<T> {

    private Invoker<T> invoker;

    public RmiInvoker(Invoker<T> invoker) {
        super(invoker.getInterface(), invoker.getUrl());
        this.invoker = invoker;
    }

    @Override
    protected Result doInvoke(Invocation invocation) throws RpcException {
        try {
            Result result = invoker.invoke(invocation);
            Throwable e = result.getException();
            if (e != null) {
                String name = e.getClass().getName();
                if (name.startsWith("java.rmi.")
                        || name.startsWith("javax.rmi.")) {
                    throw setRpcExceptionCode(e, new RpcException("Failed to invoke remote service: " + getInterface() + ", method: "
                            + invocation.getMethodName() + ", cause: " + e.getMessage(), e));
                }
            }
            return result;
        } catch (RpcException e) {
            throw setRpcExceptionCode(e.getCause(), e);
        } catch (Throwable e) {
            throw setRpcExceptionCode(e, new RpcException(e.getMessage(), e));
        }
    }
    
    private RpcException setRpcExceptionCode(Throwable e, RpcException re) {
        if (e != null && e.getCause() != null) {
            Class<?> cls = e.getCause().getClass();
            if (SocketTimeoutException.class.equals(cls)) {
                re.setCode(RpcException.TIMEOUT_EXCEPTION);
            } else if (IOException.class.isAssignableFrom(cls)) {
                re.setCode(RpcException.NETWORK_EXCEPTION);
            } else if (ClassNotFoundException.class.isAssignableFrom(cls)) {
                re.setCode(RpcException.SERIALIZATION_EXCEPTION);
            }
        }
        return re;
    }

}