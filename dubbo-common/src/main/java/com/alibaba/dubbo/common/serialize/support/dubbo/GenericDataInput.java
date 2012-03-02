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
package com.alibaba.dubbo.common.serialize.support.dubbo;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UTFDataFormatException;

import com.alibaba.dubbo.common.serialize.DataInput;

/**
 * Default DataInput impl.
 * Not thread-safe.
 * 
 * @author qian.lei
 */

public class GenericDataInput implements DataInput, GenericDataFlags
{
	private static final String EMPTY_STRING = "";

	private static final byte[] EMPTY_BYTES = {};

	private final InputStream mInput;

	private final byte[] mBuffer;

	private int mRead = 0;

	private int mPosition = 0;

	public GenericDataInput(InputStream is)
	{
		this(is, 1024);
	}

	public GenericDataInput(InputStream is, int buffSize)
	{
		mInput = is;
		mBuffer = new byte[buffSize];
	}

	public boolean readBool() throws IOException
	{
		byte b = read0();

		switch( b )
		{
			case VARINT_0: return false;
			case VARINT_1: return true;
			default:
				throw new IOException("Tag error, expect BYTE_TRUE|BYTE_FALSE, but get " + b);
		}
	}

	public byte readByte() throws IOException
	{
		byte b = read0();

		switch( b )
		{
			case VARINT8:
				return read0();
			case VARINT_0: return 0; case VARINT_1: return 1; case VARINT_2: return 2; case VARINT_3: return 3;
			case VARINT_4: return 4; case VARINT_5: return 5; case VARINT_6: return 6; case VARINT_7: return 7;
			case VARINT_8: return 8; case VARINT_9: return 9; case VARINT_A: return 10; case VARINT_B: return 11;
			case VARINT_C: return 12; case VARINT_D: return 13; case VARINT_E: return 14; case VARINT_F: return 15;
			case VARINT_10: return 16; case VARINT_11: return 17; case VARINT_12: return 18; case VARINT_13: return 19;
			case VARINT_14: return 20; case VARINT_15: return 21; case VARINT_16: return 22; case VARINT_17: return 23;
			case VARINT_18: return 24; case VARINT_19: return 25; case VARINT_1A: return 26; case VARINT_1B: return 27;
			case VARINT_1C: return 28; case VARINT_1D: return 29; case VARINT_1E: return 30; case VARINT_1F: return 31;
			default:
				throw new IOException("Tag error, expect VARINT, but get " + b);
		}
	}

	public short readShort() throws IOException
	{
		return (short)readVarint32();
	}

	public int readInt() throws IOException
	{
		return readVarint32();
	}

	public long readLong() throws IOException
	{
		return readVarint64();
	}

	public float readFloat() throws IOException
	{
		return Float.intBitsToFloat(readVarint32());
	}

	public double readDouble() throws IOException
	{
		return Double.longBitsToDouble(readVarint64());
	}

	public String readUTF() throws IOException
	{
		byte b = read0();

		switch( b )
		{
			case OBJECT_BYTES:
				int len = readUInt();
				StringBuilder sb = new StringBuilder();

				for(int i=0;i<len;i++)
				{
					byte b1 = read0();
					if( (b1 & 0x80) == 0 )
					{
						sb.append((char)b1);
					}
					else if( (b1 & 0xE0) == 0xC0 )
					{
						byte b2 = read0();
						sb.append((char)(((b1 & 0x1F) << 6) | (b2 & 0x3F)));
					}
					else if( (b1 & 0xF0) == 0xE0 )
					{
						byte b2 = read0(), b3 = read0();
						sb.append((char)(((b1 & 0x0F) << 12) | ((b2 & 0x3F) << 6) | (b3 & 0x3F)));
					}
					else
						throw new UTFDataFormatException("Bad utf-8 encoding at " + b1);
				}
				return sb.toString();
			case OBJECT_NULL: return null;
			case OBJECT_DUMMY: return EMPTY_STRING;
			default:
				throw new IOException("Tag error, expect BYTES|BYTES_NULL|BYTES_EMPTY, but get " + b);
		}
	}

	public byte[] readBytes() throws IOException
	{
		byte b = read0();

		switch( b )
		{
			case OBJECT_BYTES: return read0(readUInt());
			case OBJECT_NULL: return null;
			case OBJECT_DUMMY: return EMPTY_BYTES;
			default:
				throw new IOException("Tag error, expect BYTES|BYTES_NULL|BYTES_EMPTY, but get " + b);
		}
	}

	public int readUInt() throws IOException
	{
		byte tmp = read0();
		if( tmp < 0 )
			return tmp & 0x7f;

		int ret = tmp & 0x7f;
		if( ( tmp = read0() ) < 0 )
		{
			ret |= ( tmp & 0x7f ) << 7;
		}
		else
		{
			ret |= tmp << 7;
			if( ( tmp = read0() ) < 0 )
			{
				ret |= ( tmp & 0x7f ) << 14;
			}
			else
			{
				ret |= tmp << 14;
				if( ( tmp = read0() ) < 0 )
				{
			        ret |= ( tmp & 0x7f ) << 21;
				}
				else
				{
					ret |= tmp << 21;
					ret |= ( read0() & 0x7f ) << 28;
				}
			}
		}
		return ret;
	}

	protected byte read0() throws IOException
	{
		if( mPosition == mRead )
			fillBuffer();

		return mBuffer[mPosition++];
	}

	protected byte[] read0(int len) throws IOException
	{
		int rem = mRead - mPosition;
		byte[] ret = new byte[len];
		if( len <= rem )
		{
			System.arraycopy(mBuffer, mPosition, ret, 0, len);
			mPosition += len;
		}
		else
		{
			System.arraycopy(mBuffer, mPosition, ret, 0, rem);
			mPosition = mRead;

			len -= rem;
			int read, pos = rem;

			while( len > 0 )
			{
				read = mInput.read(ret, pos, len);
				if( read == -1 )
					throw new EOFException();
				pos += read;
				len -= read;
			}
		}
		return ret;
	}

	private int readVarint32() throws IOException
	{
		byte b = read0();

		switch( b )
		{
			case VARINT8:
				return read0();
			case VARINT16:
			{
				byte b1 = read0(), b2 = read0();
				return (short)( ( b1 & 0xff ) | ( ( b2 & 0xff ) << 8 ) );
			}
			case VARINT24:
			{
				byte b1 = read0(), b2 = read0(), b3 = read0();
				int ret = ( b1 & 0xff ) | ( ( b2 & 0xff ) << 8 ) | ( ( b3 & 0xff ) << 16 );
				if( b3 < 0 )
					return ret | 0xff000000;
				return ret;
			}
			case VARINT32:
			{
				byte b1 = read0(), b2 = read0(), b3 = read0(), b4 = read0();
				return ( ( b1 & 0xff ) |
					( ( b2 & 0xff ) << 8 ) |
					( ( b3 & 0xff ) << 16 ) |
					( ( b4 & 0xff ) << 24 ) );
			}
			case VARINT_NF: return -15; case VARINT_NE: return -14; case VARINT_ND: return -13;
			case VARINT_NC: return -12; case VARINT_NB: return -11; case VARINT_NA: return -10; case VARINT_N9: return -9;
			case VARINT_N8: return -8; case VARINT_N7: return -7; case VARINT_N6: return -6; case VARINT_N5: return -5;
			case VARINT_N4: return -4; case VARINT_N3: return -3; case VARINT_N2: return -2; case VARINT_N1: return -1;
			case VARINT_0: return 0; case VARINT_1: return 1; case VARINT_2: return 2; case VARINT_3: return 3;
			case VARINT_4: return 4; case VARINT_5: return 5; case VARINT_6: return 6; case VARINT_7: return 7;
			case VARINT_8: return 8; case VARINT_9: return 9; case VARINT_A: return 10; case VARINT_B: return 11;
			case VARINT_C: return 12; case VARINT_D: return 13; case VARINT_E: return 14; case VARINT_F: return 15;
			case VARINT_10: return 16; case VARINT_11: return 17; case VARINT_12: return 18; case VARINT_13: return 19;
			case VARINT_14: return 20; case VARINT_15: return 21; case VARINT_16: return 22; case VARINT_17: return 23;
			case VARINT_18: return 24; case VARINT_19: return 25; case VARINT_1A: return 26; case VARINT_1B: return 27;
			case VARINT_1C: return 28; case VARINT_1D: return 29; case VARINT_1E: return 30; case VARINT_1F: return 31;
			default:
				throw new IOException("Tag error, expect VARINT, but get " + b);
		}
	}

	private long readVarint64() throws IOException
	{
		byte b = read0();

		switch( b )
		{
			case VARINT8:
				return read0();
			case VARINT16:
			{
				byte b1 = read0(), b2 = read0();
				return (short)( ( b1 & 0xff ) | ( ( b2 & 0xff ) << 8 ) );
			}
			case VARINT24:
			{
				byte b1 = read0(), b2 = read0(), b3 = read0();
				int ret = ( b1 & 0xff ) | ( ( b2 & 0xff ) << 8 ) | ( ( b3 & 0xff ) << 16 );
				if( b3 < 0 )
					return ret | 0xff000000;
				return ret;
			}
			case VARINT32:
			{
				byte b1 = read0(), b2 = read0(), b3 = read0(), b4 = read0();
				return ( ( b1 & 0xff ) |
					( ( b2 & 0xff ) << 8 ) |
					( ( b3 & 0xff ) << 16 ) |
					( ( b4 & 0xff ) << 24 ) );
			}
			case VARINT40:
			{
				byte b1 = read0(), b2 = read0(), b3 = read0(), b4 = read0(), b5 = read0();
				long ret = ( (long)b1 & 0xff ) |
					( ( (long)b2 & 0xff ) << 8 ) |
					( ( (long)b3 & 0xff ) << 16 ) |
					( ( (long)b4 & 0xff ) << 24 ) |
					( ( (long)b5 & 0xff ) << 32 );
				if( b5 < 0 )
					return ret | 0xffffff0000000000l;
				return ret;
			}
			case VARINT48:
			{
				byte b1 = read0(), b2 = read0(), b3 = read0(), b4 = read0(), b5 = read0(), b6 = read0();
				long ret = ( (long)b1 & 0xff ) |
					( ( (long)b2 & 0xff ) << 8 ) |
					( ( (long)b3 & 0xff ) << 16 ) |
					( ( (long)b4 & 0xff ) << 24 ) |
					( ( (long)b5 & 0xff ) << 32 ) |
					( ( (long)b6 & 0xff ) << 40 );
				if( b6 < 0 )
					return ret | 0xffff000000000000l;
				return ret;
			}
			case VARINT56:
			{
				byte b1 = read0(), b2 = read0(), b3 = read0(), b4 = read0(), b5 = read0(), b6 = read0(), b7 = read0();
				long ret = ( (long)b1 & 0xff ) |
					( ( (long)b2 & 0xff ) << 8 ) |
					( ( (long)b3 & 0xff ) << 16 ) |
					( ( (long)b4 & 0xff ) << 24 ) |
					( ( (long)b5 & 0xff ) << 32 ) |
					( ( (long)b6 & 0xff ) << 40 ) |
					( ( (long)b7 & 0xff ) << 48 );
				if( b7 < 0 )
					return ret | 0xff00000000000000l;
				return ret;
			}
			case VARINT64:
			{
				byte b1 = read0(), b2 = read0(), b3 = read0(), b4 = read0();
				byte b5 = read0(), b6 = read0(), b7 = read0(), b8 = read0();
				return ( ( (long)b1 & 0xff ) |
					( ( (long)b2 & 0xff ) << 8 ) |
					( ( (long)b3 & 0xff ) << 16 ) |
					( ( (long)b4 & 0xff ) << 24 ) |
					( ( (long)b5 & 0xff ) << 32 ) |
					( ( (long)b6 & 0xff ) << 40 ) |
					( ( (long)b7 & 0xff ) << 48 ) |
					( ( (long)b8 & 0xff ) << 56 ) );
			}
			case VARINT_NF: return -15; case VARINT_NE: return -14; case VARINT_ND: return -13;
			case VARINT_NC: return -12; case VARINT_NB: return -11; case VARINT_NA: return -10; case VARINT_N9: return -9;
			case VARINT_N8: return -8; case VARINT_N7: return -7; case VARINT_N6: return -6; case VARINT_N5: return -5;
			case VARINT_N4: return -4; case VARINT_N3: return -3; case VARINT_N2: return -2; case VARINT_N1: return -1;
			case VARINT_0: return 0; case VARINT_1: return 1; case VARINT_2: return 2; case VARINT_3: return 3;
			case VARINT_4: return 4; case VARINT_5: return 5; case VARINT_6: return 6; case VARINT_7: return 7;
			case VARINT_8: return 8; case VARINT_9: return 9; case VARINT_A: return 10; case VARINT_B: return 11;
			case VARINT_C: return 12; case VARINT_D: return 13; case VARINT_E: return 14; case VARINT_F: return 15;
			case VARINT_10: return 16; case VARINT_11: return 17; case VARINT_12: return 18; case VARINT_13: return 19;
			case VARINT_14: return 20; case VARINT_15: return 21; case VARINT_16: return 22; case VARINT_17: return 23;
			case VARINT_18: return 24; case VARINT_19: return 25; case VARINT_1A: return 26; case VARINT_1B: return 27;
			case VARINT_1C: return 28; case VARINT_1D: return 29; case VARINT_1E: return 30; case VARINT_1F: return 31;
			default:
				throw new IOException("Tag error, expect VARINT, but get " + b);
		}
	}

	private void fillBuffer() throws IOException
	{
		mPosition = 0;
		mRead = mInput.read(mBuffer);

		if( mRead == -1 )
		{
			mRead = 0;
			throw new EOFException();
		}
	}
}