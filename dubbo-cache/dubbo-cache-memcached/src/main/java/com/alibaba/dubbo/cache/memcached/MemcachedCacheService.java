package com.alibaba.dubbo.cache.memcached;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.auth.PlainCallbackHandler;
import net.spy.memcached.internal.OperationFuture;

import com.alibaba.dubbo.cache.CacheService;
import com.alibaba.dubbo.cache.exception.CacheException;

public class MemcachedCacheService implements CacheService {

	private MemcachedClient mc = null;
	
	private boolean isConnected = false;
	private boolean isShutdown = false;
	
	private static int MAX_EXPIRY_TIME = -1;

	@Override
	public void connect(String address, int maxActive, int maxIdle,
			int minIdle, int defaultTimeout) throws CacheException {
		// address username:password@localhost:11211
		MAX_EXPIRY_TIME = defaultTimeout;
		try {
			if (address.indexOf("@") != -1) {
				String[] addr = address.split("@");
				String authStr = addr[0];
				String serverList = addr[1];
				String[] auth = authStr.split(":");
				String user = auth[0];
				String password = auth[1];
				// 指定验证机制，推荐PLAIN，
				// 部分客户端存在协议BUG，只能使用PLAIN协议(PlainCallbackHandler)
				AuthDescriptor ad = new AuthDescriptor(new String[] { "PLAIN" },
						new PlainCallbackHandler(user, password)); // 用户名，密码
				mc = new MemcachedClient(new ConnectionFactoryBuilder()
						.setProtocol(Protocol.BINARY) // 指定使用Binary协议
						// .setOpTimeout(100)// 设置超时时间为100ms, 默认2.5s
						.setAuthDescriptor(ad).build(),
						AddrUtil.getAddresses(serverList)); // 访问地址
				isConnected = true;
			} else {
				mc = new MemcachedClient(AddrUtil.getAddresses(address));
				isConnected = true;
			}
			OperationFuture<Boolean> future = mc.set("_check_cache_", 3600, "yes");
			future.get();
			Object obj = mc.get("_check_cache_");
			if (obj == null) {
				throw new CacheException("Memcached client start up failed! Cause can not get object which setted before!");
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
			put(key, value, 0);
		}
	}
	
	@Override
	public void put(String key, Object value, int timeout) throws CacheException {
		try {
			mc.set(key, timeout, value);//spymemcached expiry unit second
		} catch (Exception e) {
			throw new CacheException(e);
		}
	}

	@Override
	public Object get(String key) throws CacheException {
		try {
			return mc.get(key);
		} catch (Exception e) {
			throw new CacheException(e);
		}
	}

	@Override
	public void delete(String key) throws CacheException {
		try {
			mc.delete(key);
		} catch (Exception e) {
			throw new CacheException(e);
		}
	}
	
	@Override
	public void shutdown() {
		isShutdown = true;
		mc.shutdown();
	}

	public static void main(String[] args) {
		String str = "memcached://127.0.0.1:11211";
		String str1 = str.substring(0, str.indexOf("://"));
		String str2 = str.substring(str.indexOf("://") + 3);
		System.out.println(str1);
		System.out.println(str2);

		// String address = "127.0.0.1:11211";
		/*String address = "mm:pdss@127.0.0.1:11211";
		MemcachedCacheService memcached = new MemcachedCacheService();
		*/
	}

}
