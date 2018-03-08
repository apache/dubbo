/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.cache;

import com.alibaba.dubbo.cache.exception.CacheException;


public interface CacheService {
	
	public void connect(String address, int maxActive, int maxIdle, int minIdle, int defaultTimeout) throws CacheException;
	
	public boolean isConnected();
	
	public void put(String key, Object value) throws CacheException;
	
	/**
	 * 
	 * @param key
	 * @param value
	 * @param timeout unit second.
	 */
	public void put(String key, Object value, int timeout) throws CacheException;
	
	public Object get(String key) throws CacheException;
	
	public void delete(String key) throws CacheException;
	
	public void shutdown();
	
	public boolean isShutdown();

}
