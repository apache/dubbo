/*
 * Copyright 1999-2101 Alibaba Group.
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

import java.lang.reflect.InvocationTargetException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import com.alibaba.dubbo.common.bytecode.Wrapper;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcResult;

/**
 * Generic Remote object adapter to rmi invocation handler.
 * 
 * @serial
 * @author qian.lei
 */
class RemoteObject2RmiInvocationHandler implements RmiInvocationHandler
{
	private Remote mRemote;

	private Wrapper mWrapper;

	RemoteObject2RmiInvocationHandler(Remote remote, Class<?> type)
	{
		// check remote object and interface.
		if( type.isInterface() == false )
			throw new IllegalArgumentException("Service type must be interface. " + type.getName());

		if( type.isInstance(remote) == false )
			throw new IllegalArgumentException("Remote object must implement interface: " + type.getName());

		mRemote = remote;
		mWrapper = Wrapper.getWrapper(type);
	}

	public Result invoke(Invocation inv)
	    throws RemoteException, NoSuchMethodException, IllegalAccessException, InvocationTargetException
	{
		RpcResult result = new RpcResult();
		try
		{
			result.setResult(mWrapper.invokeMethod(mRemote, inv.getMethodName(), inv.getParameterTypes(), inv.getArguments()));
		}
        catch(InvocationTargetException e)
        {
            Throwable rmiInvocationEx = e.getTargetException();
            if(null == rmiInvocationEx) throw e; 
            
            if(rmiInvocationEx.getClass().getName().startsWith("java.rmi.")
                    || rmiInvocationEx.getClass().getName().startsWith("javax.rmi.")) {
                throw new RemoteException("", rmiInvocationEx);
            }
            result.setException(rmiInvocationEx);
        }
        
		return result;
	}

}