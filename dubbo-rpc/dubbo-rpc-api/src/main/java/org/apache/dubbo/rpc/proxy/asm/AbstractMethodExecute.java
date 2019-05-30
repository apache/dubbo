package org.apache.dubbo.rpc.proxy.asm;

public abstract class AbstractMethodExecute<S> implements MethodExecute<S> {

	S object;
	
	public AbstractMethodExecute(S object) {
		this.object = object;
	}
	
	protected S getObject(){
		return object;
	}
}
