package org.apache.dubbo.rpc.proxy.asm;

import org.apache.dubbo.rpc.proxy.RemoteService;

public class TestMethodExecute extends AbstractMethodExecute<RemoteService>{

	public TestMethodExecute(RemoteService object) {
		super(object);
	}

	@SuppressWarnings("unchecked")
	public <T> T execute(Object[] arguments)  throws Throwable {
			return (T) object.setTesttest((int)arguments[0], (Integer)arguments[1], (long)arguments[2], (Long)arguments[3]);
	}
	
	public <T> T executes(Object[] arguments)  throws Throwable {
		return (T) object.setTesttest((String)arguments[0], (String)arguments[1], (String)arguments[2], (String)arguments[3]);
	}
	
	public <T> T executess(Object[] arguments)  throws Throwable {
		return (T) object.setThreadGroup((String[])arguments[0]);
	}
	
	public void executesss(Object[] arguments)  throws Throwable {
		object.setThreadName();
	}
	
	public Object executessss(Object[] arguments)  throws Throwable {
		return  object.getThreadName();
		
		
	}

	
	public void executesssss(Object[] arguments)  throws Throwable {
		object.setThreadGroup((String)arguments[0]);
	}
}
