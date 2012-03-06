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
package com.alibaba.dubbo.common.utils;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

public class StringUtilsTest extends TestCase
{
	public void testJoin() throws Exception
	{
		String[] s = {"1","2","3"};
		assertEquals(StringUtils.join(s), "123");
		assertEquals(StringUtils.join(s, ','), "1,2,3");
	}

	public void testSplit() throws Exception
	{
		String s = "d,1,2,4";
		assertEquals(StringUtils.split(s, ',').length, 4);
	}

	public void testTranslat() throws Exception
	{
		String s = "16314";
		assertEquals(StringUtils.translat(s, "123456", "abcdef"), "afcad");
		assertEquals(StringUtils.translat(s, "123456", "abcd"), "acad");
	}

	public void testJoin_Colletion_String() throws Exception
	{
	    List<String> list = new ArrayList<String>();
	    assertEquals("", StringUtils.join(list, ","));
	    
	    list.add("v1");
        assertEquals("v1", StringUtils.join(list, "-"));
        
	    list.add("v2");
	    list.add("v3");
	    String out = StringUtils.join(list, ":");
	    assertEquals("v1:v2:v3", out);
	}
	
	@Test
	public void testCamelToSplitName() throws Exception
    {
	    assertEquals("ab-cd-ef", StringUtils.camelToSplitName("abCdEf", "-"));
        assertEquals("ab-cd-ef", StringUtils.camelToSplitName("AbCdEf", "-"));
        assertEquals("ab-cd-ef", StringUtils.camelToSplitName("ab-cd-ef", "-"));
        assertEquals("abcdef", StringUtils.camelToSplitName("abcdef", "-"));
    }
}