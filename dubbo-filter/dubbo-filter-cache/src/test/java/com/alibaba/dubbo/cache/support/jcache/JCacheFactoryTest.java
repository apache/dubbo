package com.alibaba.dubbo.cache.support.jcache;

import com.alibaba.dubbo.cache.Cache;
import com.alibaba.dubbo.cache.support.AbstractCacheFactory;
import com.alibaba.dubbo.cache.support.AbstractCacheFactoryTest;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class JCacheFactoryTest extends AbstractCacheFactoryTest {

    @Test
    public void testJCacheFactory() throws Exception {
        Cache cache = super.constructCache();
        assertThat(cache instanceof JCache, is(true));
    }

    @Override
    protected AbstractCacheFactory getCacheFactory() {
        return new JCacheFactory();
    }
}