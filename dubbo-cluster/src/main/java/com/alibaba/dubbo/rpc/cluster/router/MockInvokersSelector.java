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
package com.alibaba.dubbo.rpc.cluster.router;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Router;

/**
 * mock invoker选择器
 * @author chao.liuc
 *
 */
public class MockInvokersSelector implements Router {

	public <T> List<Invoker<T>> route(final List<Invoker<T>> invokers,
			URL url, final Invocation invocation) throws RpcException {
		if (invocation.getAttachments() == null) {
			return getNormalInvokers(invokers);
		} else {
			String value = invocation.getAttachments().get(Constants.INVOCATION_NEED_MOCK);
			if (value == null) 
				return getNormalInvokers(invokers);
			else if (Boolean.TRUE.toString().equalsIgnoreCase(value)){
				return getMockedInvokers(invokers);
			} 
		}
		return invokers;
	}
	
	private <T> List<Invoker<T>> getMockedInvokers(final List<Invoker<T>> invokers) {
		if (! hasMockProviders(invokers)){
			return null;
		}
		List<Invoker<T>> sInvokers = new ArrayList<Invoker<T>>(1);
		for (Invoker<T> invoker : invokers){
			if (invoker.getUrl().getProtocol().equals(Constants.MOCK_PROTOCOL)){
				sInvokers.add(invoker);
			}
		}
		return sInvokers;
	}
	
	private <T> List<Invoker<T>> getNormalInvokers(final List<Invoker<T>> invokers){
		if (! hasMockProviders(invokers)){
			return invokers;
		} else {
			List<Invoker<T>> sInvokers = new ArrayList<Invoker<T>>(invokers.size());
			for (Invoker<T> invoker : invokers){
				if (! invoker.getUrl().getProtocol().equals(Constants.MOCK_PROTOCOL)){
					sInvokers.add(invoker);
				}
			}
			return sInvokers;
		}
	}
	
	private <T> boolean hasMockProviders(final List<Invoker<T>> invokers){
		boolean hasMockProvider = false;
		for (Invoker<T> invoker : invokers){
			if (invoker.getUrl().getProtocol().equals(Constants.MOCK_PROTOCOL)){
				hasMockProvider = true;
				break;
			}
		}
		return hasMockProvider;
	}

    public URL getUrl() {
        return null;
    }

    public int compareTo(Router o) {
        return 1;
    }

}
