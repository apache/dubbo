package org.apache.dubbo.rpc.proxy.asm;

import java.util.Map;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.proxy.AbstractProxyFactory;
import org.apache.dubbo.rpc.proxy.AbstractProxyInvoker;

public class AsmProxyFactory extends AbstractProxyFactory {

	@Override
	public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) throws RpcException {
		Wrapper reflect = new Wrapper();

		Map<String, MethodExecute<?>> executeMap = reflect.getInvoke(proxy, type);
		return new AbstractProxyInvoker<T>(proxy, type, url) {
			@Override
			protected Object doInvoke(T proxy, String methodName, Class<?>[] parameterTypes, Object[] arguments) throws Throwable {
				MethodExecute<?> execute = executeMap.get(ReflectUtils.getAlias(methodName, parameterTypes));
				return execute.execute(arguments);
			}
		};
	}

	@Override
	public <T> T getProxy(Invoker<T> invoker, Class<?>[] types) {
		Wrapper reflect = new Wrapper();
		return  reflect.getProxy(types, invoker);

	}

	
	
}
