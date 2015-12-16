package com.alibaba.dubbo.common.serialize.serialization.proto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.alibaba.dubbo.common.serialize.support.proto.ProtoObjectInput;
import com.alibaba.dubbo.common.serialize.support.proto.ProtoObjectOutput;

public class ProtoerializationTest {
	@Test
	public void test_list() throws Exception{
		A a = new A();
		a.setName("hello");
		a.setValue(8);
		List<A> lists = new ArrayList<A>();
		lists.add(a);
		
		ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
		ProtoObjectOutput protoOutput = new ProtoObjectOutput(outputStream2);

		protoOutput.writeObject(lists);
		protoOutput.flushBuffer();
		
		for(A tmp : lists){
			System.out.println(tmp);
		}
	}
	
	@Test
	public void test_genericity() throws Exception{
		final B b = new B();
		b.setName("hello");
		b.setValue(8);
		
		C<B> c = new C<B>();
		c.setName("iam c");
		c.setValue(777);
		c.setLists(new ArrayList<B>(){
			{
				add(b);
			}
		});
		
		
		ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
		ProtoObjectOutput protoOutput = new ProtoObjectOutput(outputStream2);

		protoOutput.writeObject(c);
		protoOutput.flushBuffer();
		
		
		ByteArrayInputStream input = new ByteArrayInputStream(outputStream2.toByteArray());
		ProtoObjectInput pIn = new ProtoObjectInput(input);
		C<B> readC = (C)pIn.readObject();
		System.out.println(readC);
	}
	
}
