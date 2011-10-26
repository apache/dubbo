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

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.protocol.AbstractInvoker;

/**
 * rmi rpc invoker.
 * 
 * @author qian.lei
 */
public class RmiInvoker<T> extends AbstractInvoker<T> {

	private RmiInvocationHandler proxy;
	
	static boolean isInstance(Object obj, String interfaceClazzName) {
	    for(Class<?> clazz = obj.getClass(); clazz != null && !clazz.equals(Object.class);
	            clazz = clazz.getSuperclass()) {
	        Class<?>[] interfaces = clazz.getInterfaces();
	        for(Class<?> itf : interfaces) {
	            if(itf.getName().equals(interfaceClazzName)) return true;
	        }
	    }
	    
	    return false;
	}
	
	public RmiInvoker(Class<T> serviceType, URL url)
	{
		super(serviceType, url);
		try
		{
			Registry reg = LocateRegistry.getRegistry(url.getHost(), url.getPort());
			String path = url.getPath();
			if (path == null || path.length() == 0) {
			    path = serviceType.getName();
			}
			Remote rmt = reg.lookup(path);
			
			if( rmt instanceof RmiInvocationHandler ) {
			    // is the Remote wrap type in Dubbo2
				proxy = (RmiInvocationHandler)rmt;
			}
			else if(isInstance(rmt, "org.springframework.remoting.rmi.RmiInvocationHandler")) {
                // is the Remote wrap type in spring? (spring rmi is used in Dubbo1)
			    proxy = new SpringHandler2RmiInvocationHandler((org.springframework.remoting.rmi.RmiInvocationHandler)rmt, serviceType);
			}
			else
				proxy = new RemoteObject2RmiInvocationHandler(rmt, serviceType);
		}
		catch(RemoteException e)
		{
		    Throwable cause = e.getCause();
		    boolean isExportedBySpringButNoSpringClass = ClassNotFoundException.class.isInstance(cause)
                && cause.getMessage().contains("org.springframework.remoting.rmi.RmiInvocationHandler");
		    
		    String msg = String.format("Can not create remote object%s. url = %s",
		            isExportedBySpringButNoSpringClass ? "(Rmi object is exported by spring rmi but NO spring class org.springframework.remoting.rmi.RmiInvocationHandler at consumer side)" : "",
		            		url);
			throw new RpcException(msg, e);
		}
		catch(NotBoundException e)
		{
			throw new RpcException("Rmi service not found. url = " + url, e);
		}
	}

	@Override
	protected Object doInvoke(Invocation invocation) throws Throwable {
		Result result;
		try
		{
			result = proxy.invoke((RpcInvocation) invocation);
		}
		catch(Throwable e) // here is non-biz exception, wrap it.
		{
			throw new RpcException(e);
		}
		return result.recreate();
	}
}