package com.alibaba.dubbo.cache.support.threadlocal;

import com.alibaba.dubbo.cache.Cache;
import com.alibaba.dubbo.cache.support.AbstractCacheFactory;
import com.alibaba.dubbo.cache.support.AbstractCacheFactoryTest;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ThreadLocalCacheFactoryTest extends AbstractCacheFactoryTest {
    @Test
    public void testThreadLocalCacheFactory() throws Exception {
        Cache cache = super.constructCache();
        assertThat(cache instanceof ThreadLocalCache, is(true));
    }

    @Override
    protected AbstractCacheFactory getCacheFactory() {
        return new ThreadLocalCacheFactory();
    }
}