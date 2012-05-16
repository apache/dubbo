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

/**
 * Constants.
 * 
 * @author qian.lei
 */

public interface GenericDataFlags
{
	// prefix three bits
	byte VARINT = 0, OBJECT = (byte)0x80;

	// varint tag
	byte VARINT8 = VARINT, VARINT16 = VARINT | 1, VARINT24 = VARINT | 2, VARINT32 = VARINT | 3;

	byte VARINT40 = VARINT | 4, VARINT48 = VARINT | 5, VARINT56 = VARINT | 6, VARINT64 = VARINT | 7;

	// varint contants
	byte VARINT_NF = VARINT | 10, VARINT_NE = VARINT | 11, VARINT_ND = VARINT | 12;

	byte VARINT_NC = VARINT | 13, VARINT_NB = VARINT | 14, VARINT_NA = VARINT | 15, VARINT_N9 = VARINT | 16;

	byte VARINT_N8 = VARINT | 17, VARINT_N7 = VARINT | 18, VARINT_N6 = VARINT | 19, VARINT_N5 = VARINT | 20;

	byte VARINT_N4 = VARINT | 21, VARINT_N3 = VARINT | 22, VARINT_N2 = VARINT | 23, VARINT_N1 = VARINT | 24;

	byte VARINT_0 = VARINT | 25, VARINT_1 = VARINT | 26, VARINT_2 = VARINT | 27, VARINT_3 = VARINT | 28;

	byte VARINT_4 = VARINT | 29, VARINT_5 = VARINT | 30, VARINT_6 = VARINT | 31, VARINT_7 = VARINT | 32;

	byte VARINT_8 = VARINT | 33, VARINT_9 = VARINT | 34, VARINT_A = VARINT | 35, VARINT_B = VARINT | 36;

	byte VARINT_C = VARINT | 37, VARINT_D = VARINT | 38, VARINT_E = VARINT | 39, VARINT_F = VARINT | 40;

	byte VARINT_10 = VARINT | 41, VARINT_11 = VARINT | 42, VARINT_12 = VARINT | 43, VARINT_13 = VARINT | 44;

	byte VARINT_14 = VARINT | 45, VARINT_15 = VARINT | 46, VARINT_16 = VARINT | 47, VARINT_17 = VARINT | 48;

	byte VARINT_18 = VARINT | 49, VARINT_19 = VARINT | 50, VARINT_1A = VARINT | 51, VARINT_1B = VARINT | 52;

	byte VARINT_1C = VARINT | 53, VARINT_1D = VARINT | 54, VARINT_1E = VARINT | 55, VARINT_1F = VARINT | 56;

	// object tag
	byte OBJECT_REF = OBJECT | 1, OBJECT_STREAM = OBJECT | 2, OBJECT_BYTES = OBJECT | 3;

	byte OBJECT_VALUE = OBJECT | 4, OBJECT_VALUES = OBJECT | 5, OBJECT_MAP = OBJECT | 6;

	byte OBJECT_DESC = OBJECT | 10, OBJECT_DESC_ID = OBJECT | 11;

	// object constants
	byte OBJECT_NULL = OBJECT | 20, OBJECT_DUMMY = OBJECT | 21;
}