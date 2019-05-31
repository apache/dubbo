package org.apache.dubbo.rpc.proxy.asm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ReflectUtilsTest {

	ReflectUtils reflect = new ReflectUtils();
	AsmTestServer remote;
	AbstractAsmProxy asm;

	Random random = new Random();
	
	@BeforeEach
	public void before() throws Exception {
		Invoker<?> mockInvoker = Mockito.mock(Invoker.class);
		Result mockResult = Mockito.mock(Result.class);
		Mockito.when(mockInvoker.invoke(Mockito.any())).thenReturn(mockResult);
		Class<?>[] clazzArray = new Class<?>[] { AsmTestServer.class };
		remote = reflect.getProxy(clazzArray, mockInvoker);
		asm = (AbstractAsmProxy) Mockito.spy(remote);
		remote = (AsmTestServer) asm;
	}

	
	@Test
	public void notReturn() {
		String str = "notReturn";
		Mockito.doAnswer(new Answer<String>() {
			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				MethodStatement ms = invocation.getArgument(0);
				assertEquals(ms.getMethod(), str);
				assertNull(invocation.getArgument(1));
				invocation.getMock();
				return null;
			}
		}).when(asm).invoke(Mockito.any(), Mockito.any());
		remote.notReturn();
	}
	
	@Test
	public void notThrowable() throws Throwable {
		String str = "notThrowable";
		Mockito.doAnswer(new Answer<String>() {
			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				MethodStatement ms = invocation.getArgument(0);
				assertEquals(ms.getMethod(), str);
				assertNull(invocation.getArgument(1));
				invocation.getMock();
				return null;
			}
		}).when(asm).invoke(Mockito.any(), Mockito.any());
		remote.notThrowable();
	}
	
	@Test
	public void returnInt() throws Throwable {
		String str = "returnInt";
		Mockito.doAnswer(new Answer<Integer>() {
			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				MethodStatement ms = invocation.getArgument(0);
				assertEquals(ms.getMethod(), str);
				assertNull(invocation.getArgument(1));
				invocation.getMock();
				return 3;
			}
		}).when(asm).invoke(Mockito.any(), Mockito.any());
		assertEquals(remote.returnInt(),3);
	}
	
	@Test
	public void returnIntArray() throws Throwable {
		String str = "returnIntArray";
		Mockito.doAnswer(new Answer<int[]>() {
			@Override
			public int[] answer(InvocationOnMock invocation) throws Throwable {
				MethodStatement ms = invocation.getArgument(0);
				assertEquals(ms.getMethod(), str);
				assertNull(invocation.getArgument(1));
				invocation.getMock();
				return new int[] {3,4};
			}
		}).when(asm).invoke(Mockito.any(), Mockito.any());
		assertArrayEquals(remote.returnIntArray(), new int[] {3,4});
	}
	
	
	@Test
	public void returnLong()  {
		String str = "returnLong";
		Mockito.doAnswer(new Answer<Long>() {
			@Override
			public Long answer(InvocationOnMock invocation) throws Throwable {
				MethodStatement ms = invocation.getArgument(0);
				assertEquals(ms.getMethod(), str);
				assertNull(invocation.getArgument(1));
				invocation.getMock();
				return 4L;
			}
		}).when(asm).invoke(Mockito.any(), Mockito.any());
		assertEquals(remote.returnLong(),4L);
	}
	
	@Test
	public void returnLongArray() throws Throwable {
		String str = "returnLongArray";
		Mockito.doAnswer(new Answer<long[]>() {
			@Override
			public long[] answer(InvocationOnMock invocation) throws Throwable {
				MethodStatement ms = invocation.getArgument(0);
				assertEquals(ms.getMethod(), str);
				assertNull(invocation.getArgument(1));
				invocation.getMock();
				return new long[] {3,4};
			}
		}).when(asm).invoke(Mockito.any(), Mockito.any());
		assertArrayEquals(remote.returnLongArray(), new long[] {3,4});
	}
	
	
	@Test
	public void returnObject() {
		String str = "returnObject";
		Mockito.doAnswer(new Answer<String>() {
			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				MethodStatement ms = invocation.getArgument(0);
				assertEquals(ms.getMethod(), str);
				assertNull(invocation.getArgument(1));
				invocation.getMock();
				return str;
			}
		}).when(asm).invoke(Mockito.any(), Mockito.any());
		assertEquals(remote.returnObject(), str);
	}
	
	
	@Test
	public void returnObjectArray() {
		String str = "returnObjectArray";
		String[] strArray = new String[] {"returnObjectArray","returnObject"};
		Mockito.doAnswer(new Answer<String[]>() {
			@Override
			public String[] answer(InvocationOnMock invocation) throws Throwable {
				MethodStatement ms = invocation.getArgument(0);
				assertEquals(ms.getMethod(), str);
				assertNull(invocation.getArgument(1));
				invocation.getMock();
				return strArray;
			}
		}).when(asm).invoke(Mockito.any(), Mockito.any());
		assertEquals(remote.returnObjectArray(), strArray);
	}
	
	
	@Test
	public void parameterInt() {
		String str = "parameterInt";
		int i = random.nextInt();
		Mockito.doAnswer(new Answer<Void>() {
			
			public Void answer(InvocationOnMock invocation) throws Throwable {
				MethodStatement ms = invocation.getArgument(0);
				assertEquals(ms.getMethod(), str);
				Object[] object = invocation.getArgument(1);
				assertEquals( object[0] , i);
				invocation.getMock();
				return null;
			}
		}).when(asm).doInvoke(Mockito.any(MethodStatement.class), Mockito.any(Object[].class));
		remote.parameterInt(i);
	}
	
	@Test
	public void parameterIntArray() {
		String str = "parameterIntArray";
		int[] i = new int[] {random.nextInt() , random.nextInt()};
		Mockito.doAnswer(new Answer<Void>() {
			
			public Void answer(InvocationOnMock invocation) throws Throwable {
				MethodStatement ms = invocation.getArgument(0);
				assertEquals(ms.getMethod(), str);
				Object[] object = invocation.getArgument(1);
				assertEquals( object[0] , i);
				invocation.getMock();
				return null;
			}
		}).when(asm).doInvoke(Mockito.any(), Mockito.any());
		remote.parameterIntArray(i);
	}
	
	@Test
	public void parameterLong() {
		String str = "parameterLong";
		long i = random.nextLong();
		Mockito.doAnswer(new Answer<Void>() {
			
			public Void answer(InvocationOnMock invocation) throws Throwable {
				MethodStatement ms = invocation.getArgument(0);
				assertEquals(ms.getMethod(), str);
				Object[] object = invocation.getArgument(1);
				assertEquals( object[0] , i);
				invocation.getMock();
				return null;
			}
		}).when(asm).doInvoke(Mockito.any(), Mockito.any());
		remote.parameterLong(i);
	}
	
	@Test
	public void parameterLongArray() {
		String str = "parameterIntArray";
		long[] i = new long[] {random.nextLong() , random.nextLong()};
		Mockito.doAnswer(new Answer<Void>() {
			
			public Void answer(InvocationOnMock invocation) throws Throwable {
				MethodStatement ms = invocation.getArgument(0);
				assertEquals(ms.getMethod(), str);
				Object[] object = invocation.getArgument(1);
				assertEquals( object[0] , i);
				invocation.getMock();
				return null;
			}
		}).when(asm).doInvoke(Mockito.any(), Mockito.any());
		remote.parameterLongArray(i);
	}
	
	@Test
	public void parameterObject() {
		String str = "parameterObject";
		String value = UUID.randomUUID().toString();
		Mockito.doAnswer(new Answer<Void>() {
			
			public Void answer(InvocationOnMock invocation) throws Throwable {
				MethodStatement ms = invocation.getArgument(0);
				assertEquals(ms.getMethod(), str);
				Object[] object = invocation.getArgument(1);
				assertEquals( object[0] , value);
				invocation.getMock();
				return null;
			}
		}).when(asm).doInvoke(Mockito.any(), Mockito.any());
		remote.parameterObject(value);
	}

	@Test
	public void parameterObjectArray() {
		String str = "parameterObjectArray";
		String[] value = new String[] {UUID.randomUUID().toString() , UUID.randomUUID().toString()};
		Mockito.doAnswer(new Answer<Void>() {
			
			public Void answer(InvocationOnMock invocation) throws Throwable {
				MethodStatement ms = invocation.getArgument(0);
				assertEquals(ms.getMethod(), str);
				Object[] object = invocation.getArgument(1);
				assertEquals( object[0] , value);
				invocation.getMock();
				return null;
			}
		}).when(asm).doInvoke(Mockito.any(), Mockito.any());
		remote.parameterObjectArray(value);
	}
	
	@Test
	public void execteIntLong() {
		String execte = "execteIntLong";
		int intValue = random.nextInt();
		int intValueTwo = random.nextInt();
		Mockito.doAnswer(new Answer<String>() {
			
			public String answer(InvocationOnMock invocation) throws Throwable {
				Object[] object = invocation.getArgument(1);
				assertEquals(object[0] , intValue);
				assertEquals(object[1] , intValueTwo);
				invocation.getMock();
				return execte;
			}
		}).when(asm).doInvoke(Mockito.any(), Mockito.any());
		assertEquals(remote.execte(intValue ,intValueTwo) , execte);
	}
	
	@Test
	public void execteStringList() {
		String execte = "execteStringList";
		String value = UUID.randomUUID().toString();
		List<String> valueList = new ArrayList<>();
		Mockito.doAnswer(new Answer<String>() {
			
			public String answer(InvocationOnMock invocation) throws Throwable {
				Object[] object = invocation.getArgument(1);
				assertEquals(object[0] , value);
				assertEquals(object[1] , valueList);
				invocation.getMock();
				return execte;
			}
		}).when(asm).doInvoke(Mockito.any(), Mockito.any());
		assertEquals(remote.execte(value ,valueList) , execte);
	}
	
	@Test
	public void execteIntLongs() {
		String execte = "execteIntLongs";
		int intValue = random.nextInt();
		long longValue = random.nextLong();
		Mockito.doAnswer(new Answer<String>() {
			public String answer(InvocationOnMock invocation) throws Throwable {
				Object[] object = invocation.getArgument(1);
				assertEquals(object[0] , intValue);
				assertEquals(object[1] , longValue);
				invocation.getMock();
				return execte;
			}
		}).when(asm).doInvoke(Mockito.any(), Mockito.any());
		assertEquals(remote.execte(intValue ,longValue) , execte);
	}
	
	@Test
	public void execteLongLongLong() {
		String execte = "execteLongLongLong";
		long oneValue = random.nextLong();
		long longValue = random.nextLong();
		long  threeValue = random.nextLong();
		Mockito.doAnswer(new Answer<String>() {
			public String answer(InvocationOnMock invocation) throws Throwable {
				Object[] object = invocation.getArgument(1);
				assertEquals(object[0] , oneValue);
				assertEquals(object[1] , longValue);
				assertEquals(object[2] , threeValue);
				invocation.getMock();
				return execte;
			}
		}).when(asm).doInvoke(Mockito.any(), Mockito.any());
		assertEquals(remote.execte(oneValue ,longValue,threeValue) , execte);
	}
	
	@Test
	public void execteIntLongIntegerLong() {
		String execte = "execteLongLongLong";
		int intValue = random.nextInt();
		long longValue = random.nextLong();
		Integer threeValue = Integer.valueOf(random.nextInt());
		Long  frouValue = Long.valueOf(random.nextLong());
		Mockito.doAnswer(new Answer<String>() {
			public String answer(InvocationOnMock invocation) throws Throwable {
				Object[] object = invocation.getArgument(1);
				assertEquals(object[0] , intValue);
				assertEquals(object[1] , longValue);
				assertEquals(object[2] , threeValue);
				invocation.getMock();
				return execte;
			}
		}).when(asm).doInvoke(Mockito.any(), Mockito.any());
		assertEquals(remote.execte(intValue ,longValue,threeValue,frouValue) , execte);
	}
	
	@Test
	public void execteIntLongIntegerLongString() {
		String execte = "execteIntLongIntegerLongString";
		int intValue = random.nextInt();
		long longValue = random.nextLong();
		Integer threeValue = Integer.valueOf(random.nextInt());
		Long  frouValue = Long.valueOf(random.nextLong());
		String five = UUID.randomUUID().toString();
		Mockito.doAnswer(new Answer<String>() {
			public String answer(InvocationOnMock invocation) throws Throwable {
				Object[] object = invocation.getArgument(1);
				assertEquals(object[0] , intValue);
				assertEquals(object[1] , longValue);
				assertEquals(object[2] , threeValue);
				assertEquals(object[3] , frouValue);
				assertEquals(object[4] , five);
				invocation.getMock();
				return execte;
			}
		}).when(asm).doInvoke(Mockito.any(), Mockito.any());
		assertEquals(remote.execte(intValue ,longValue,threeValue,frouValue,five) , execte);
	}
	
	@Test
	public void execteIntLongStringListMap() {
		String execte = "execteIntLongStringListMap";
		int intValue = random.nextInt();
		long longValue = random.nextLong();
		String threeValue = UUID.randomUUID().toString();
		List<String>  frouValue = new ArrayList<String>();
		Map<String,String> five = new HashMap<String, String>();
		Mockito.doAnswer(new Answer<String>() {
			public String answer(InvocationOnMock invocation) throws Throwable {
				Object[] object = invocation.getArgument(1);
				assertEquals(object[0] , intValue);
				assertEquals(object[1] , longValue);
				assertEquals(object[2] , threeValue);
				assertEquals(object[3] , frouValue);
				assertEquals(object[4] , five);
				invocation.getMock();
				return execte;
			}
		}).when(asm).doInvoke(Mockito.any(), Mockito.any());
		assertEquals(remote.execte(intValue ,longValue,threeValue,frouValue,five) , execte);
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
				.thenReturn("test").thenReturn(new int[] { 1 }).thenReturn(new long[] { 2L });

		TestAsmProxy tap = new TestAsmProxy(mockInvoker);
		Map<String, MethodExecute<?>> mehtodMap = reflect.getInvoke(tap, tap.getClass());

		Iterator<Entry<String, MethodExecute<?>>> it = mehtodMap.entrySet().iterator();

		while (it.hasNext()) {
			Object object = it.next().getValue().execute(null);
			if (object == null) {
				System.out.println("null");
			} else {
				System.out.println(object);
			}

		}

	}
}
