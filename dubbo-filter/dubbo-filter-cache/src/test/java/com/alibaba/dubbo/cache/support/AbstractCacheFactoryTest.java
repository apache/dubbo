package com.alibaba.dubbo.cache.support;

import com.alibaba.dubbo.cache.Cache;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.RpcInvocation;

public abstract class AbstractCacheFactoryTest {

    protected Cache constructCache() {
        URL url = URL.valueOf("test://test:11/test?cache=jcache");
        Invocation invocation = new RpcInvocation();
        return getCacheFactory().getCache(url, invocation);
    }

    protected abstract AbstractCacheFactory getCacheFactory();
}