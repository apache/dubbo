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
package org.apache.dubbo.common.utils;

import java.util.LinkedHashMap;
import java.util.concurrent.Semaphore;

public class LRUCache<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = -5167631809472116969L;

    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private static final int DEFAULT_MAX_CAPACITY = 1000;

    private volatile int maxCapacity;

    private final Semaphore lock = new Semaphore(Integer.MAX_VALUE, true);

    public LRUCache() {
        this(DEFAULT_MAX_CAPACITY);
    }

    public LRUCache(int maxCapacity) {
        super(16, DEFAULT_LOAD_FACTOR, true);
        this.maxCapacity = maxCapacity;
    }

    @Override
    protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
        return super.size() > maxCapacity;
    }

    @Override
    public boolean containsKey(Object key) {
        lock.acquireUninterruptibly();
        try {
            return super.containsKey(key);
        } finally {
            lock.release();
        }
    }

    @Override
    public V get(Object key) {
        lock.acquireUninterruptibly();
        try {
            return super.get(key);
        } finally {
            lock.release();
        }
    }

    @Override
    public V put(K key, V value) {
        lock.acquireUninterruptibly(Integer.MAX_VALUE);
        try {
            return super.put(key, value);
        } finally {
            lock.release(Integer.MAX_VALUE);
        }
    }

    @Override
    public V remove(Object key) {
        lock.acquireUninterruptibly(Integer.MAX_VALUE);
        try {
            return super.remove(key);
        } finally {
            lock.release(Integer.MAX_VALUE);
        }
    }

    @Override
    public int size() {
        lock.acquireUninterruptibly();
        try {
            return super.size();
        } finally {
            lock.release();
        }
    }

    @Override
    public void clear() {
        lock.acquireUninterruptibly(Integer.MAX_VALUE);
        try {
            super.clear();
        } finally {
            lock.release(Integer.MAX_VALUE);
        }
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

}