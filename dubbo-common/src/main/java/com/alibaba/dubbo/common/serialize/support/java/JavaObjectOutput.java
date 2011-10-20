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
package com.alibaba.dubbo.common.serialize.support.java;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import com.alibaba.dubbo.common.serialize.ObjectOutput;

/**
 * Java Object output.
 * 
 * @author qian.lei
 */

public class JavaObjectOutput implements ObjectOutput
{
	private final ObjectOutputStream mOut;

	public JavaObjectOutput(OutputStream os) throws IOException
	{
		mOut = new ObjectOutputStream(os);
	}

	public JavaObjectOutput(OutputStream os, boolean compact) throws IOException
	{
		mOut = compact ? new CompactedObjectOutputStream(os) : new ObjectOutputStream(os);
	}

	public void writeBool(boolean v) throws IOException
	{
		mOut.writeBoolean(v);
	}

	public void writeByte(byte v) throws IOException
	{
		mOut.writeByte(v);
	}

	public void writeShort(short v) throws IOException
	{
		mOut.writeShort(v);
	}

	public void writeInt(int v) throws IOException
	{
		mOut.writeInt(v);
	}

	public void writeLong(long v) throws IOException
	{
		mOut.writeLong(v);
	}

	public void writeFloat(float v) throws IOException
	{
		mOut.writeFloat(v);
	}

	public void writeDouble(double v) throws IOException
	{
		mOut.writeDouble(v);
	}

	public void writeBytes(byte[] b) throws IOException
	{
		if( b == null )
			mOut.writeInt(-1);
		else
			this.writeBytes(b, 0, b.length);
	}

	public void writeBytes(byte[] b, int off, int len) throws IOException
	{
		mOut.writeInt(len);
		mOut.write(b, off, len);
	}

	public void writeUTF(String v) throws IOException
	{
		if( v == null )
		{
			mOut.writeInt(-1);
		}
		else
		{
			mOut.writeInt(v.length());
			mOut.writeUTF(v);
		}
	}

	public void writeObject(Object obj) throws IOException
	{
		if( obj == null )
		{
			mOut.writeByte(0);
		}
		else
		{
			mOut.writeByte(1);
			mOut.writeObject(obj);
		}
	}

	public void flushBuffer() throws IOException
	{
		mOut.flush();
	}
}