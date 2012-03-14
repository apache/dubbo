/*
 * Copyright 1999-2012 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.cache.support.threadlocal;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.dubbo.cache.Cache;
import com.alibaba.dubbo.common.URL;

/**
 * ThreadLocalCache
 * 
 * @author william.liangf
 */
public class ThreadLocalCache implements Cache {

    private final ThreadLocal<Map<Object, Object>> store;

    public ThreadLocalCache(URL url) {
        this.store = new ThreadLocal<Map<Object, Object>>() {
            @Override
            protected Map<Object, Object> initialValue() {
                return new HashMap<Object, Object>();
            }
        };
    }

    public void put(Object key, Object value) {
        store.get().put(key, value);
    }

    public Object get(Object key) {
        return store.get().get(key);
    }

}
