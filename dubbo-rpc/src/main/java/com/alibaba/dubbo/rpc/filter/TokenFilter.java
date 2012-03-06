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

import java.util.Map;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;

/**
 * TokenInvokerFilter
 * 
 * @author william.liangf
 */
@Activate(group = Constants.PROVIDER, value = Constants.TOKEN_KEY)
public class TokenFilter implements Filter {

	public Result invoke(Invoker<?> invoker, Invocation inv)
			throws RpcException {
	    String token = invoker.getUrl().getParameter(Constants.TOKEN_KEY);
	    if (ConfigUtils.isNotEmpty(token)) {
	        Class<?> serviceType = invoker.getInterface();
	        Map<String, String> attachments = inv.getAttachments();
    		String remoteToken = attachments == null ? null : attachments.get(Constants.TOKEN_KEY);
    		if (! token.equals(remoteToken)) {
    			throw new RpcException("Invalid token! Forbid invoke remote service " + serviceType + " method " + inv.getMethodName() + "() from consumer " + RpcContext.getContext().getRemoteHost() + " to provider "  + RpcContext.getContext().getLocalHost());
    		}
	    }
		return invoker.invoke(inv);
	}

}