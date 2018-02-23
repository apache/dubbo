package com.alibaba.dubbo.cache.redis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

import com.alibaba.com.caucho.hessian.io.HessianInput;
import com.alibaba.com.caucho.hessian.io.HessianOutput;
import com.alibaba.dubbo.cache.CacheService;
import com.alibaba.dubbo.cache.exception.CacheException;

public class RedisCacheService implements CacheService {
	
	private boolean isConnected = false;
	private boolean isShutdown = false;
	
	//private static int MAX_EXPIRY_TIME = 30 * 24 * 3600;
	private static int MAX_EXPIRY_TIME = -1;
	
	private JedisPool pool;

	@Override
	public void connect(String address, int maxActive, int maxIdle,
			int minIdle, int defaultTimeout) throws CacheException {
		// address username:password@localhost:11211
		if (maxActive == -1) {
			maxActive = 20;
		}
		if (maxIdle == -1) {
			maxIdle = 10;
		}
		if (minIdle == -1) {
			minIdle = 5;
		}
		if (maxActive == -1) {
			maxActive = 20;
		}
		MAX_EXPIRY_TIME = defaultTimeout;
		try {
			if (address.indexOf("@") != -1) {
				String[] addr = address.split("@");
				String authStr = addr[0];
				String server = addr[1];
				String[] auth = authStr.split(":");
				//String user = auth[0];
				String password = auth[1];
				String[] ser = server.split(":");
				String host = ser[0];
				int port = Integer.parseInt(ser[1]);
				int maxWait = 60000;
				JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
				jedisPoolConfig.setMaxTotal(maxActive);
				jedisPoolConfig.setMaxIdle(maxIdle);
				jedisPoolConfig.setMaxWaitMillis(maxWait);
				jedisPoolConfig.setMinIdle(minIdle);
				/*jedisPoolConfig
						.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
				jedisPoolConfig
						.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);*/
				jedisPoolConfig.setNumTestsPerEvictionRun(2);
				jedisPoolConfig.setTestOnBorrow(false);
				jedisPoolConfig.setTestOnReturn(false);
				jedisPoolConfig.setTestWhileIdle(true);
				int database = 0;
				pool = new JedisPool(jedisPoolConfig, host, port, Protocol.DEFAULT_TIMEOUT, password, database);				
				isConnected = true;
			} else {
				String[] ser = address.split(":");
				String host = ser[0];
				int port = Integer.parseInt(ser[1]);
				int maxWait = 60000;
				JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
				jedisPoolConfig.setMaxTotal(maxActive);
				jedisPoolConfig.setMaxIdle(maxIdle);
				jedisPoolConfig.setMaxWaitMillis(maxWait);
				jedisPoolConfig.setMinIdle(minIdle);
				jedisPoolConfig.setNumTestsPerEvictionRun(2);
				jedisPoolConfig.setTestOnBorrow(false);
				jedisPoolConfig.setTestOnReturn(false);
				jedisPoolConfig.setTestWhileIdle(true);
				pool = new JedisPool(jedisPoolConfig, host, port);
				isConnected = true;
			}
		} catch (Exception e) {
			isConnected = false;
			throw new CacheException(e);
		}
	}
	
	@Override
	public boolean isConnected() {
		return isConnected;
	}
	
	@Override
	public boolean isShutdown() {
		return isShutdown;
	}
	
	@Override
	public void put(String key, Object value) throws CacheException {
		if (MAX_EXPIRY_TIME > 0) {
			put(key, value, MAX_EXPIRY_TIME);
		} else {
			Jedis jedis = null;
			try {
				jedis = pool.getResource();
				byte[] bytes = serialize(value);
				jedis.set(key.getBytes(), bytes);
			} catch (Exception e) {
				throw new CacheException(e);
			} finally {
				if (jedis != null) {
					jedis.close();
				}
			}
		}
	}
	
	@Override
	public void put(String key, Object value, int timeout) throws CacheException {
		//mc.set(key, timeout, value);//spymemcached expiry unit second
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			byte[] bytes = serialize(value);
			jedis.set(key.getBytes(), bytes);
			jedis.expire(key, timeout);
		} catch (Exception e) {
			throw new CacheException(e);
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}

	@Override
	public Object get(String key) throws CacheException {
		//return mc.get(key);
		Object value = null;
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			byte[] bytes = jedis.get(key.getBytes());
			if (bytes != null) {
				value = deserialize(bytes);
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new CacheException(e);
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
		return value;
	}

	@Override
	public void delete(String key) throws CacheException {
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			jedis.del(key);
		} catch (Exception e) {
			throw new CacheException(e);
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}
	
	@Override
	public void shutdown() {
		isShutdown = true;
		pool.close();
	}
	
	public static byte[] serialize(Object obj) throws IOException{
		if (obj == null) {
			throw new NullPointerException();
		}
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		HessianOutput ho = new HessianOutput(os);
		ho.writeObject(obj);
		return os.toByteArray();
	}

	public static Object deserialize(byte[] bytes) throws IOException{
		if (bytes == null) {
			throw new NullPointerException();
		}
		ByteArrayInputStream is = new ByteArrayInputStream(bytes);
		HessianInput hi = new HessianInput(is);
		return hi.readObject();
	}

}
