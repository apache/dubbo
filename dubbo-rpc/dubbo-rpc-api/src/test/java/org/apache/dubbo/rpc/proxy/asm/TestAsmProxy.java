package org.apache.dubbo.rpc.proxy.asm;

import java.rmi.RemoteException;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.proxy.RemoteService;

public class TestAsmProxy extends AbstractAsmProxy implements  RemoteService{

	public TestAsmProxy(Invoker<?> handler) {
		super(handler);
	}

	private static  MethodStatement sayHello_statement = new MethodStatement();
	
	public String sayHello(String name) throws RemoteException {
			return super.invoke(sayHello_statement, new Object[] {name});
	}

	public String getThreadName() throws RemoteException {
			return super.invoke(sayHello_statement);
	}
	
	public void setThreadName() throws RemoteException {
		super.invoke(sayHello_statement);
	}
		
	public String[] setThreadGroup(String[] threadGroup)throws RemoteException{
		return super.invoke(sayHello_statement , new Object[] {threadGroup});
	}
}
