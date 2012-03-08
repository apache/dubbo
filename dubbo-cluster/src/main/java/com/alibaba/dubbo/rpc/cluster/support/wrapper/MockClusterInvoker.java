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
package com.alibaba.dubbo.rpc.cluster.support.wrapper;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.cluster.Directory;
import com.alibaba.dubbo.rpc.support.MockInvoker;

/**
 * @author chao.liuc
 */
public class MockClusterInvoker<T> implements Invoker<T>{
	
	private static final Logger logger = LoggerFactory.getLogger(MockClusterInvoker.class);

	private final Directory<T> directory ;
	
	private final Invoker<T> invoker;

    public MockClusterInvoker(Directory<T> directory, Invoker<T> invoker) {
       	this.directory = directory;
       	this.invoker = invoker;
    }

	public URL getUrl() {
		return directory.getUrl();
	}

	public boolean isAvailable() {
		return directory.isAvailable();
	}

	public void destroy() {
		this.invoker.destroy();
	}

	public Class<T> getInterface() {
		return directory.getInterface();
	}

	public Result invoke(Invocation invocation) throws RpcException {
		Result result = null;
        
        String value = directory.getUrl().getMethodParameter(invocation.getMethodName(), Constants.MOCK_KEY, Boolean.FALSE.toString()).trim(); 
        if (value.length() == 0 || value.equalsIgnoreCase("false")){
        	//no mock
        	result = this.invoker.invoke(invocation);
        } else if (value.startsWith("force")) {
        	//force:direct mock
        	result = doMockInvoke(invocation, null);
        } else {
        	//fail-mock
        	try {
        		result = this.invoker.invoke(invocation);
        	}catch (RpcException e) {
				if (e.isBiz()) {
					throw e;
				} else {
					result = doMockInvoke(invocation, e);
				}
			}
        }
        return result;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Result doMockInvoke(Invocation invocation,RpcException e){
    	Result result = null;
    	List<Invoker<T>> mockInvokers = selectMockInvoker(invocation);
    	Invoker<T> minvoker ;
		if (mockInvokers.size() == 0){
			minvoker = (Invoker<T>) new MockInvoker(directory.getUrl());
			
		} else {
			minvoker = mockInvokers.get(0);
		}
		try {
//			result = doInvoke(invocation, mockInvokers, loadbalance) ;
			result = minvoker.invoke(invocation);
		} catch (RpcException me) {
			if (e != null) {
				logger.warn("mock invoker invoke error : " + StringUtils.toString(me), e);
			} else {
				logger.warn("mock invoker invoke error", me);
			}
		}
		//如果mock invoke结果为null,则说明发生异常，褪化为返回null业务结果 
		//void类型也可通过null处理 
		if (result == null){
			result = new RpcResult();
			((RpcResult)result).setValue(null);
		} else if (result.hasException()){
			//如果exception是mock exception则抛出
			if (result.getException() instanceof RpcException && ((RpcException)result.getException()).isMock()){
				if (e != null) {
					logger.error(e);
				}
				throw (RpcException)result.getException();
			} 
		}
		return result;
    }

	/**
     * 返回MockInvoker
     * 契约：
     * directory根据invocation中是否有Constants.INVOCATION_NEED_MOCK，来判断获取的是一个normal invoker 还是一个 mock invoker
     * 如果directorylist 返回多个mock invoker，只使用第一个invoker.
     * @param invocation
     * @return 
     */
    private List<Invoker<T>> selectMockInvoker(Invocation invocation){
    	//TODO generic invoker？
        if (invocation instanceof RpcInvocation){
            //存在隐含契约(虽然在接口声明中增加描述，但扩展性会存在问题.同时放在attachement中的做法需要改进
        	((RpcInvocation)invocation).setAttachment(Constants.INVOCATION_NEED_MOCK, Boolean.TRUE.toString());
            List<Invoker<T>> invokers = directory.list(invocation);
            return invokers == null ? new ArrayList<Invoker<T>>(1) : invokers;
        } else {
            return new ArrayList<Invoker<T>>(1) ;
        }
    }
}