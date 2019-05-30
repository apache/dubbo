package org.apache.dubbo.rpc.proxy.asm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.proxy.asm.MethodExecuteTest.ReturnInt;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ReflectUtilsTest {

	ReflectUtils reflect = new ReflectUtils();
 
	@Test
	public void getProxy() throws Throwable {
		Class<?>[] clazzArray = new Class<?>[] { AsmTestServer.class };

		AsmTestServer remote = (AsmTestServer) reflect.getProxy(clazzArray, in);
//		remote.notReturn();
//		remote.notThrowable();
//		
//		remote.returnInt();
//		remote.returnIntArray();
//		remote.returnLong();
//		remote.returnLongArray();
//		remote.returnObject();
//		remote.returnObjectArray();
//		
//		remote.parameterInt(1);
//		remote.parameterIntArray(new int[1]);
//		remote.parameterLong(2L);
//		remote.parameterLongArray(new long[2]);
//		remote.parameterObject("123");
//		remote.parameterObjectArray(new String[]{"123123"});
//		remote.execte(1, 1);
//		remote.execte("sdf", new ArrayList<>());
//		remote.execute(1, 2L);
//		remote.execute(1, 2L,3);
//		remote.execute(1, 2L, new Integer(3), new Long(4));
//		remote.execute(1, 2L, new Integer(3), new Long(4), "123");
//		remote.execute(1, 3,3, 5L, "6", new ArrayList<>());
//		remote.execute(3, 5L, "6", new ArrayList<>(), new HashMap<>());
//		remote.execute(new ArrayList<>());
//		remote.execute(1, 2L, 3, 5L, "6", new ArrayList<>(), new HashMap<>());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void methodExecute() throws Throwable {
		
		Invoker<String> mockInvoker = Mockito.mock(Invoker.class);
		
		Result mockResult = Mockito.mock(Result.class);
		Mockito.when(mockInvoker.invoke(Mockito.any())).thenReturn(mockResult);
		Mockito.when(mockResult.recreate())
//		.thenReturn(null)
//		.thenReturn(null)
//		.thenReturn(1)
//		.thenReturn(2L)
		.thenReturn("test")
		.thenReturn(new int[] {1})
		.thenReturn(new long[] {2L});
		
		TestAsmProxy tap = new TestAsmProxy(mockInvoker);		
		Map<String, MethodExecute<?>> mehtodMap = reflect.getInvoke(tap, tap.getClass());
		
		Iterator<Entry<String, MethodExecute<?>>> it = mehtodMap.entrySet().iterator();
		
		while(it.hasNext()) {
			Object object = it.next().getValue().execute(null);
			if(object == null) {
				System.out.println("null");
			}else {
				System.out.println(object);
			}
			
		}
		
	}
	
	Invoker<String> in = new Invoker<String>() {

		@Override
		public URL getUrl() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isAvailable() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void destroy() {
			// TODO Auto-generated method stub

		}

		@Override
		public Class<String> getInterface() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Result invoke(Invocation invocation) throws RpcException {
			// TODO Auto-generated method stub
			return null;
		}
	};
}
