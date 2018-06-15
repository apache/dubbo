package org.apache.dubbo.compatible.cache;

import com.alibaba.dubbo.cache.Cache;
import com.alibaba.dubbo.cache.support.AbstractCacheFactory;
import com.alibaba.dubbo.common.URL;

public class MyCacheFactory extends AbstractCacheFactory {

    @Override
    protected Cache createCache(URL url) {
        return new MyCache(url);
    }
}
