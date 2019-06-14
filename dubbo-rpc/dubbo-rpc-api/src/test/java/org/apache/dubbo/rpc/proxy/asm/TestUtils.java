package org.apache.dubbo.rpc.proxy.asm;

import java.lang.reflect.InvocationTargetException;
import java.util.Random;
import java.util.UUID;

public class TestUtils {

	static Random random = new Random();
	
	public static Object getValue(Class<?> clazz) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		if(void.class.equals(clazz)){
			return null;
		} else if(boolean.class.equals(clazz) || Boolean.class.equals(clazz)) {
			return true;
		}else if(char.class.equals(clazz) || Character.class.equals(clazz)) {
			return (char)random.nextInt(128);
		}else if(byte.class.equals(clazz) || Byte.class.equals(clazz)) {
			return (byte)random.nextInt(128);
		}else if(short.class.equals(clazz) || Short.class.equals(clazz)) {
			return (short)random.nextInt();
		}else if(int.class.equals(clazz) || Integer.class.equals(clazz)) {
			return random.nextInt();
		}else if(long.class.equals(clazz) || Long.class.equals(clazz)) {
			return random.nextLong();
		}else if(float.class.equals(clazz) || Float.class.equals(clazz)) {
			return random.nextFloat();
		}else if(double.class.equals(clazz) || Double.class.equals(clazz)) {
			return random.nextDouble();
			
		}else if( String.class.equals(clazz)) {
			return UUID.randomUUID().toString();
		}else if(clazz.isArray()) {
			if(boolean[].class.equals(clazz)) {
				return new boolean[] {true};
			}else if(char[].class.equals(clazz)) {
				return new char[] {(char)random.nextInt(128)};
			}else if(byte[].class.equals(clazz)) {
				return new byte[] {(byte)random.nextInt(128)};
			}else if(short[].class.equals(clazz)) {
				return new short[] {(short)random.nextInt()};
			}else if(int[].class.equals(clazz) ) {
				return new int[] {random.nextInt()};
			}else if(long[].class.equals(clazz)) {
				return new long[] {random.nextLong()};
			}else if(float[].class.equals(clazz)) {
				return new float[] {random.nextFloat()};
			}else if(double[].class.equals(clazz)) {
				return new double[] {random.nextDouble()};
				
			}
			return new Object[0];
		}else {
			return clazz.getConstructor(new Class[] {}).newInstance();
		}
	}
	
	public static Object[] getParmameter(Class<?>[] clazzArray) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		if(clazzArray == null || clazzArray.length == 0) {
			return null;
		}
		Object[] object = new Object[clazzArray.length];
		int i = 0;
		for(Class<?> clazz : clazzArray) {
			object[i++] = getValue(clazz);
		}
		return object;
	}

}
