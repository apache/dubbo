package com.alibaba.dubbo.cache.support.expiry;

import com.alibaba.dubbo.cache.Cache;
import com.alibaba.dubbo.cache.support.AbstractCacheFactory;
import com.alibaba.dubbo.common.URL;

/**
 * ExpiryCacheFactory
 */
public class ExpiryCacheFactory extends AbstractCacheFactory {
    
    @Override
    protected Cache createCache(URL url) {
        return new ExpiryCache(url);
    }
}
