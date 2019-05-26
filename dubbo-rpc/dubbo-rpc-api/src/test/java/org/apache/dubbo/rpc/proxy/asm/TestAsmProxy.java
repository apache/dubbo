package org.apache.dubbo.rpc.proxy.asm;

import java.util.List;
import java.util.Map;

import org.apache.dubbo.rpc.Invoker;

public class TestAsmProxy extends AbstractAsmProxy implements  AsmTestServer{

	public TestAsmProxy(Invoker<?> handler) {
		super(handler);
	}

	private static  MethodStatement sayHello_statement = new MethodStatement();

	
	public void notReturn() {
		super.invoke(sayHello_statement);
		
	}

	
	public void notThrowable() throws Throwable {
		super.invoke(sayHello_statement);
	}

	
	public int returnInt() {
		Integer in = super.invoke(sayHello_statement);
		if(in != null) {
			return in.intValue();
		}
		return 0;
	}



	public byte returnByte() {
		return super.invoke(sayHello_statement);
	}
	
	
	public long returnLong() {
		Long in = super.invoke(sayHello_statement);
		if(in != null) {
			return in.longValue();
		}
		return 0L;
	}

	
	public String returnObject() {
		return super.invoke(sayHello_statement);
	}

	
	public int[] returnIntArray() {
		return super.invoke(sayHello_statement);
	}

	
	public long[] returnLongArray() {
		return super.invoke(sayHello_statement);
	}

	
	public String[] returnObjectArray() {
		return super.invoke(sayHello_statement);
	}

	
	public void parameterInt(int i) {
		super.invoke(sayHello_statement , new Object[] {i});
	}

	
	public void parameterLong(long l) {
		super.invoke(sayHello_statement , new Object[] {l});
	}

	
	public void parameterIntArray(int[] intArray) {
		super.invoke(sayHello_statement , new Object[] {intArray});
	}

	
	public void parameterLongArray(long[] longArray) {
		super.invoke(sayHello_statement , new Object[] {longArray});
	}

	
	public void parameterObject(String string) {
		super.invoke(sayHello_statement , new Object[] {string});
	}

	
	public void parameterObjectArray(String[] stringArray) {
		super.invoke(sayHello_statement , new Object[] {stringArray});
	}

	public String execute(List<String> list) {
		return super.invoke(sayHello_statement , new Object[] {list});
	}
	
	
	public String execute(int in, long lo) {
		return super.invoke(sayHello_statement , new Object[] {in , lo});
	}
	
	public String execute(int in, long lo , Integer integer) {
		return super.invoke(sayHello_statement , new Object[] {in , lo,integer});
	}
	
	public String execute(int in, long lo , Integer integer , Long lon) {
		return super.invoke(sayHello_statement , new Object[] {in , lo,integer, lon});
	}
	
	public String execute(int in, long lo , Integer integer , Long lon , String str) {
		return super.invoke(sayHello_statement , new Object[] {in , lo,integer, lon ,str});
	}
	
	public String execute(int in, long lo , Integer integer , Long lon , String str ,List<String> list) {
		return super.invoke(sayHello_statement , new Object[] {in , lo,integer, lon ,str , list});
	}
	
	public String execute(Integer integer, Long lon, String string, List<String> list,
			Map<String, String> map) {
		return super.invoke(sayHello_statement , new Object[] { integer , lon , string , list , map});
	}
	
	public String execute(int in, long lo, Integer integer, Long lon, String string, List<String> list,
			Map<String, String> map) {
		return super.invoke(sayHello_statement , new Object[] {in , lo , integer , lon , string , list , map});
	}


	public String execte(int str, int list) {
		return super.invoke(sayHello_statement , new Object[] {str , list});
	}
	
	public String execte(long str, long list, long three) {
		return super.invoke(sayHello_statement , new Object[] {str , list , three});
	}
	
	public String execte(long str, long list) {
		return super.invoke(sayHello_statement , new Object[] {str , list});
	}
	
	public String execte(String str, List<String> list) {
		return super.invoke(sayHello_statement , new Object[] {str , list});
	}
	

}
