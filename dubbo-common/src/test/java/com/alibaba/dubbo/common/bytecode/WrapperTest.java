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
package com.alibaba.dubbo.common.bytecode;

import org.junit.Test;
import static org.junit.Assert.*;

public class WrapperTest
{
    @Test
	public void testMain() throws Exception
	{
		Wrapper w = Wrapper.getWrapper(I1.class);
		String[] ns = w.getDeclaredMethodNames();
		assertEquals(ns.length, 5);
		ns = w.getMethodNames();
		assertEquals(ns.length, 6);

		Object obj = new Impl1();
		assertEquals(w.getPropertyValue(obj, "name"), "you name");

		w.setPropertyValue(obj, "name", "changed");
		assertEquals(w.getPropertyValue(obj, "name"), "changed");

		w.invokeMethod(obj, "hello", new Class<?>[] {String.class}, new Object[]{ "qianlei" });
	}
	
	// bug: DUBBO-132
    @Test
	public void test_unwantedArgument() throws Exception {
	    Wrapper w = Wrapper.getWrapper(I1.class);
	    Object obj = new Impl1();
        try {
            w.invokeMethod(obj, "hello", new Class<?>[] { String.class, String.class },
                    new Object[] { "qianlei", "badboy" });
            fail();
        } catch (NoSuchMethodException expected) {
        }
    }
	
	//bug: DUBBO-425
    @Test
    public void test_makeEmptyClass() throws Exception {
        Wrapper.getWrapper(EmptyServiceImpl.class);
    }

    /**
     * see http://code.alibabatech.com/jira/browse/DUBBO-571
     */
    @Test
    public void test_getDeclaredMethodNames_ContainExtendsParentMethods() throws Exception {
        assertArrayEquals(new String[]{"hello", }, Wrapper.getWrapper(Parent1.class).getMethodNames());

        assertArrayEquals(new String[]{}, Wrapper.getWrapper(Son.class).getDeclaredMethodNames());
    }

    /**
     * see http://code.alibabatech.com/jira/browse/DUBBO-571
     */
    @Test
    public void test_getMethodNames_ContainExtendsParentMethods() throws Exception {
        assertArrayEquals(new String[]{"hello", "world"}, Wrapper.getWrapper(Son.class).getMethodNames());
    }

    public static class Impl0
	{
		public float a,b,c;
	}

	public static interface I0
	{
		String getName();
	}

	public static interface I1 extends I0
	{
		void setName(String name);

		void hello(String name);

		int showInt(int v);

		void setFloat(float f);

		float getFloat();
	}

	public static class Impl1 implements I1
	{
		private String name = "you name";

		private float fv = 0;

		public String getName()
		{
			return name;
		}

		public void setName(String name)
		{
			this.name = name;
		}

		public void hello(String name)
		{
			System.out.println("hello " + name);
		}

		public int showInt(int v)
		{
			return v;
		}

		public float getFloat()
		{
			return fv;
		}

		public void setFloat(float f)
		{
			fv = f;
		}
	}


	
	public static interface EmptyService
    {
    }
	
	public static class EmptyServiceImpl implements EmptyService
    {
    }

    public static interface Parent1 {
        void hello();
    }

    public static interface Parent2 {
        void world();
    }

    public static interface Son extends Parent1, Parent2 {

    }
}