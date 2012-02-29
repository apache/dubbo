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
package com.alibaba.dubbo.rpc.filter;

import java.lang.reflect.Method;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.utils.PojoUtils;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.service.GenericException;

/**
 * GenericInvokerFilter.
 * 
 * @author william.liangf
 */
public class GenericFilter implements Filter {
    
    public Result invoke(Invoker<?> invoker, Invocation inv) throws RpcException {
        if (inv.getMethodName().equals(Constants.$INVOKE) 
                && inv.getArguments() != null
                && inv.getArguments().length == 3
                && ! invoker.getUrl().getParameter(Constants.GENERIC_KEY, false)) {
            String name = ((String) inv.getArguments()[0]).trim();
            String[] types = (String[]) inv.getArguments()[1];
            Object[] args = (Object[]) inv.getArguments()[2];
            try {
                Method method = ReflectUtils.findMethodByMethodSignature(invoker.getInterface(), name, types);
                Class<?>[] params = method.getParameterTypes();
                if (args == null) {
                    args = new Object[params.length];
                }
                args = PojoUtils.realize(args, params, method.getGenericParameterTypes());
                Result result = invoker.invoke(new RpcInvocation(method, args, inv.getAttachments()));
                if (result.hasException()
                        && ! (result.getException() instanceof GenericException)) {
                    return new RpcResult(new GenericException(result.getException()));
                }
                return new RpcResult(PojoUtils.generalize(result.getResult()));
            } catch (NoSuchMethodException e) {
                throw new RpcException(e.getMessage(), e);
            } catch (ClassNotFoundException e) {
                throw new RpcException(e.getMessage(), e);
            }
        }
        return invoker.invoke(inv);
    }

}