package org.apache.dubbo.rpc.proxy.asm;

import java.util.List;
import java.util.Map;

import org.apache.dubbo.rpc.Invoker;

public class TestAsmProxy extends AbstractAsmProxy implements  AsmTestServer{

	public TestAsmProxy(Invoker<?> handler) {
		super(handler);
	}

	private static  MethodStatement sayHello_statement = new MethodStatement();

	@Override
	public void notReturn() {
		super.invoke(sayHello_statement);
		
	}

	@Override
	public void notThrowable() throws Throwable {
		super.invoke(sayHello_statement);
	}

	@Override
	public int returnInt() {
		return super.invoke(sayHello_statement);
	}

	@Override
	public long returnLong() {
		return super.invoke(sayHello_statement);
	}

	@Override
	public String returnObject() {
		return super.invoke(sayHello_statement);
	}

	@Override
	public int[] returnIntArray() {
		return super.invoke(sayHello_statement);
	}

	@Override
	public long[] returnLongArray() {
		return super.invoke(sayHello_statement);
	}

	@Override
	public String[] returnObjectArray() {
		return super.invoke(sayHello_statement);
	}

	@Override
	public void parameterInt(int i) {
		super.invoke(sayHello_statement , new Object[] {i});
	}

	@Override
	public void parameterLong(long l) {
		super.invoke(sayHello_statement , new Object[] {l});
	}

	@Override
	public void parameterIntArray(int[] intArray) {
		super.invoke(sayHello_statement , new Object[] {intArray});
	}

	@Override
	public void parameterLongArray(long[] longArray) {
		super.invoke(sayHello_statement , new Object[] {longArray});
	}

	@Override
	public void parameterObject(String string) {
		super.invoke(sayHello_statement , new Object[] {string});
	}

	@Override
	public void parameterObjectArray(String[] stringArray) {
		super.invoke(sayHello_statement , new Object[] {stringArray});
	}

	@Override
	public String execute(int in, long lo, Integer integer, Long lon, String string, List<String> list,
			Map<String, String> map) {
		return super.invoke(sayHello_statement , new Object[] {in , lo , integer , lon , string , list , map});
	}
	

}
