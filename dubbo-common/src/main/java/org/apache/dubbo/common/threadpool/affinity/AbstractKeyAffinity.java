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
package org.apache.dubbo.common.threadpool.affinity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author earthchen
 * @date 2021/10/2
 **/
public abstract class AbstractKeyAffinity<K, V> implements KeyAffinity<K, V> {

    private final Map<K, KeyRef> mapping = new ConcurrentHashMap<>();

    private final List<ValueRef> all;

    private final int count;

    protected void putVal(K key, KeyRef ref) {
        mapping.put(key, ref);
    }

    protected KeyRef genRandomKeyRef() {
        return new KeyRef(all.get(ThreadLocalRandom.current().nextInt(count)));
    }

    public Map<K, KeyRef> getMapping() {
        return mapping;
    }

    public List<ValueRef> getAll() {
        return all;
    }

    @Override
    public List<V> getAllVal() {
        return all.stream().map(it -> it.obj).collect(Collectors.toList());
    }

    public int getCount() {
        return count;
    }

    public AbstractKeyAffinity(Supplier<V> v, int count) {
        this.count = count;
        this.all = getAll(v, count);
    }

    private List<ValueRef> getAll(Supplier<V> v, int count) {
        List<ValueRef> all = new CopyOnWriteArrayList<>();
        for (int i = 0; i < count; i++) {
            all.add(new ValueRef(v.get()));
        }
        return all;
    }

    @Override
    public V select(K key, boolean create) {
        KeyRef ref = mapping.get(key);
        if (ref == null) {
            if (!create) {
                return null;
            }
            ref = selectKeyIfNotExists(key);
        }
        ref.incrConcurrency();
        return ref.ref();
    }

    abstract KeyRef selectKeyIfNotExists(K key);

    @Override
    public void finishCall(K key) {
        KeyRef ref = mapping.get(key);
        if (ref != null) {
            if (ref.decrConcurrency()) {
                mapping.remove(key);
            }
        }
    }

    protected class KeyRef {

        private final ValueRef valueRef;
        private final AtomicInteger concurrency = new AtomicInteger();

        KeyRef(ValueRef valueRef) {
            this.valueRef = valueRef;
        }

        void incrConcurrency() {
            concurrency.incrementAndGet();
            valueRef.concurrency.incrementAndGet();
        }

        /**
         * @return {@code true} if no ref by key
         */
        boolean decrConcurrency() {
            int r = concurrency.decrementAndGet();
            int refConcurrency = valueRef.concurrency.decrementAndGet();
            if (refConcurrency < 0) {
                synchronized (all) {
                    all.notifyAll();
                }
            }
            return r <= 0;
        }

        V ref() {
            return valueRef.obj;
        }
    }

    protected class ValueRef {

        private final V obj;
        private final AtomicInteger concurrency = new AtomicInteger();

        ValueRef(V obj) {
            this.obj = obj;
        }

        int concurrency() {
            return concurrency.get();
        }
    }
}
