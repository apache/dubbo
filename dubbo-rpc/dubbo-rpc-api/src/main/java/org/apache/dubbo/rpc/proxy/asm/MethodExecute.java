package org.apache.dubbo.rpc.proxy.asm;

public interface MethodExecute<S> {

	public <T> T execute(Object[] arguments) throws Throwable ;
}
