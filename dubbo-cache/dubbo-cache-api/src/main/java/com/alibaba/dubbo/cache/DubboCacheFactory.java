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

import java.util.HashMap;
import java.util.Map;

import com.alibaba.dubbo.cache.common.DubboCache;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;

public class DubboCacheFactory {
	
	protected static final Logger logger = LoggerFactory.getLogger(DubboCacheFactory.class);
	
	private static Map<String, CacheService> cacheServiceMap = new HashMap<String, CacheService>();
	
	public static CacheService getCacheService(DubboCache dubboCache) {
		//memcached://127.0.0.1:11211
		String address = dubboCache.getAddress();
		CacheService cacheService = cacheServiceMap.get(address);
		if (cacheService == null) {
			String cacheType = address.substring(0, address.indexOf("://"));
			String server = address.substring(address.indexOf("://") + 3);
			try {
				if (cacheType.equals("memcached")) {
					Class<?> clazz = Class.forName("com.alibaba.dubbo.cache.memcached.MemcachedCacheService");
					cacheService = (CacheService)clazz.newInstance();
				} else {
					cacheService = (CacheService)Class.forName("com.alibaba.dubbo.cache.redis.RedisCacheService").newInstance();
				}
				cacheService.connect(server, dubboCache.getMaxActive(), dubboCache.getMaxIdle(), dubboCache.getMinIdle(), dubboCache.getDefaultTimeout());
				cacheServiceMap.put(address, cacheService);
			} catch (Exception e) {
				logger.error("Initialize CacheService error! {}", e);
			}
		}
		return cacheService;
	}
	
	public static void removeCacheService(DubboCache dubboCache) {
		String address = dubboCache.getAddress();
		cacheServiceMap.remove(address);
	}

}
