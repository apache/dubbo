/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.common.utils;

import org.junit.Test;

import java.util.EmptyStackException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class StackTest {
    @Test
    public void testOps() throws Exception {
        Stack<String> stack = new Stack<String>();
        stack.push("one");
        assertThat(stack.get(0), equalTo("one"));
        assertThat(stack.peek(), equalTo("one"));
        assertThat(stack.size(), equalTo(1));
        stack.push("two");
        assertThat(stack.get(0), equalTo("one"));
        assertThat(stack.peek(), equalTo("two"));
        assertThat(stack.size(), equalTo(2));
        assertThat(stack.set(0, "three"), equalTo("one"));
        assertThat(stack.remove(0), equalTo("three"));
        assertThat(stack.size(), equalTo(1));
        assertThat(stack.isEmpty(), is(false));
        assertThat(stack.get(0), equalTo("two"));
        assertThat(stack.peek(), equalTo("two"));
        assertThat(stack.pop(), equalTo("two"));
        assertThat(stack.isEmpty(), is(true));
    }

    @Test
    public void testClear() throws Exception {
        Stack<String> stack = new Stack<String>();
        stack.push("one");
        stack.push("two");
        assertThat(stack.isEmpty(), is(false));
        stack.clear();
        assertThat(stack.isEmpty(), is(true));
    }

    @Test(expected = EmptyStackException.class)
    public void testIllegalPop() throws Exception {
        Stack<String> stack = new Stack<String>();
        stack.pop();
    }

    @Test(expected = EmptyStackException.class)
    public void testIllegalPeek() throws Exception {
        Stack<String> stack = new Stack<String>();
        stack.peek();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testIllegalGet() throws Exception {
        Stack<String> stack = new Stack<String>();
        stack.get(1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testIllegalSet() throws Exception {
        Stack<String> stack = new Stack<String>();
        stack.set(1, "illegal");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testIllegalRemove() throws Exception {
        Stack<String> stack = new Stack<String>();
        stack.remove(1);
    }
}
