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
package com.alibaba.dubbo.common.io;

import java.io.IOException;
import java.io.Reader;

/**
 * Thread unsafed StringReader.
 * 
 * @author qian.lei
 */

public class UnsafeStringReader extends Reader
{
	private String mString;

	private int mPosition, mLimit, mMark;

	public UnsafeStringReader(String str)
	{
		mString = str;
		mLimit = str.length();
		mPosition = mMark = 0;
	}

	@Override
	public int read() throws IOException
	{
		ensureOpen();
		if( mPosition >= mLimit )
			return -1;

		return mString.charAt(mPosition++);
	}

	@Override
	public int read(char[] cs, int off, int len) throws IOException
	{
		ensureOpen();
		if( (off < 0) || (off > cs.length) || (len < 0) ||
				((off + len) > cs.length) || ((off + len) < 0) )
			throw new IndexOutOfBoundsException();

		if( len == 0 )
			return 0;

		if( mPosition >= mLimit )
			return -1;

		int n = Math.min(mLimit - mPosition, len);
		mString.getChars(mPosition, mPosition + n, cs, off);
		mPosition += n;
		return n;
	}

	public long skip(long ns) throws IOException
	{
		ensureOpen();
		if( mPosition >= mLimit )
			return 0;

		long n = Math.min(mLimit - mPosition, ns);
		n = Math.max(-mPosition, n);
		mPosition += n;
		return n;
	}

	public boolean ready() throws IOException
	{
		ensureOpen();
		return true;
	}

	@Override
	public boolean markSupported()
	{
		return true;
	}

	public void mark(int readAheadLimit) throws IOException
	{
		if( readAheadLimit < 0 )
			throw new IllegalArgumentException("Read-ahead limit < 0");

		ensureOpen();
		mMark = mPosition;
	}

	public void reset() throws IOException
	{
		ensureOpen();
		mPosition = mMark;
	}
 
	@Override
	public void close() throws IOException
	{
		mString = null;
	}

    private void ensureOpen() throws IOException
    {
    	if( mString == null )
    		throw new IOException("Stream closed");
	}
}