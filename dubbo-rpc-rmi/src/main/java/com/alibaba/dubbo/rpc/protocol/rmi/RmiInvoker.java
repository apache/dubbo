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
            return invoker.invoke(invocation);
        } catch (RpcException e) {
            throw e;
        } catch (Throwable e) {
            throw new RpcException(e.getMessage(), e);
        }
    }

}