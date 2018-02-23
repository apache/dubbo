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
