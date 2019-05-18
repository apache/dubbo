package org.apache.dubbo.rpc.proxy.asm;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.proxy.AbstractProxyFactory;
import org.apache.dubbo.rpc.proxy.AbstractProxyInvoker;

public class ASMProxyFactory extends AbstractProxyFactory {

	@Override
	public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) throws RpcException {
		return new AbstractProxyInvoker<T>(proxy, type, url) {

			@Override
			protected Object doInvoke(T proxy, String methodName, Class<?>[] parameterTypes, Object[] arguments) throws Throwable {
				return null;
			}
		};
	}

	@Override
	public <T> T getProxy(Invoker<T> invoker, Class<?>[] types) {
		return  null;

	}

	
	
}
