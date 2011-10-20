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

/**
 * JSONToken.
 * 
 * @author qian.lei
 */

public class JSONToken
{
	// token type
	public static final int ANY = 0, IDENT = 0x01, LBRACE = 0x02, LSQUARE = 0x03, RBRACE = 0x04, RSQUARE = 0x05, COMMA = 0x06, COLON = 0x07;

	public static final int NULL = 0x10, BOOL = 0x11, INT = 0x12, FLOAT = 0x13, STRING = 0x14, ARRAY = 0x15, OBJECT = 0x16;

	public final int type;

	public final Object value;

	JSONToken(int t)
	{
		this(t, null);
	}

	JSONToken(int t, Object v)
	{
		type = t;
		value = v;
	}

	static String token2string(int t)
	{
		switch( t )
		{
			case LBRACE: return "{";
			case RBRACE: return "}";
			case LSQUARE: return "[";
			case RSQUARE: return "]";
			case COMMA: return ",";
			case COLON: return ":";
			case IDENT: return "IDENT";
			case NULL: return "NULL";
			case BOOL: return "BOOL VALUE";
			case INT: return "INT VALUE";
			case FLOAT: return "FLOAT VALUE";
			case STRING: return "STRING VALUE";
			default: return "ANY";
		}
	}
}