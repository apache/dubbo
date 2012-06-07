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

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.injvm.InjvmProtocol;

/**
 * LocalInvokerFilter
 * 
 * @author chao.liuc
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 * 
 */
@Activate(group = Constants.CONSUMER, order = 100000)
public class InjvmInvokerFilter implements Filter {

	private Invoker<?> localInvoker;

	private static final Protocol protocol = ExtensionLoader
			.getExtensionLoader(Protocol.class).getAdaptiveExtension();

	public Result invoke(Invoker<?> invoker, Invocation invocation)
			throws RpcException {
		if (InjvmProtocol.getInjvmProtocol().isInjvmRefer(invoker.getUrl())) {
			Invoker<?> lInvoker = getLocalInvoker(invoker);
			return lInvoker.invoke(invocation);
		} else {
			return invoker.invoke(invocation);
		}
	}

	private Invoker<?> getLocalInvoker(Invoker<?> invoker) {
		if (localInvoker == null) {
			try {
				localInvoker = protocol.refer(invoker.getInterface(), new URL(
						Constants.LOCAL_PROTOCOL, NetUtils.LOCALHOST, 0,
						invoker.getUrl().getParameter(Constants.INTERFACE_KEY),
						invoker.getUrl().getParameters()));
			} catch (Throwable e) { /* ignore */
			}
		}
		return localInvoker;
	}
}