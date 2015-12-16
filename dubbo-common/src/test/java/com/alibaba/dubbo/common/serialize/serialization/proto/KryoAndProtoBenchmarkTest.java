package com.alibaba.dubbo.common.serialize.serialization.proto;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.dubbo.common.serialize.support.kryo.KryoObjectOutput;
import com.alibaba.dubbo.common.serialize.support.proto.ProtoObjectOutput;

public class KryoAndProtoBenchmarkTest {

	public static void main(String[] args) throws Exception {

		testIt(1, 10);
		testIt(5, 10);
		testIt(10, 10);
		testIt(100, 10);
		testIt(1000, 10);
		testIt(3000, 10);

		System.out.println("\n\n\n****grow up the concurrency****\n\n\n");

		testIt(1, 100);
		testIt(5, 100);
		testIt(10, 100);
		testIt(100, 100);
		testIt(1000, 100);
		testIt(3000, 100);
		
		System.out.println("\n\n\n****grow up the concurrency****\n\n\n");
		
		testIt(1, 1000);
		testIt(5, 1000);
		testIt(10, 1000);
		testIt(100, 1000);
		testIt(1000, 1000);
		testIt(3000, 1000);
	}

	private static void testIt(int objCnt, int loop) throws Exception {
		System.out.println("================test Begin(objCnt: " + objCnt
				+ ", loopCnt: " + loop + ")===============");

		TestObj lists = createInputObject(objCnt);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		KryoObjectOutput kryoOutput = new KryoObjectOutput(outputStream);

		kryoOutput.writeObject(lists);
		kryoOutput.flushBuffer();
		System.out.print("[Proto]size: " + outputStream.size());
		long kryoBegin = System.currentTimeMillis();
		for (int i = 0; i < loop; i++) {
			kryoOutput.writeObject(lists);
			kryoOutput.flushBuffer();
		}

		long kryoCost = System.currentTimeMillis() - kryoBegin;
		System.out.println(" with the time: " + kryoCost + "ms");
		System.out.println("[KRYO]per seril cost: " + (double) kryoCost / loop
				+ "ms");

		ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
		ProtoObjectOutput protoOutput = new ProtoObjectOutput(outputStream2);

		protoOutput.writeObject(lists);
		protoOutput.flushBuffer();
		System.out.print("[Proto]size: " + outputStream2.size());
		long protoBegin = System.currentTimeMillis();
		for (int i = 0; i < loop; i++) {
			protoOutput.writeObject(lists);
			protoOutput.flushBuffer();
		}
		long protoCost = System.currentTimeMillis() - protoBegin;
		System.out.println(" with the time: " + protoCost + "ms");
		System.out.println("[Proto]per seril cost: " + (double) protoCost
				/ loop + "ms");

		System.out.println("[Compare] proto faster: "
				+ (double) (kryoCost - protoCost) / kryoCost);

		System.out.println("================test End===============\n\n");

	}

	private static TestObj createInputObject(int num) {
		TestObj obj = new TestObj();
		obj.setName("zhuzhu");
		obj.setId(1231231);
		List<ListEnum> tmps = new ArrayList<ListEnum>();
		obj.setLists(tmps);
		for (int i = 0; i < num; i++) {
			ListEnum temp = new ListEnum();
			temp.setSmallId(i);
			temp.setSmallName("small_" + i);
			tmps.add(temp);
		}
		System.out.println("[INPUT]per Obj " + obj.toString().getBytes().length
				+ "bytes");
		return obj;
	}
}
