package org.apache.dubbo.rpc.proxy.asm;

public interface MethodExecute {

	public <T> T execute(Object[] arguments);
}
