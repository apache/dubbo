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
import java.util.EmptyStackException;
import java.util.List;

/**
 * Stack.
 * 
 * @author qian.lei
 */

public class Stack<E>
{
	private int mSize = 0;

	private List<E> mElements = new ArrayList<E>();

	public Stack(){}

	/**
	 * push.
	 * 
	 * @param ele
	 */
	public void push(E ele)
	{
		if( mElements.size() > mSize )
			mElements.set(mSize, ele);
		else
			mElements.add(ele);
		mSize++;
	}

	/**
	 * pop.
	 * 
	 * @return the last element.
	 */
	public E pop()
	{
		if( mSize == 0 )
			throw new EmptyStackException();
		return mElements.set(--mSize, null);
	}

	/**
	 * peek.
	 * 
	 * @return the last element.
	 */
	public E peek()
	{
		if( mSize == 0 )
			throw new EmptyStackException();
		return mElements.get(mSize-1);
	}

	/**
	 * get.
	 * 
	 * @param index index.
	 * @return element.
	 */
	public E get(int index)
	{
		if( index >= mSize )
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + mSize);

		return index < 0 ? mElements.get(index+mSize) : mElements.get(index);
	}

	/**
	 * set.
	 * 
	 * @param index index.
	 * @param value element.
	 * @return old element.
	 */
	public E set(int index, E value)
	{
		if( index >= mSize )
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + mSize);

		return mElements.set(index < 0 ? index + mSize : index, value);
	}

	/**
	 * remove.
	 * 
	 * @param index
	 * @return element
	 */
	public E remove(int index)
	{
		if( index >= mSize )
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + mSize);

		E ret = mElements.remove(index < 0 ? index + mSize : index);
		mSize--;
		return ret;
	}

	/**
	 * get stack size.
	 * 
	 * @return size.
	 */
	public int size()
	{
		return mSize;
	}

	/**
	 * is empty.
	 * 
	 * @return empty or not.
	 */
	public boolean isEmpty()
	{
		return mSize == 0;
	}

	/**
	 * clear stack.
	 */
	public void clear()
	{
		mSize = 0;
		mElements.clear();
	}
}