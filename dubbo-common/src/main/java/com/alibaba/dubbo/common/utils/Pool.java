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

/**
 * Pool.
 * 
 * @author qian.lei
 */

public interface Pool<T>
{
	/**
	 * clear.
	 */
	void clear() throws Exception;

	/**
	 * close pool.
	 * 
	 * @throws Exception.
	 */
	void close() throws Exception;

	/**
	 * borrow.
	 * 
	 * @return object.
	 */
	T borrowObject() throws Exception;

	/**
	 * borrow.
	 * 
	 * @param timeout timeout.
	 * @return object.
	 */
	T borrowObject(long timeout) throws Exception;

	/**
	 * return object.
	 * 
	 * @param obj object.
	 */
	void returnObject(T obj) throws Exception;

	/**
	 * get factory.
	 * 
	 * @return Factory instance.
	 */
	Factory<T> getFactory();

	/**
	 * get idle number.
	 * 
	 * @return idle number.
	 */
	int getIdleNum();

	/**
	 * get active number.
	 * 
	 * @return active number.
	 */
	int getActiveNum();

	/**
	 * pool factory.
	 */
	public interface Factory<T>
	{
		/**
		 * make object.
		 * 
		 * @return object.
		 * @throws Exception.
		 */
		T makeObject() throws Exception;

		/**
		 * destroy object.
		 * 
		 * @param obj object.
		 * @throws Exception.
		 */
		void destroyObject(T obj) throws Exception;

		/**
		 * activate object.
		 * 
		 * @param obj object.
		 */
		void activateObject(T obj);

		/**
		 * passivate object.
		 * 
		 * @param obj object.
		 */
		void passivateObject(T obj);

//		/**
//		 * validate object.
//		 * 
//		 * @param obj object.
//		 * @return valid or not.
//		 */
//		boolean validateObject(T obj);
	}
}