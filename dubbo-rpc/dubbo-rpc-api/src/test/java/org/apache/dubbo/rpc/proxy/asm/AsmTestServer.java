package org.apache.dubbo.rpc.proxy.asm;

import java.util.List;
import java.util.Map;

public interface AsmTestServer {

//	void notReturn();
//	
//	void notThrowable() throws Throwable;
//	
//	int  returnInt();
//	
//	long returnLong();
//	
//	String returnObject();
//	
//	int[] returnIntArray();
//	
//	long[] returnLongArray();
//	
//	String[] returnObjectArray();
//	
//	void parameterInt(int i);
//	
//	void parameterLong(long l);
//	
//	void parameterIntArray(int[] intArray);
//
//	void parameterLongArray(long[] longArray);
//	
//	void parameterObject(String string);
//	
//	void parameterObjectArray(String[] stringArray);
	
//	String execte(int str , int list);
	
	
//	String execte(long str , long list , long l);
	
//	String execte(String str , List<String> list);
	
//	String execute(int in , long lo);
	
//	String execute(int in , long lo, Integer integer);
	
//	String execute(int in , long lo, Integer integer, Long lon );
	
//	String execute(int in , long lo, Integer integer, Long lon , String str );
	
	String execute(int in, long lo , Integer integer , Long lon , String str ,List<String> list);

	
//	String execute(List<String> list);
	
//	String execute(int in , long lo , Integer integer , Long lon , String string,List<String> list , Map<String,String> map);
	
	
	
}
