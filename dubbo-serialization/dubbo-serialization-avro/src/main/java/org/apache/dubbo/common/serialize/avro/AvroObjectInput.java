package org.apache.dubbo.common.serialize.avro;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.util.Utf8;
import org.apache.dubbo.common.serialize.ObjectInput;

public class AvroObjectInput implements ObjectInput{
	private DecoderFactory decoderFactory=DecoderFactory.get();;
	private BinaryDecoder decoder;
	
	public AvroObjectInput(InputStream in){
		decoder=decoderFactory.binaryDecoder(in, null);
	}
	
	@Override
	public boolean readBool() throws IOException {
		return decoder.readBoolean();
	}

	@Override
	public byte readByte() throws IOException {
		byte[] bytes=new byte[1];
		decoder.readFixed(bytes);
		return bytes[0];
	}

	@Override
	public short readShort() throws IOException {
		return (short) decoder.readInt();
	}

	@Override
	public int readInt() throws IOException {
		return decoder.readInt();
	}

	@Override
	public long readLong() throws IOException {
		return decoder.readLong();
	}

	@Override
	public float readFloat() throws IOException {
		return decoder.readFloat();
	}

	@Override
	public double readDouble() throws IOException {
		return decoder.readDouble();
	}

	@Override
	public String readUTF() throws IOException {
		Utf8 result= new Utf8();
		result=decoder.readString(result);
		return result.toString();
	}

	@Override
	public byte[] readBytes() throws IOException {
		String resultStr = decoder.readString();
		return resultStr.getBytes("utf8");
	}
	
	/**
	 * will lost all attribute
	 */
	@Override
	public Object readObject() throws IOException, ClassNotFoundException {
		ReflectDatumReader<Object> reader = new ReflectDatumReader<>(Object.class);
		return reader.read(null, decoder);
	}

	@Override
	@SuppressWarnings(value={"unchecked"})
	public <T> T readObject(Class<T> cls) throws IOException, ClassNotFoundException {
		//Map interface class change to HashMap implement
		if(cls==Map.class){
			cls=(Class<T>) HashMap.class;
		}
		
		ReflectDatumReader<T> reader = new ReflectDatumReader<>(cls);
		return reader.read(null, decoder);
	}

	@Override
	public <T> T readObject(Class<T> cls, Type type) throws IOException, ClassNotFoundException {
		ReflectDatumReader<T> reader = new ReflectDatumReader<>(cls);
		return reader.read(null, decoder);
	}

}
