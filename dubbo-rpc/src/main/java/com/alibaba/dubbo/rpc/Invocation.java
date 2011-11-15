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
package com.alibaba.dubbo.rpc;

import java.util.Map;

/**
 * Rpc invocation. (API, Prototype, ThreadSafe)
 * 
 * @serial Don't change the class name.
 * @see com.alibaba.dubbo.rpc.Invoker#invoke(Invocation)
 * @see com.alibaba.dubbo.rpc.RpcInvocation
 * @author qian.lei
 * @author william.liangf
 */
public interface Invocation {
    
	/**
	 * get method name.
	 * 
	 * @return method name.
	 */
	String getMethodName();

	/**
	 * get parameter types.
	 * 
	 * @return parameter types.
	 */
	Class<?>[] getParameterTypes();

	/**
	 * get arguments.
	 * 
	 * @return arguments.
	 */
	Object[] getArguments();

	/**
	 * get attachments.
	 * 
	 * @return attachments.
	 */
	Map<String, String> getAttachments();

}