package com.alibaba.dubbo.common.serialize.serialization.proto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.alibaba.dubbo.common.serialize.support.hessian.Hessian2ObjectInput;
import com.alibaba.dubbo.common.serialize.support.hessian.Hessian2ObjectOutput;
import com.alibaba.dubbo.common.serialize.support.java.JavaObjectInput;
import com.alibaba.dubbo.common.serialize.support.java.JavaObjectOutput;
import com.alibaba.dubbo.common.serialize.support.json.FastJsonObjectInput;
import com.alibaba.dubbo.common.serialize.support.json.FastJsonObjectOutput;
import com.alibaba.dubbo.common.serialize.support.kryo.KryoObjectInput;
import com.alibaba.dubbo.common.serialize.support.kryo.KryoObjectOutput;
import com.alibaba.dubbo.common.serialize.support.proto.ProtoObjectInput;
import com.alibaba.dubbo.common.serialize.support.proto.ProtoObjectOutput;

/**
 * 为了进行基准测试，基本把已有的几个序列化工具都开了个setOutputStream的口来做每次流的重置。
 * @author surlymo
 *
 */
public class SerializationBenchmarkTest {

	private static double top = 99999999;
	private static String topName;
	private static Map<String, Integer> map = new HashMap<String, Integer>();
	
	private static double fastjsonCnt = 0;
	private static double javaCnt = 0;
	private static double pbCnt = 0;
	private static double kryoCnt = 0;
	private static double h2Cnt = 0;
	
	static enum ObjType{
		SMALL,
		NORMAL,
		BIG
	}

	public static void main(String[] args) throws Exception {

		test(ObjType.SMALL, 100);
		test(ObjType.SMALL, 200);
		test(ObjType.SMALL, 500);
		test(ObjType.SMALL, 1000);
		test(ObjType.SMALL, 1500);
		test(ObjType.SMALL, 2000);
		test(ObjType.SMALL, 5000);
		test(ObjType.SMALL, 10000);
		
		report();
		
		test(ObjType.NORMAL, 100);
		test(ObjType.NORMAL, 200);
		test(ObjType.NORMAL, 500);
		test(ObjType.NORMAL, 1000);
		test(ObjType.NORMAL, 1500);
		test(ObjType.NORMAL, 2000);
		test(ObjType.NORMAL, 5000);
		test(ObjType.NORMAL, 10000);
		
		report();
		
		test(ObjType.BIG, 100);
		test(ObjType.BIG, 200);
		test(ObjType.BIG, 500);
		test(ObjType.BIG, 1000);
		test(ObjType.BIG, 1500);
		test(ObjType.BIG, 2000);
		test(ObjType.BIG, 5000);
		test(ObjType.BIG, 10000);

		report();
	}
	
	private static void report(){

		for (Entry<String, Integer> entry : map.entrySet()) {
			System.out.println("key: " + entry.getKey() + ", value: "
					+ entry.getValue());

		}
		
		System.out.println("fastjson: " + fastjsonCnt);
		System.out.println("java: " + javaCnt);
		System.out.println("pb: " + pbCnt);
		System.out.println("kryo： " + kryoCnt);
		System.out.println("h2: " + h2Cnt);
		
		map.clear();
		fastjsonCnt = 0;
		javaCnt = 0;
		pbCnt = 0;
		kryoCnt = 0;
		h2Cnt = 0;
		
	}

	private static void test(ObjType type, int loop) throws Exception {
		System.out.println("******* test Begin *********");
		test_pb(type, loop);
		test_kryo(type, loop);
		test_h2(type, loop);
		test_java(type, loop);
		test_fastjson(type, loop);

		System.out.println("top name is: " + topName + ", and top cost is: "
				+ top);

		int cnt = 0;
		if (map.get(topName) != null) {
			cnt = map.get(topName);
		}
		map.put(topName, ++cnt);
		topName = "";
		top = 9999999;

		System.out.println("******* test End *********\n\n\n");
	}

	private static void test_fastjson(ObjType type, int loop) throws Exception {
		System.out.println("================Fastjson Begin(objCnt: " + type
				+ ", loopCnt: " + loop + ")===============");

		TestObj lists = createInputObject(type);

		ByteArrayOutputStream outputStream3 = new ByteArrayOutputStream();
		FastJsonObjectOutput javaOutput = new FastJsonObjectOutput(
				outputStream3);

		javaOutput.writeObject(lists);
		javaOutput.flushBuffer();
		new FastJsonObjectInput(new ByteArrayInputStream(
				outputStream3.toByteArray())).readObject(TestObj.class);

		System.out.print("[FASTJSON]size: " + outputStream3.size());
		long heBegin = System.nanoTime();

		for (int i = 0; i < loop; i++) {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//			javaOutput.setOutputStream(outputStream);
			javaOutput.writeObject(lists);
			javaOutput.flushBuffer();
			FastJsonObjectInput in = new FastJsonObjectInput(
					new ByteArrayInputStream(outputStream.toByteArray()));
			in.readObject(TestObj.class);
		}

		long heCost = System.nanoTime() - heBegin;
		System.out.println(" with the time: " + (double) heCost / 1000000
				+ "ms");
		double perCost = (double) heCost / (1000000 * loop);
		System.out.println("[FASTJSON]per seril cost: " + perCost + "ms");
		if (perCost < top) {
			top = perCost;
			topName = "fastjson";
		}
		fastjsonCnt += perCost;
	}

	private static void test_java(ObjType type, int loop) throws Exception {
		System.out.println("================Java Begin(objCnt: " + type
				+ ", loopCnt: " + loop + ")===============");

		TestObj lists = createInputObject(type);

		ByteArrayOutputStream outputStream3 = new ByteArrayOutputStream();
		JavaObjectOutput javaOutput = new JavaObjectOutput(outputStream3);

		javaOutput.writeObject(lists);
		javaOutput.flushBuffer();
		new JavaObjectInput(new ByteArrayInputStream(
				outputStream3.toByteArray())).readObject(TestObj.class);

		System.out.print("[JAVA]size: " + outputStream3.size());
		long heBegin = System.nanoTime();

		for (int i = 0; i < loop; i++) {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//			javaOutput.setOutputStream(outputStream);
			javaOutput.writeObject(lists);
			javaOutput.flushBuffer();
			JavaObjectInput in = new JavaObjectInput(new ByteArrayInputStream(
					outputStream.toByteArray()));
			TestObj obj = in.readObject(TestObj.class);
		}
		long heCost = System.nanoTime() - heBegin;
		System.out.println(" with the time: " + (double) heCost / 1000000
				+ "ms");
		double perCost = (double) heCost / (1000000 * loop);
		System.out.println("[JAVA]per seril cost: " + perCost + "ms");
		if (perCost < top) {
			top = perCost;
			topName = "java";
		}
		javaCnt += perCost;
	}

	private static void test_pb(ObjType type, int loop) throws Exception {
		System.out.println("================Proto Begin(objCnt: " + type
				+ ", loopCnt: " + loop + ")===============");

		TestObj lists = createInputObject(type);

		ByteArrayOutputStream outputStream3 = new ByteArrayOutputStream();
		ProtoObjectOutput pbOutput = new ProtoObjectOutput(outputStream3);

		pbOutput.writeObject(lists);
		pbOutput.flushBuffer();
		new ProtoObjectInput(new ByteArrayInputStream(
				outputStream3.toByteArray())).readObject(TestObj.class);
		outputStream3.flush();
		System.out.print("[PROTO]size: " + outputStream3.size());
		long heBegin = System.nanoTime();

		for (int i = 0; i < loop; i++) {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//			pbOutput.setOutputStream(outputStream);
			pbOutput.writeObject(lists);
			pbOutput.flushBuffer();
			ProtoObjectInput in = new ProtoObjectInput(
					new ByteArrayInputStream(outputStream.toByteArray()));
			TestObj obj = in.readObject(TestObj.class);
		}
		long heCost = System.nanoTime() - heBegin;
		System.out.println(" with the time: " + (double) heCost / 1000000
				+ "ms");

		double perCost = (double) heCost / (1000000 * loop);
		System.out.println("[PROTO]per seril cost: " + perCost + "ms");

		if (perCost < top) {
			top = perCost;
			topName = "pb";
		}
		pbCnt += perCost;
	}

	private static void test_kryo(ObjType type, int loop) throws Exception {
		System.out.println("================Kryo Begin(objCnt: " + type
				+ ", loopCnt: " + loop + ")===============");

		TestObj lists = createInputObject(type);

		ByteArrayOutputStream outputStream3 = new ByteArrayOutputStream();
		KryoObjectOutput kryoOutput = new KryoObjectOutput(outputStream3);

		kryoOutput.writeObject(lists);
		kryoOutput.flushBuffer();
		new KryoObjectInput(new ByteArrayInputStream(
				outputStream3.toByteArray())).readObject(TestObj.class);
		System.out.print("[KRYO]size: " + outputStream3.size());
		long heBegin = System.nanoTime();

		for (int i = 0; i < loop; i++) {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//			kryoOutput.setOutputStream(outputStream);
			kryoOutput.writeObject(lists);
			kryoOutput.flushBuffer();
			KryoObjectInput in = new KryoObjectInput(new ByteArrayInputStream(
					outputStream.toByteArray()));
			in.readObject(TestObj.class);
		}
		// Thread.sleep(1000L);
		long heCost = System.nanoTime() - heBegin;
		System.out.println(" with the time: " + (double) heCost / 1000000
				+ "ms");

		double perCost = (double) heCost / (1000000 * loop);
		System.out.println("[KRYO]per seril cost: " + perCost + "ms");
		if (perCost < top) {
			top = perCost;
			topName = "kryo";
		}
		kryoCnt += perCost;

	}

	private static void test_h2(ObjType type, int loop) throws Exception {
		System.out.println("================Hessian2 Begin(objCnt: " + type
				+ ", loopCnt: " + loop + ")===============");

		TestObj lists = createInputObject(type);

		ByteArrayOutputStream outputStream3 = new ByteArrayOutputStream();
		Hessian2ObjectOutput hessianOutput = new Hessian2ObjectOutput(
				outputStream3);

		hessianOutput.writeObject(lists);
		hessianOutput.flushBuffer();
		new Hessian2ObjectInput(new ByteArrayInputStream(
				outputStream3.toByteArray())).readObject(TestObj.class);
		System.out.print("[HESSIAN2]size: " + outputStream3.size());
		long heBegin = System.nanoTime();

		for (int i = 0; i < loop; i++) {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			hessianOutput = new Hessian2ObjectOutput(outputStream);
			hessianOutput.writeObject(lists);
			hessianOutput.flushBuffer();
			Hessian2ObjectInput in = new Hessian2ObjectInput(
					new ByteArrayInputStream(outputStream.toByteArray()));
			in.readObject();
		}
		// Thread.sleep(1000L);
		long heCost = System.nanoTime() - heBegin;
		System.out.println(" with the time: " + (double) heCost / 1000000
				+ "ms");
		double perCost = (double) heCost / (1000000 * loop);
		System.out.println("[HESSIAN2]per seril cost: " + perCost + "ms");

		if (perCost < top) {
			top = perCost;
			topName = "h2";
		}
		h2Cnt += perCost;
	}

	private static TestObj createInputObject(ObjType type) {
		TestObj obj = new TestObj();
		if(type.equals(ObjType.SMALL)){
			fillSmall(obj, type);
		}
		
		if(type.equals(ObjType.NORMAL)){
			System.out.println("in normal process..");
			fillNormal(obj, type);
		}	
		
		if(type.equals(ObjType.BIG)){
			fillBig(obj, type);
		}
		
		System.out.println("[INPUT]per Obj " + obj.toString().getBytes().length
				+ "bytes");
		return obj;
	}
	
	private static void fillSmall(TestObj obj, ObjType type){
		obj.setName("zhuzhu");
		obj.setId(1231231);
		obj.setHelloworldandhellokitty(3);
		obj.setHouehousehousehouse("helloworld");
		obj.setTakeiteasy("xxxxxx");
		obj.setTakemeawayawayaway(3);
		obj.setWatruthinkingabt(3);
		obj.setWatruthinkingabt2(66);
	}
	
	private static void fillNormal(TestObj obj, ObjType type){
		
		fillSmall(obj, type);
		
		obj.setHelloworldandhellokitty2(3);
		obj.setHouehousehousehouse2("helloworld");
		obj.setTakeiteasy2("xxxxxx");
		obj.setTakemeawayawayaway2(3);
		obj.setWatruthinkingabt3(3);
		obj.setWatruthinkingabt4(66);
	}
	
	private static void fillBig(TestObj obj, ObjType type){
		
		fillNormal(obj, type);
		
		obj.setBigDataAndValueIsBigger1("hello");
		obj.setBigDataAndValueIsBigger2("hello");
		obj.setBigDataAndValueIsBigger3("hello");
		obj.setBigDataAndValueIsBigger4("hello");
		obj.setBigDataAndValueIsBigger5("hello");
		obj.setBigDataAndValueIsBigger6("hello");
		obj.setBigDataAndValueIsBigger7("hello");
		obj.setBigDataAndValueIsBigger8("hello");
		obj.setBigDataAndValueIsBigger9("hello");
		obj.setBigDataAndValueIsBigger10("hello");
		obj.setBigDataAndValueIsBigger11("hello");
		obj.setBigDataAndValueIsBigger12("hello");
	}
}
