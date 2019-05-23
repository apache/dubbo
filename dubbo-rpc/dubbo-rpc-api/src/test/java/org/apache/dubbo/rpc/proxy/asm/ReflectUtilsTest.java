package org.apache.dubbo.rpc.proxy.asm;

import java.lang.reflect.Field;
import java.rmi.RemoteException;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.proxy.RemoteService;
import org.junit.jupiter.api.Test;

public class ReflectUtilsTest {

	private String[] strArray = new String[0];
	
	
	@Test
	public void testArrayType() throws NoSuchFieldException, SecurityException {
		Field field = this.getClass().getDeclaredField("strArray");
		System.out.println(field.getGenericType());
	}
	
	@Test
	public void getProxy() {
		Class<?>[] clazzArray = new Class<?>[] {RemoteService.class};
		
		ReflectUtils reflect = new ReflectUtils();
		
		RemoteService remote = (RemoteService)reflect.getProxy(clazzArray , in);
		try {
			//remote.sayHello("1");
			//remote.getThreadName();
			//remote.setThreadName();
			remote.setThreadGroup(null);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void test() {
		TestAsmProxyDump testAsmProxyDump = new TestAsmProxyDump();
		try {
			testAsmProxyDump.dump();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
