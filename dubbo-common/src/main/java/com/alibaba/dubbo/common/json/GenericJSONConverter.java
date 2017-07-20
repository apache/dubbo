/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common.json;

import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.dubbo.common.bytecode.Wrapper;
import com.alibaba.dubbo.common.io.Bytes;

public class GenericJSONConverter implements JSONConverter
{
	private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	
	protected interface Encoder{ void encode(Object obj, JSONWriter jb) throws IOException; }

	protected interface Decoder{ Object decode(Object jv) throws IOException; }

	private static final Map<Class<?>, Encoder> GlobalEncoderMap = new HashMap<Class<?>, Encoder>();

	private static final Map<Class<?>, Decoder> GlobalDecoderMap = new HashMap<Class<?>, Decoder>();

	@SuppressWarnings("unchecked")
	public void writeValue(Object obj, JSONWriter jb, boolean writeClass) throws IOException
	{
		if (obj == null) {
			jb.valueNull();
			return;
		}
		Class<?> c = obj.getClass();
		Encoder encoder = GlobalEncoderMap.get(c);

		if( encoder != null )
		{
			encoder.encode(obj, jb);
		}
		else if( obj instanceof JSONNode )
		{
			((JSONNode)obj).writeJSON(this, jb, writeClass);
		}
		else if( c.isEnum() )
		{
			jb.valueString(((Enum<?>)obj).name());
		}
		else if( c.isArray() )
		{
			int len = Array.getLength(obj);
			jb.arrayBegin();
			for(int i=0;i<len;i++)
				writeValue(Array.get(obj, i), jb, writeClass);
			jb.arrayEnd();
		}
		else if( Map.class.isAssignableFrom(c) )
		{
			Object key, value;
			jb.objectBegin();
			for( Map.Entry<Object, Object> entry : ((Map<Object, Object>)obj).entrySet() )
			{
				key = entry.getKey();
				if( key == null )
					continue;
				jb.objectItem(key.toString());

				value = entry.getValue();
				if( value == null )
					jb.valueNull();
				else
					writeValue(value, jb, writeClass);
			}
			jb.objectEnd();
		}
		else if( Collection.class.isAssignableFrom(c) )
		{
			jb.arrayBegin();
			for( Object item : (Collection<Object>)obj )
			{
				if( item == null )
					jb.valueNull();
				else
					writeValue(item, jb, writeClass);
			}
			jb.arrayEnd();
		}
		else
		{
			jb.objectBegin();
			
			Wrapper w = Wrapper.getWrapper(c);
			String pns[] = w.getPropertyNames();

			for( String pn : pns )
			{
				if ((obj instanceof Throwable) && (
						"localizedMessage".equals(pn) 
						|| "cause".equals(pn)
						|| "suppressed".equals(pn)
						|| "stackTrace".equals(pn))) {
					continue;
				}
				
				jb.objectItem(pn);

				Object value = w.getPropertyValue(obj,pn);
				if( value == null || value == obj)
					jb.valueNull();
				else
					writeValue(value, jb, writeClass);
			}
			if (writeClass) {
			    jb.objectItem(JSONVisitor.CLASS_PROPERTY);
			    writeValue(obj.getClass().getName(), jb, writeClass);
			}
			jb.objectEnd();
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object readValue(Class<?> c, Object jv) throws IOException
	{
		if (jv == null) {
			return null;
		}
		Decoder decoder = GlobalDecoderMap.get(c);
		if( decoder != null ) {
			return decoder.decode(jv);
		}
		if (c.isEnum()) {
			return Enum.valueOf((Class<Enum>)c, String.valueOf(jv));
		}
		return jv;
	}

	static
	{
		// init encoder map.
		Encoder e = new Encoder(){
			public void encode(Object obj, JSONWriter jb) throws IOException
			{
				jb.valueBoolean((Boolean)obj);
			}
		};
		GlobalEncoderMap.put(boolean.class, e);
		GlobalEncoderMap.put(Boolean.class, e);

		e = new Encoder(){
			public void encode(Object obj, JSONWriter jb) throws IOException
			{
				jb.valueInt(((Number)obj).intValue());
			}
		};
		GlobalEncoderMap.put(int.class, e);
		GlobalEncoderMap.put(Integer.class, e);
		GlobalEncoderMap.put(short.class, e);
		GlobalEncoderMap.put(Short.class, e);
		GlobalEncoderMap.put(byte.class, e);
		GlobalEncoderMap.put(Byte.class, e);
		GlobalEncoderMap.put(AtomicInteger.class, e);
		
		e = new Encoder(){
			public void encode(Object obj, JSONWriter jb) throws IOException
			{
				jb.valueString(Character.toString((Character)obj));
			}
		};
		GlobalEncoderMap.put(char.class, e);
		GlobalEncoderMap.put(Character.class, e);
		
		e = new Encoder(){
			public void encode(Object obj, JSONWriter jb) throws IOException
			{
				jb.valueLong(((Number)obj).longValue());
			}
		};
		GlobalEncoderMap.put(long.class, e);
		GlobalEncoderMap.put(Long.class, e);
		GlobalEncoderMap.put(AtomicLong.class, e);
		GlobalEncoderMap.put(BigInteger.class, e);

		e = new Encoder(){
			public void encode(Object obj, JSONWriter jb) throws IOException
			{
				jb.valueFloat(((Number)obj).floatValue());
			}
		};
		GlobalEncoderMap.put(float.class, e);
		GlobalEncoderMap.put(Float.class, e);

		e = new Encoder(){
			public void encode(Object obj, JSONWriter jb) throws IOException
			{
				jb.valueDouble(((Number)obj).doubleValue());
			}
		};
		GlobalEncoderMap.put(double.class, e);
		GlobalEncoderMap.put(Double.class, e);
		GlobalEncoderMap.put(BigDecimal.class, e);

		e = new Encoder(){
			public void encode(Object obj, JSONWriter jb) throws IOException
			{
				jb.valueString(obj.toString());
			}
		};
		GlobalEncoderMap.put(String.class, e);
		GlobalEncoderMap.put(StringBuilder.class, e);
		GlobalEncoderMap.put(StringBuffer.class, e);

		e = new Encoder(){
			public void encode(Object obj, JSONWriter jb) throws IOException
			{
				jb.valueString(Bytes.bytes2base64((byte[])obj));
			}
		};
		GlobalEncoderMap.put(byte[].class, e);
		
		e = new Encoder(){
			public void encode(Object obj, JSONWriter jb) throws IOException
			{
				jb.valueString(new SimpleDateFormat(DATE_FORMAT).format((Date)obj));
			}
		};
		GlobalEncoderMap.put(Date.class, e);

		// init decoder map.
		Decoder d = new Decoder(){
			public Object decode(Object jv){ 
				return jv.toString(); 
			} 
		};
		GlobalDecoderMap.put(String.class, d);

		d = new Decoder(){
			public Object decode(Object jv)
			{
				if( jv instanceof Boolean ) return ((Boolean)jv).booleanValue();
				return false;
			}
		};
		GlobalDecoderMap.put(boolean.class, d);

		d = new Decoder(){
			public Object decode(Object jv)
			{
				if( jv instanceof Boolean ) return (Boolean)jv;
				return (Boolean)null;
			}
		};
		GlobalDecoderMap.put(Boolean.class, d);
		
		d = new Decoder(){
			public Object decode(Object jv)
			{
				if( jv instanceof String && ((String)jv).length() > 0) return ((String)jv).charAt(0);
				return (char)0;
			}
		};
		GlobalDecoderMap.put(char.class, d);
		
		d = new Decoder(){
			public Object decode(Object jv)
			{
				if( jv instanceof String && ((String)jv).length() > 0) return ((String)jv).charAt(0);
				return (Character)null;
			}
		};
		GlobalDecoderMap.put(Character.class, d);

		d = new Decoder(){
			public Object decode(Object jv)
			{
				if( jv instanceof Number ) return ((Number)jv).intValue();
				return 0;
			}
		};
		GlobalDecoderMap.put(int.class, d);

		d = new Decoder(){
			public Object decode(Object jv)
			{
				if( jv instanceof Number ) return Integer.valueOf(((Number)jv).intValue());
				return (Integer)null;
			}
		};
		GlobalDecoderMap.put(Integer.class, d);

		d = new Decoder(){
			public Object decode(Object jv)
			{
				if( jv instanceof Number ) return ((Number)jv).shortValue();
				return (short)0;
			}
		};
		GlobalDecoderMap.put(short.class, d);

		d = new Decoder(){
			public Object decode(Object jv)
			{
				if( jv instanceof Number ) return Short.valueOf(((Number)jv).shortValue());
				return (Short)null;
			}
		};
		GlobalDecoderMap.put(Short.class, d);

		d = new Decoder(){
			public Object decode(Object jv)
			{
				if( jv instanceof Number ) return ((Number)jv).longValue();
				return (long)0;
			}
		};
		GlobalDecoderMap.put(long.class, d);

		d = new Decoder(){
			public Object decode(Object jv)
			{
				if( jv instanceof Number ) return Long.valueOf(((Number)jv).longValue());
				return (Long)null;
			}
		};
		GlobalDecoderMap.put(Long.class, d);

		d = new Decoder(){
			public Object decode(Object jv)
			{
				if( jv instanceof Number ) return ((Number)jv).floatValue();
				return (float)0;
			}
		};
		GlobalDecoderMap.put(float.class, d);

		d = new Decoder(){
			public Object decode(Object jv)
			{
				if( jv instanceof Number ) return new Float(((Number)jv).floatValue());
				return (Float)null;
			}
		};
		GlobalDecoderMap.put(Float.class, d);

		d = new Decoder(){
			public Object decode(Object jv)
			{
				if( jv instanceof Number ) return ((Number)jv).doubleValue();
				return (double)0;
			}
		};
		GlobalDecoderMap.put(double.class, d);

		d = new Decoder(){
			public Object decode(Object jv)
			{
				if( jv instanceof Number ) return new Double(((Number)jv).doubleValue());
				return (Double)null;
			}
		};
		GlobalDecoderMap.put(Double.class, d);

		d = new Decoder(){
			public Object decode(Object jv)
			{
				if( jv instanceof Number ) return ((Number)jv).byteValue();
				return (byte)0;
			}
		};
		GlobalDecoderMap.put(byte.class, d);

		d = new Decoder(){
			public Object decode(Object jv)
			{
				if( jv instanceof Number ) return Byte.valueOf(((Number)jv).byteValue());
				return (Byte)null;
			}
		};
		GlobalDecoderMap.put(Byte.class, d);

		d = new Decoder(){
			public Object decode(Object jv) throws IOException
			{
				if( jv instanceof String ) return Bytes.base642bytes((String)jv);
				return (byte[])null;
			}
		};
		GlobalDecoderMap.put(byte[].class, d);

		d = new Decoder(){
			public Object decode(Object jv) throws IOException{ return new StringBuilder(jv.toString()); }
		};
		GlobalDecoderMap.put(StringBuilder.class, d);

		d = new Decoder(){
			public Object decode(Object jv) throws IOException{ return new StringBuffer(jv.toString()); }
		};
		GlobalDecoderMap.put(StringBuffer.class, d);

		d = new Decoder(){
			public Object decode(Object jv) throws IOException
			{
				if( jv instanceof Number ) return BigInteger.valueOf(((Number)jv).longValue());
				return (BigInteger)null;
			}
		};
		GlobalDecoderMap.put(BigInteger.class, d);

		d = new Decoder(){
			public Object decode(Object jv) throws IOException
			{
				if( jv instanceof Number ) return BigDecimal.valueOf(((Number)jv).doubleValue());
				return (BigDecimal)null;
			}
		};
		GlobalDecoderMap.put(BigDecimal.class, d);

		d = new Decoder(){
			public Object decode(Object jv) throws IOException
			{
				if( jv instanceof Number ) return new AtomicInteger(((Number)jv).intValue());
				return (AtomicInteger)null;
			}
		};
		GlobalDecoderMap.put(AtomicInteger.class, d);

		d = new Decoder(){
			public Object decode(Object jv) throws IOException
			{
				if( jv instanceof Number ) return new AtomicLong(((Number)jv).longValue());
				return (AtomicLong)null;
			}
		};
		GlobalDecoderMap.put(AtomicLong.class, d);
		
		d = new Decoder(){
			public Object decode(Object jv) throws IOException
			{
				if( jv instanceof String ) {
					try {
						return new SimpleDateFormat(DATE_FORMAT).parse((String) jv);
					} catch (ParseException e) {
						throw new IllegalArgumentException(e.getMessage(), e);
					}
				}
				if( jv instanceof Number ) 
					return new Date(((Number)jv).longValue());
				return (Date)null;
			}
		};
		GlobalDecoderMap.put(Date.class, d);
	}
}