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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LFUCache<K, V> {

    private Map<K, CacheNode<K, V>> map;
    private Map<Long, CacheDeque<K, V>> freqTable;

    private final int capacity;
    private int evictionCount;
    private int curSize = 0;
    private long removeFreqEntryTimeout;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final int DEFAULT_INITIAL_CAPACITY = 1000;

    private static final float DEFAULT_EVICTION_FACTOR = 0.75f;

    private static final long DEFAULT_REMOVE_FREQ_TABLE_TIME_OUT = 1800000L;

    public LFUCache() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_EVICTION_FACTOR, DEFAULT_REMOVE_FREQ_TABLE_TIME_OUT);
    }

    /**
     * Constructs and initializes cache with specified capacity and eviction
     * factor. Unacceptable parameter values followed with
     * {@link IllegalArgumentException}.
     *
     * @param maxCapacity    cache max capacity
     * @param evictionFactor cache proceedEviction factor
     */
    @SuppressWarnings("unchecked")
    public LFUCache(final int maxCapacity, final float evictionFactor) {
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " +
                    maxCapacity);
        }
        boolean factorInRange = evictionFactor <= 1 && evictionFactor > 0;
        if (!factorInRange || Float.isNaN(evictionFactor)) {
            throw new IllegalArgumentException("Illegal eviction factor value:"
                    + evictionFactor);
        }
        this.capacity = maxCapacity;
        this.evictionCount = (int) (capacity * evictionFactor);
        this.map = new HashMap<>();
        this.freqTable = new TreeMap<>(Long::compareTo);
        freqTable.put(1L, new CacheDeque<>());
    }

    /**
     * Constructs and initializes cache with specified capacity and eviction
     * factor. Unacceptable parameter values followed with
     * {@link IllegalArgumentException}.
     *
     * @param maxCapacity    cache max capacity
     * @param evictionFactor cache proceedEviction factor
     * @param removeFreqEntryTimeout cache queue remove timeout
     */
    @SuppressWarnings("unchecked")
    public LFUCache(final int maxCapacity, final float evictionFactor, final long removeFreqEntryTimeout) {
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " +
                    maxCapacity);
        }
        boolean factorInRange = evictionFactor <= 1 && evictionFactor > 0;
        if (!factorInRange || Float.isNaN(evictionFactor)) {
            throw new IllegalArgumentException("Illegal eviction factor value:"
                    + evictionFactor);
        }
        this.capacity = maxCapacity;
        this.evictionCount = (int) (capacity * evictionFactor);
        this.removeFreqEntryTimeout = removeFreqEntryTimeout;
        this.map = new HashMap<>();
        this.freqTable = new TreeMap<>(Long::compareTo);
        freqTable.put(1L, new CacheDeque<>());
    }

    public int getCapacity() {
        return capacity;
    }

    public V put(final K key, final V value) {
        CacheNode<K, V> node;
        lock.writeLock().lock();
        try {
            node = map.get(key);
            if (node != null) {
                CacheNode.withdrawNode(node);
                node.value = value;
                moveToNextFreqQueue(node.incrFreq(), node);
                map.put(key, node);
            } else {
                if (curSize + 1 > capacity) {
                    proceedEviction();
                }
                node = freqTable.get(1L).addLast(key, value);
                map.put(key, node);
                curSize++;
            }
        } finally {
            lock.writeLock().unlock();
        }
        return node.value;
    }

    public V remove(final K key) {
        CacheNode<K, V> node = null;
        lock.writeLock().lock();
        try {
            if (map.containsKey(key)) {
                node = map.remove(key);
                if (node != null) {
                    CacheNode.withdrawNode(node);
                }
                curSize--;
            }
        } finally {
            lock.writeLock().unlock();
        }
        return (node != null) ? node.value : null;
    }

    public V get(final K key) {
        CacheNode<K, V> node = null;
        lock.writeLock().lock();
        try {
            if (map.containsKey(key)) {
                node = map.get(key);
                CacheNode.withdrawNode(node);
                moveToNextFreqQueue(node.incrFreq(), node);
            }
        } finally {
            lock.writeLock().unlock();
        }
        return (node != null) ? node.value : null;
    }

    /**
     * Returns size of the freq table
     *
     * @return size
     */
    public int getFreqTableSize(){
        return freqTable.size();
    }

    /**
     * Returns freq of the element
     *
     * @return freq
     */
    public Long getFreq(final K key) {
        CacheNode<K, V> node = null;
        lock.readLock().lock();
        try {
            if (map.containsKey(key)) {
                node = map.get(key);
                return node.getFreq();
            }
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    /**
     * Returns node list of this frequency
     *
     * @return node list
     */
    private List<CacheNode<K,V>> getFreqList(final Long freq){
        if(freq == null){
            return null;
        }
        lock.writeLock().lock();
        try {
            if (freqTable.containsKey(freq)) {
                if(freqTable.get(freq).nodeMap.size() > 0){
                    return new ArrayList<>(freqTable.get(freq).nodeMap.values());
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
        return null;
    }

    /**
     * Returns node list's size of this frequency
     *
     * @return node list's size
     */
    public int getFreqListSize(final Long freq){
        if(freq == null){
            return 0;
        }
        lock.writeLock().lock();
        try {
            if (freqTable.containsKey(freq)) {
                return freqTable.get(freq).size.get();
            }
        } finally {
            lock.writeLock().unlock();
        }
        return 0;
    }

    /**
     * Evicts less frequently used elements corresponding to eviction factor,
     * specified at instantiation step.
     *
     * @return number of evicted elements
     */
    private int proceedEviction() {
        int targetSize = capacity - evictionCount - 1;
        int evictedElements = 0;
        Set<Long> freqKeys = freqTable.keySet();
        boolean evictionEnd = false;
        for (Long freq : freqKeys) {
            CacheDeque<K, V> q = freqTable.get(freq);
            CacheNode<K, V> node;
            if(!evictionEnd) {
                while (!q.isEmpty()) {
                    node = q.pollFirst();
                    remove(node.key);
                    evictedElements++;
                    if (targetSize >= curSize) {
                        evictionEnd = true;
                        break;
                    }
                }
            }
            // If the queue is empty for a long time, delete the queue
            if (removeFreqEntryTimeout > 0 && freq > 1 && q.isEmpty() && (System.currentTimeMillis() - q.getLastReqTime()) >= removeFreqEntryTimeout) {
                freqTable.remove(freq);
            }
        }
        return evictedElements;
    }

    /**
     * Move the node to the next cache queue
     */
    private void moveToNextFreqQueue(long newFreq, CacheNode<K, V> node){
        freqTable.putIfAbsent(newFreq, new CacheDeque<>());
        freqTable.get(newFreq).addLastNode(node);
    }

    /**
     * Returns cache current size.
     *
     * @return cache size
     */
    public int getSize() {
        return curSize;
    }

    static class CacheNode<K, V> {

        CacheNode<K, V> prev;
        CacheNode<K, V> next;
        K key;
        V value;
        volatile AtomicLong freq = new AtomicLong(1);
        CacheDeque<K, V> owner;

        CacheNode() {
        }

        CacheNode(final K key, final V value) {
            this.key = key;
            this.value = value;
        }

        long incrFreq(){
            return freq.incrementAndGet();
        }

        long getFreq(){
            return freq.get();
        }

        /**
         * This method takes specified node and reattaches it neighbors nodes
         * links to each other, so specified node will no longer tied with them.
         * Returns united node, returns null if argument is null.
         *
         * @param node note to retrieve
         * @param <K>  key
         * @param <V>  value
         * @return retrieved node
         */
        static <K, V> CacheNode<K, V> withdrawNode(
                final CacheNode<K, V> node) {
            if (node != null && node.prev != null) {
                node.prev.next = node.next;
                if (node.next != null) {
                    node.next.prev = node.prev;
                    node.owner.nodeMap.remove(node.key);
                    node.owner.size.decrementAndGet();
                }
            }
            return node;
        }

    }

    /**
     * Custom deque implementation of LIFO type. Allows to place element at top
     * of deque and poll very last added elements. An arbitrary node from the
     * deque can be removed with {@link CacheNode#withdrawNode(CacheNode)}
     * method.
     *
     * @param <K> key
     * @param <V> value
     */
    static class CacheDeque<K, V> {

        CacheNode<K, V> last;
        CacheNode<K, V> first;
        Map<K, CacheNode<K, V>> nodeMap;
        long lastReqTime;
        volatile AtomicInteger size = new AtomicInteger(0);
        /**
         * Constructs list and initializes last and first pointers.
         */
        CacheDeque() {
            last = new CacheNode<>();
            first = new CacheNode<>();
            last.next = first;
            first.prev = last;
            nodeMap = new HashMap<>();
        }

        /**
         * Puts the node with specified key and value at the end of the deque
         * and returns node.
         *
         * @param key   key
         * @param value value
         * @return added node
         */
        CacheNode<K, V> addLast(final K key, final V value) {
            CacheNode<K, V> node = new CacheNode<>(key, value);
            node.owner = this;
            node.next = last.next;
            node.prev = last;
            node.next.prev = node;
            last.next = node;
            this.setLastReqTime(System.currentTimeMillis());
            this.size.incrementAndGet();
            return node;
        }

        CacheNode<K, V> addLastNode(final CacheNode<K, V> node) {
            node.owner = this;
            node.next = last.next;
            node.prev = last;
            node.next.prev = node;
            last.next = node;
            this.setLastReqTime(System.currentTimeMillis());
            this.nodeMap.put(node.key, node);
            this.size.incrementAndGet();
            return node;
        }

        /**
         * Retrieves and removes the first node of this deque.
         *
         * @return removed node
         */
        CacheNode<K, V> pollFirst() {
            CacheNode<K, V> node = null;
            if (first.prev != last) {
                node = first.prev;
                first.prev = node.prev;
                first.prev.next = first;
                node.prev = null;
                node.next = null;
                this.nodeMap.remove(node.key);
                this.size.decrementAndGet();
            }
            return node;
        }

        /**
         * Checks if link to the last node points to link to the first node.
         *
         * @return is deque empty
         */
        boolean isEmpty() {
            return last.next == first;
        }

        public CacheDeque<K, V> setLastReqTime(long lastReqTime) {
            this.lastReqTime = lastReqTime;
            return this;
        }

        public long getLastReqTime() {
            return lastReqTime;
        }
    }
}