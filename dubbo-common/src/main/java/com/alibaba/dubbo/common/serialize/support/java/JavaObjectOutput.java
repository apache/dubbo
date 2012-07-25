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

import com.alibaba.dubbo.common.serialize.support.nativejava.NativeJavaObjectOutput;

/**
 * Java Object output.
 * 
 * @author qian.lei
 */

public class JavaObjectOutput extends NativeJavaObjectOutput
{
	public JavaObjectOutput(OutputStream os) throws IOException
	{
		super(new ObjectOutputStream(os));
	}

	public JavaObjectOutput(OutputStream os, boolean compact) throws IOException
	{
		super(compact ? new CompactedObjectOutputStream(os) : new ObjectOutputStream(os));
	}

	public void writeUTF(String v) throws IOException
	{
		if( v == null )
		{
			getObjectOutputStream().writeInt(-1);
		}
		else
		{
			getObjectOutputStream().writeInt(v.length());
			getObjectOutputStream().writeUTF(v);
		}
	}

	public void writeObject(Object obj) throws IOException
	{
		if( obj == null )
		{
			getObjectOutputStream().writeByte(0);
		}
		else
		{
			getObjectOutputStream().writeByte(1);
			getObjectOutputStream().writeObject(obj);
		}
	}

	public void flushBuffer() throws IOException
	{
		getObjectOutputStream().flush();
	}
}