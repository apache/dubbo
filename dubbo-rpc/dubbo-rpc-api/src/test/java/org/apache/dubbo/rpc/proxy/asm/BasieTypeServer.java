package org.apache.dubbo.rpc.proxy.asm;

public interface BasieTypeServer {

	public void returnVoid();
	
	public boolean returnBoolean();
	
	public char returnChar();
	
	public byte returnByte();
	
	public short returnShort();
	
	public int returnInt();
	
	public long returnLong();
	
	public float returnFloat();
	
	public double returnDouble();
	
	public int[] returnIntArray();
	
	public long[] returnLongArray();
	
	public Object returnObject();
	
	public Object[] returnObjectArray();
	
	public void parameterBoolean(boolean boo);
	
	public void parameterChar(char ch);
	
	public void parameterByte(byte by);
	
	public void parameterShort(short sh);
	
	public void parameterInt(int in);
	
	public void parameterLong(long lo);
	
	public void parameterFloat(float fl);
	
	public void parameterDouble(double dou);
	
	public void parameterIntArray(int[] intArray);
	
	public void parameterLongArray(long[] longArray);
	
	public void parameterObject(Object object);
	
	public void parameterObjectArray(Object[] objectArray);
	
	
	public boolean parameterAndReturnBoolean(boolean boo);
	
	public char parameterAndReturnChar(char ch);
	
	public byte parameterAndReturnByte( byte by);
	
	public short parameterAndReturnShort(short sh);
	
	public int parameterAndReturnInt(int in);
	
	public long parameterAndReturnLong(long lo);
	
	public float parameterAndReturnFloat(float fl);
	
	public double parameterAndReturnDouble(double dou);
	
	public int[] parameterAndReturnIntArray(int[] array);
	
	public long[] parameterAndReturnLongArray(long[] array);
	
	public Object parameterAndReturnObject(Object o);
	
	public Object[] parameterAndReturnObjectArray(Object[] array);
}
