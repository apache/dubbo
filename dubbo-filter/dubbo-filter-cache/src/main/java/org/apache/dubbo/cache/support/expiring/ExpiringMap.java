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
package org.apache.dubbo.cache.support.expiring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * can be expired map
 * Contains a background thread that periodically checks if the data is out of date
 */
public class ExpiringMap<K, V> implements Map<K, V> {

    /**
     * default time to live (second)
     */
    private static final int DEFAULT_TIME_TO_LIVE = 180;

    /**
     * default expire check interval (second)
     */
    private static final int DEFAULT_EXPIRATION_INTERVAL = 1;

    private static volatile int expireCount = 1;

    private final ConcurrentHashMap<K, ExpiryObject> delegateMap;

    private final ExpireThread expireThread;

    public ExpiringMap() {
        this(DEFAULT_TIME_TO_LIVE, DEFAULT_EXPIRATION_INTERVAL);
    }

    /**
     * Constructor
     *
     * @param timeToLive time to live (second)
     */
    public ExpiringMap(int timeToLive) {
        this(timeToLive, DEFAULT_EXPIRATION_INTERVAL);
    }

    public ExpiringMap(int timeToLive, int expirationInterval) {
        this(new ConcurrentHashMap<K, ExpiryObject>(), timeToLive, expirationInterval);
    }

    private ExpiringMap(ConcurrentHashMap<K, ExpiryObject> delegateMap, int timeToLive, int expirationInterval) {
        this.delegateMap = delegateMap;
        this.expireThread = new ExpireThread();
        expireThread.setTimeToLive(timeToLive);
        expireThread.setExpirationInterval(expirationInterval);
    }

    @Override
    public V put(K key, V value) {
        ExpiryObject answer = delegateMap.put(key, new ExpiryObject(key, value, System.currentTimeMillis()));
        if (answer == null) {
            return null;
        }
        return answer.getValue();
    }

    @Override
    public V get(Object key) {
        ExpiryObject object = delegateMap.get(key);
        if (object != null) {
            object.setLastAccessTime(System.currentTimeMillis());
            return object.getValue();
        }
        return null;
    }

    @Override
    public V remove(Object key) {
        ExpiryObject answer = delegateMap.remove(key);
        if (answer == null) {
            return null;
        }
        return answer.getValue();
    }

    @Override
    public boolean containsKey(Object key) {
        return delegateMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return delegateMap.containsValue(value);
    }

    @Override
    public int size() {
        return delegateMap.size();
    }

    @Override
    public boolean isEmpty() {
        return delegateMap.isEmpty();
    }

    @Override
    public void clear() {
        delegateMap.clear();
        expireThread.stopExpiring();
    }

    @Override
    public int hashCode() {
        return delegateMap.hashCode();
    }

    @Override
    public Set<K> keySet() {
        return delegateMap.keySet();
    }

    @Override
    public boolean equals(Object obj) {
        return delegateMap.equals(obj);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> inMap) {
        for (Entry<? extends K, ? extends V> e : inMap.entrySet()) {
            this.put(e.getKey(), e.getValue());
        }
    }

    @Override
    public Collection<V> values() {
        List<V> list = new ArrayList<V>();
        Set<Entry<K, ExpiryObject>> delegatedSet = delegateMap.entrySet();
        for (Entry<K, ExpiryObject> entry : delegatedSet) {
            ExpiryObject value = entry.getValue();
            list.add(value.getValue());
        }
        return list;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    public ExpireThread getExpireThread() {
        return expireThread;
    }

    public int getExpirationInterval() {
        return expireThread.getExpirationInterval();
    }

    public void setExpirationInterval(int expirationInterval) {
        expireThread.setExpirationInterval(expirationInterval);
    }

    public int getTimeToLive() {
        return expireThread.getTimeToLive();
    }

    public void setTimeToLive(int timeToLive) {
        expireThread.setTimeToLive(timeToLive);
    }

    @Override
    public String toString() {
        return "ExpiringMap{" +
                "delegateMap=" + delegateMap.toString() +
                ", expireThread=" + expireThread.toString() +
                '}';
    }

    /**
     * can be expired object
     */
    private class ExpiryObject {
        private K key;
        private V value;
        private AtomicLong lastAccessTime;

        ExpiryObject(K key, V value, long lastAccessTime) {
            if (value == null) {
                throw new IllegalArgumentException("An expiring object cannot be null.");
            }
            this.key = key;
            this.value = value;
            this.lastAccessTime = new AtomicLong(lastAccessTime);
        }

        public long getLastAccessTime() {
            return lastAccessTime.get();
        }

        public void setLastAccessTime(long lastAccessTime) {
            this.lastAccessTime.set(lastAccessTime);
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        @Override
        public boolean equals(Object obj) {
            return value.equals(obj);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public String toString() {
            return "ExpiryObject{" +
                    "key=" + key +
                    ", value=" + value +
                    ", lastAccessTime=" + lastAccessTime +
                    '}';
        }
    }

    /**
     * Background thread, periodically checking if the data is out of date
     */
    public class ExpireThread implements Runnable {
        private long timeToLiveMillis;
        private long expirationIntervalMillis;
        private volatile boolean running = false;
        private final Thread expirerThread;

        @Override
        public String toString() {
            return "ExpireThread{" +
                    ", timeToLiveMillis=" + timeToLiveMillis +
                    ", expirationIntervalMillis=" + expirationIntervalMillis +
                    ", running=" + running +
                    ", expirerThread=" + expirerThread +
                    '}';
        }

        public ExpireThread() {
            expirerThread = new Thread(this, "ExpiryMapExpire-" + expireCount++);
            expirerThread.setDaemon(true);
        }

        @Override
        public void run() {
            while (running) {
                processExpires();
                try {
                    Thread.sleep(expirationIntervalMillis);
                } catch (InterruptedException e) {
                    running = false;
                }
            }
        }

        private void processExpires() {
            long timeNow = System.currentTimeMillis();
            for (ExpiryObject o : delegateMap.values()) {
                if (timeToLiveMillis <= 0) {
                    continue;
                }
                long timeIdle = timeNow - o.getLastAccessTime();
                if (timeIdle >= timeToLiveMillis) {
                    delegateMap.remove(o.getKey());
                }
            }
        }

        /**
         * start expiring Thread
         */
        public void startExpiring() {
            if (!running) {
                running = true;
                expirerThread.start();
            }
        }

        /**
         * start thread
         */
        public void startExpiryIfNotStarted() {
            if (running) {
                return;
            }
            startExpiring();
        }

        /**
         * stop thread
         */
        public void stopExpiring() {
            if (running) {
                running = false;
                expirerThread.interrupt();
            }
        }

        /**
         * get thread state
         *
         * @return thread state
         */
        public boolean isRunning() {
            return running;
        }

        /**
         * get time to live
         *
         * @return time to live
         */
        public int getTimeToLive() {
            return (int) timeToLiveMillis / 1000;
        }

        /**
         * update time to live
         *
         * @param timeToLive time to live
         */
        public void setTimeToLive(long timeToLive) {
            this.timeToLiveMillis = timeToLive * 1000;
        }

        /**
         * get expiration interval
         *
         * @return expiration interval (second)
         */
        public int getExpirationInterval() {
            return (int) expirationIntervalMillis / 1000;
        }

        /**
         * set expiration interval
         *
         * @param expirationInterval expiration interval (second)
         */
        public void setExpirationInterval(long expirationInterval) {
            this.expirationIntervalMillis = expirationInterval * 1000;
        }
    }
}



