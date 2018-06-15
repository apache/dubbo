package org.apache.dubbo.compatible.cache;

import com.alibaba.dubbo.cache.Cache;
import com.alibaba.dubbo.common.URL;

import java.util.HashMap;
import java.util.Map;

public class MyCache implements Cache {

    private Map<Object, Object> map = new HashMap<Object, Object>();

    public MyCache(URL url) {
    }

    @Override
    public void put(Object key, Object value) {
        map.put(key, value);
    }

    @Override
    public Object get(Object key) {
        return map.get(key);
    }
}
