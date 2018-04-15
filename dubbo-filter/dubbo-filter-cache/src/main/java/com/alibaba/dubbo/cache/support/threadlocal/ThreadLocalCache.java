/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.cache.support.threadlocal;

import com.alibaba.dubbo.cache.Cache;
import com.alibaba.dubbo.common.URL;

import java.util.HashMap;
import java.util.Map;

/**
 * ThreadLocalCache
 *
 * 当前线程缓存，比如一个页面渲染，用到很多 portal，每个 portal 都要去查用户信息，通过线程缓存，可以减少这种多余访问。
 */
public class ThreadLocalCache implements Cache {

    private final ThreadLocal<Map<Object, Object>> store; // 线程变量

    public ThreadLocalCache(URL url) {
        this.store = new ThreadLocal<Map<Object, Object>>() {

            @Override
            protected Map<Object, Object> initialValue() {
                return new HashMap<Object, Object>();
            }

        };
    }

    @Override
    public void put(Object key, Object value) {
        store.get().put(key, value);
    }

    @Override
    public Object get(Object key) {
        return store.get().get(key);
    }

}