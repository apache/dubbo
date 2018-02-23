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
