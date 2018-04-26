package com.alibaba.dubbo.cache.support.expiry;

import com.alibaba.dubbo.cache.Cache;
import com.alibaba.dubbo.common.URL;

import java.util.Map;

/**
 * ExpiryCache - With the characteristic of expiration time .
 */
public class ExpiryCache implements Cache {
    private final Map<Object, Object> store;

    public ExpiryCache(URL url) {
        // 缓存时间
        final int secondsToLive = url.getParameter("cache.seconds", 180);
        // 缓存检查的时间间隔
        final int intervalSeconds = url.getParameter("cache.interval", 1);
        ExpiryMap<Object, Object> expiryMap = new ExpiryMap<Object, Object>(secondsToLive, intervalSeconds);
        expiryMap.getExpireThread().startExpiryIfNotStarted();
        this.store = expiryMap;
    }

    @Override
    public void put(Object key, Object value) {
        store.put(key, value);
    }

    @Override
    public Object get(Object key) {
        return store.get(key);
    }
}
