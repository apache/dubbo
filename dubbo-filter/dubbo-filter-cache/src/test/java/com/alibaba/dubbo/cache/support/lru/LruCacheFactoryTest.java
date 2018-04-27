package com.alibaba.dubbo.cache.support.lru;

import com.alibaba.dubbo.cache.Cache;
import com.alibaba.dubbo.cache.support.AbstractCacheFactory;
import com.alibaba.dubbo.cache.support.AbstractCacheFactoryTest;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class LruCacheFactoryTest extends AbstractCacheFactoryTest{
    @Test
    public void testLruCacheFactory() throws Exception {
        Cache cache = super.constructCache();
        assertThat(cache instanceof LruCache, is(true));
    }

    @Override
    protected AbstractCacheFactory getCacheFactory() {
        return new LruCacheFactory();
    }
}