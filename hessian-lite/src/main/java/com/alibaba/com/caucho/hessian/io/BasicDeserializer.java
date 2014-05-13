/*
 * Copyright (c) 2001-2008 Caucho Technology, Inc.  All rights reserved.
 *
 * The Apache Software License, Version 1.1
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Burlap", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Scott Ferguson
 */

package com.alibaba.com.caucho.hessian.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Serializing an object for known object types.
 */
public class BasicDeserializer extends AbstractDeserializer {
  public static final int NULL = BasicSerializer.NULL;
  public static final int BOOLEAN = BasicSerializer.BOOLEAN;
  public static final int BYTE = BasicSerializer.BYTE;
  public static final int SHORT = BasicSerializer.SHORT;
  public static final int INTEGER = BasicSerializer.INTEGER;
  public static final int LONG = BasicSerializer.LONG;
  public static final int FLOAT = BasicSerializer.FLOAT;
  public static final int DOUBLE = BasicSerializer.DOUBLE;
  public static final int CHARACTER = BasicSerializer.CHARACTER;
  public static final int CHARACTER_OBJECT = BasicSerializer.CHARACTER_OBJECT;
  public static final int STRING = BasicSerializer.STRING;
  public static final int DATE = BasicSerializer.DATE;
  public static final int NUMBER = BasicSerializer.NUMBER;
  public static final int OBJECT = BasicSerializer.OBJECT;
  
  public static final int BOOLEAN_ARRAY = BasicSerializer.BOOLEAN_ARRAY;
  public static final int BYTE_ARRAY = BasicSerializer.BYTE_ARRAY;
  public static final int SHORT_ARRAY = BasicSerializer.SHORT_ARRAY;
  public static final int INTEGER_ARRAY = BasicSerializer.INTEGER_ARRAY;
  public static final int LONG_ARRAY = BasicSerializer.LONG_ARRAY;
  public static final int FLOAT_ARRAY = BasicSerializer.FLOAT_ARRAY;
  public static final int DOUBLE_ARRAY = BasicSerializer.DOUBLE_ARRAY;
  public static final int CHARACTER_ARRAY = BasicSerializer.CHARACTER_ARRAY;
  public static final int STRING_ARRAY = BasicSerializer.STRING_ARRAY;
  public static final int OBJECT_ARRAY = BasicSerializer.OBJECT_ARRAY;

  private int _code;

  public BasicDeserializer(int code)
  {
    _code = code;
  }

  public Class getType()
  {
    switch (_code) {
    case NULL:
      return void.class;
    case BOOLEAN:
      return Boolean.class;
    case BYTE:
      return Byte.class;
    case SHORT:
      return Short.class;
    case INTEGER:
      return Integer.class;
    case LONG:
      return Long.class;
    case FLOAT:
      return Float.class;
    case DOUBLE:
      return Double.class;
    case CHARACTER:
      return Character.class;
    case CHARACTER_OBJECT:
      return Character.class;
    case STRING:
      return String.class;
    case DATE:
      return Date.class;
    case NUMBER:
      return Number.class;
    case OBJECT:
      return Object.class;

    case BOOLEAN_ARRAY:
      return boolean[].class;
    case BYTE_ARRAY:
      return byte[].class;
    case SHORT_ARRAY:
      return short[].class;
    case INTEGER_ARRAY:
      return int[].class;
    case LONG_ARRAY:
      return long[].class;
    case FLOAT_ARRAY:
      return float[].class;
    case DOUBLE_ARRAY:
      return double[].class;
    case CHARACTER_ARRAY:
      return char[].class;
    case STRING_ARRAY:
      return String[].class;
    case OBJECT_ARRAY:
      return Object[].class;
    default:
      throw new UnsupportedOperationException();
    }
  }
  
  public Object readObject(AbstractHessianInput in)
    throws IOException
  {
    switch (_code) {
    case NULL:
      // hessian/3490
      in.readObject();
      
      return null;
      
    case BOOLEAN:
      return Boolean.valueOf(in.readBoolean());
      
    case BYTE:
      return Byte.valueOf((byte) in.readInt());
      
    case SHORT:
      return Short.valueOf((short) in.readInt());
      
    case INTEGER:
      return Integer.valueOf(in.readInt());

    case LONG:
      return Long.valueOf(in.readLong());

    case FLOAT:
      return Float.valueOf((float) in.readDouble());

    case DOUBLE:
      return Double.valueOf(in.readDouble());
      
    case STRING:
      return in.readString();
      
    case OBJECT:
      return in.readObject();
      
    case CHARACTER:
      {
	String s = in.readString();
	if (s == null || s.equals(""))
	  return Character.valueOf((char) 0);
	else
	  return Character.valueOf(s.charAt(0));
      }
      
    case CHARACTER_OBJECT:
      {
	String s = in.readString();
	if (s == null || s.equals(""))
	  return null;
	else
	  return Character.valueOf(s.charAt(0));
      }
      
    case DATE:
      return new Date(in.readUTCDate());
      
    case NUMBER:
      return in.readObject();

    case BYTE_ARRAY:
      return in.readBytes();

    case CHARACTER_ARRAY:
    {
      String s = in.readString();

      if (s == null)
        return null;
      else {
        int len = s.length();
        char []chars = new char[len];
        s.getChars(0, len, chars, 0);
        return chars;
      }
    }

    case BOOLEAN_ARRAY:
    case SHORT_ARRAY:
    case INTEGER_ARRAY:
    case LONG_ARRAY:
    case FLOAT_ARRAY:
    case DOUBLE_ARRAY:
    case STRING_ARRAY:
    {
      int code = in.readListStart();

      switch (code) {
      case 'N':
	return null;

      case 0x10: case 0x11: case 0x12: case 0x13:
      case 0x14: case 0x15: case 0x16: case 0x17:
      case 0x18: case 0x19: case 0x1a: case 0x1b:
      case 0x1c: case 0x1d: case 0x1e: case 0x1f:
	int length = code - 0x10;
	in.readInt();

	return readLengthList(in, length);

      default:
	String type = in.readType();
	length = in.readLength();

	return readList(in, length);
      }
    }

    default:
      throw new UnsupportedOperationException();
    }
  }
  
  public Object readList(AbstractHessianInput in, int length)
    throws IOException
  {
    switch (_code) {
    case BOOLEAN_ARRAY: {
      if (length >= 0) {
        boolean []data = new boolean[length];

        in.addRef(data);

        for (int i = 0; i < data.length; i++)
          data[i] = in.readBoolean();
	
        in.readEnd();

        return data;
      }
      else {
        ArrayList list = new ArrayList();

        while (! in.isEnd())
          list.add(Boolean.valueOf(in.readBoolean()));
	
        in.readEnd();

        boolean []data = new boolean[list.size()];
        
        in.addRef(data);
        
        for (int i = 0; i < data.length; i++)
          data[i] = ((Boolean) list.get(i)).booleanValue();

        return data;
      }
    }
    
    case SHORT_ARRAY: {
      if (length >= 0) {
        short []data = new short[length];

        in.addRef(data);
        
        for (int i = 0; i < data.length; i++)
          data[i] = (short) in.readInt();
	
        in.readEnd();

        return data;
      }
      else {
        ArrayList list = new ArrayList();

        while (! in.isEnd())
          list.add(Short.valueOf((short) in.readInt()));

        in.readEnd();

        short []data = new short[list.size()];
        for (int i = 0; i < data.length; i++)
          data[i] = ((Short) list.get(i)).shortValue();

        in.addRef(data);
        
        return data;
      }
    }
    
    case INTEGER_ARRAY: {
      if (length >= 0) {
        int []data = new int[length];

        in.addRef(data);
        
        for (int i = 0; i < data.length; i++)
          data[i] = in.readInt();

        in.readEnd();
	
        return data;
      }
      else {
        ArrayList list = new ArrayList();

        while (! in.isEnd())
          list.add(Integer.valueOf(in.readInt()));


        in.readEnd();
	
        int []data = new int[list.size()];
        for (int i = 0; i < data.length; i++)
          data[i] = ((Integer) list.get(i)).intValue();
        
        in.addRef(data);

        return data;
      }
    }
    
    case LONG_ARRAY: {
      if (length >= 0) {
        long []data = new long[length];

        in.addRef(data);

        for (int i = 0; i < data.length; i++)
          data[i] = in.readLong();

        in.readEnd();

        return data;
      }
      else {
        ArrayList list = new ArrayList();

        while (! in.isEnd())
          list.add(Long.valueOf(in.readLong()));

        in.readEnd();
        
        long []data = new long[list.size()];
        for (int i = 0; i < data.length; i++)
          data[i] = ((Long) list.get(i)).longValue();
        
        in.addRef(data);

        return data;
      }
    }
    
    case FLOAT_ARRAY: {
      if (length >= 0) {
        float []data = new float[length];
        in.addRef(data);

        for (int i = 0; i < data.length; i++)
          data[i] = (float) in.readDouble();

        in.readEnd();

        return data;
      }
      else {
        ArrayList list = new ArrayList();

        while (! in.isEnd())
          list.add(new Float(in.readDouble()));

        in.readEnd();
        
        float []data = new float[list.size()];
        for (int i = 0; i < data.length; i++)
          data[i] = ((Float) list.get(i)).floatValue();
        
        in.addRef(data);

        return data;
      }
    }
    
    case DOUBLE_ARRAY: {
      if (length >= 0) {
        double []data = new double[length];
        in.addRef(data);

        for (int i = 0; i < data.length; i++)
          data[i] = in.readDouble();

        in.readEnd();

        return data;
      }
      else {
        ArrayList list = new ArrayList();

        while (! in.isEnd())
          list.add(new Double(in.readDouble()));

        in.readEnd();
        
        double []data = new double[list.size()];
        in.addRef(data);
        for (int i = 0; i < data.length; i++)
          data[i] = ((Double) list.get(i)).doubleValue();

        return data;
      }
    }
    
    case STRING_ARRAY: {
      if (length >= 0) {
        String []data = new String[length];
        in.addRef(data);

        for (int i = 0; i < data.length; i++)
          data[i] = in.readString();

        in.readEnd();

        return data;
      }
      else {
        ArrayList list = new ArrayList();

        while (! in.isEnd())
          list.add(in.readString());

        in.readEnd();
        
        String []data = new String[list.size()];
        in.addRef(data);
        for (int i = 0; i < data.length; i++)
          data[i] = (String) list.get(i);

        return data;
      }
    }
    
    case OBJECT_ARRAY: {
      if (length >= 0) {
        Object []data = new Object[length];
        in.addRef(data);

        for (int i = 0; i < data.length; i++)
          data[i] = in.readObject();

        in.readEnd();

        return data;
      }
      else {
        ArrayList list = new ArrayList();
        
        in.addRef(list); // XXX: potential issues here

        while (! in.isEnd())
          list.add(in.readObject());

        in.readEnd();
        
        Object []data = new Object[list.size()];
        for (int i = 0; i < data.length; i++)
          data[i] = (Object) list.get(i);

        return data;
      }
    }
    
    default:
      throw new UnsupportedOperationException(String.valueOf(this));
    }
  }
  
  public Object readLengthList(AbstractHessianInput in, int length)
    throws IOException
  {
    switch (_code) {
    case BOOLEAN_ARRAY: {
      boolean []data = new boolean[length];

      in.addRef(data);

      for (int i = 0; i < data.length; i++)
	data[i] = in.readBoolean();

      return data;
    }
    
    case SHORT_ARRAY: {
      short []data = new short[length];

      in.addRef(data);
        
      for (int i = 0; i < data.length; i++)
	data[i] = (short) in.readInt();

      return data;
    }
    
    case INTEGER_ARRAY: {
      int []data = new int[length];

      in.addRef(data);
        
      for (int i = 0; i < data.length; i++)
	data[i] = in.readInt();

      return data;
    }
    
    case LONG_ARRAY: {
      long []data = new long[length];

      in.addRef(data);
        
      for (int i = 0; i < data.length; i++)
	data[i] = in.readLong();

      return data;
    }
    
    case FLOAT_ARRAY: {
      float []data = new float[length];
      in.addRef(data);

      for (int i = 0; i < data.length; i++)
	data[i] = (float) in.readDouble();

      return data;
    }
    
    case DOUBLE_ARRAY: {
      double []data = new double[length];
      in.addRef(data);

      for (int i = 0; i < data.length; i++)
	data[i] = in.readDouble();

      return data;
    }
    
    case STRING_ARRAY: {
      String []data = new String[length];
      in.addRef(data);

      for (int i = 0; i < data.length; i++)
	data[i] = in.readString();

      return data;
    }
    
    case OBJECT_ARRAY: {
      Object []data = new Object[length];
      in.addRef(data);

      for (int i = 0; i < data.length; i++)
	data[i] = in.readObject();

      return data;
    }
    
    default:
      throw new UnsupportedOperationException(String.valueOf(this));
    }
  }
}
