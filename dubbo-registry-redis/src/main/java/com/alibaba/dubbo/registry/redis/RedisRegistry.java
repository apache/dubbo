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
package com.alibaba.dubbo.registry.redis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool.impl.GenericObjectPool;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.support.FailbackRegistry;

/**
 * RedisRegistry
 * 
 * @author william.liangf
 */
public class RedisRegistry extends FailbackRegistry {

    private static final Logger logger = LoggerFactory.getLogger(RedisRegistry.class);

    private static final int DEFAULT_REDIS_PORT = 6379;

    private final static String DEFAULT_ROOT = "dubbo";

    private final ScheduledExecutorService expireExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("DubboRegistryExpireTimer", true));

    private final ScheduledFuture<?> expireFuture;
    
    private final String root;

    private final JedisPool jedisPool;

    private final NotifySub sub = new NotifySub();
    
    private final ConcurrentMap<String, Notifier> notifiers = new ConcurrentHashMap<String, Notifier>();
    
    private final int reconnectPeriod;

    private final int expirePeriod;

    public RedisRegistry(URL url) {
        super(url);
        GenericObjectPool.Config config = new GenericObjectPool.Config();
        config.testOnBorrow = url.getParameter("test.on.borrow", true);
        config.testOnReturn = url.getParameter("test.on.return", false);
        config.testWhileIdle = url.getParameter("test.while.idle", false);
        if (url.getParameter("max.idle", 0) > 0)
            config.maxIdle = url.getParameter("max.idle", 0);
        if (url.getParameter("min.idle", 0) > 0)
            config.minIdle = url.getParameter("min.idle", 0);
        if (url.getParameter("max.active", 0) > 0)
            config.maxActive = url.getParameter("max.active", 0);
        if (url.getParameter("max.wait", 0) > 0)
            config.maxWait = url.getParameter("max.wait", 0);
        if (url.getParameter("num.tests.per.eviction.run", 0) > 0)
            config.numTestsPerEvictionRun = url.getParameter("num.tests.per.eviction.run", 0);
        if (url.getParameter("time.between.eviction.runs.millis", 0) > 0)
            config.timeBetweenEvictionRunsMillis = url.getParameter("time.between.eviction.runs.millis", 0);
        if (url.getParameter("min.evictable.idle.time.millis", 0) > 0)
            config.minEvictableIdleTimeMillis = url.getParameter("min.evictable.idle.time.millis", 0);
        
        this.jedisPool = new JedisPool(config, url.getHost(), 
                url.getPort() == 0 ? DEFAULT_REDIS_PORT : url.getPort(), 
                url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT));
        this.reconnectPeriod = url.getParameter(Constants.REGISTRY_RECONNECT_PERIOD_KEY, Constants.DEFAULT_REGISTRY_RECONNECT_PERIOD);
        String group = url.getParameter(Constants.GROUP_KEY, DEFAULT_ROOT);
        if (! group.startsWith(Constants.PATH_SEPARATOR)) {
            group = Constants.PATH_SEPARATOR + group;
        }
        if (! group.endsWith(Constants.PATH_SEPARATOR)) {
            group = group + Constants.PATH_SEPARATOR;
        }
        this.root = group;
        
        this.expirePeriod = url.getParameter(Constants.EXPIRE_KEY, Constants.DEFAULT_EXPIRE);
        this.expireFuture = expireExecutor.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                try {
                    deferExpired(); // 延长过期时间
                } catch (Throwable t) { // 防御性容错
                    logger.error("Unexpected error occur at defer expire time, cause: " + t.getMessage(), t);
                }
            }
        }, expirePeriod, expirePeriod, TimeUnit.MILLISECONDS);
    }
    
    private void deferExpired() {
        Jedis jedis = jedisPool.getResource();
        try {
            for (String provider : new HashSet<String>(getRegistered())) {
                jedis.hset(toRegisterPath(URL.valueOf(provider)), provider, String.valueOf(System.currentTimeMillis() + expirePeriod));
            }
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    public boolean isAvailable() {
        try {
            Jedis jedis = jedisPool.getResource();
            try {
                return jedis.isConnected();
            } finally {
                jedisPool.returnResource(jedis);
            }
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            expireFuture.cancel(true);
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
        try {
            for (Notifier notifier : notifiers.values()) {
                notifier.shutdown();
            }
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
        try {
            jedisPool.destroy();
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
    }

    @Override
    public void doRegister(URL url) {
        Jedis jedis = jedisPool.getResource();
        try {
            jedis.hset(toRegisterPath(url), url.toFullString(), String.valueOf(System.currentTimeMillis() + expirePeriod));
            jedis.publish(toServicePath(url), Constants.REGISTER);
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    @Override
    public void doUnregister(URL url) {
        Jedis jedis = jedisPool.getResource();
        try {
            jedis.hdel(toRegisterPath(url), url.toFullString());
            jedis.publish(toServicePath(url), Constants.UNREGISTER);
        } finally {
            jedisPool.returnResource(jedis);
        }
    }
    
    private class Notifier extends Thread {

        private final String service;

        private volatile Jedis jedis;

        private volatile boolean running = true;
        
        private final AtomicInteger connectSkip = new AtomicInteger();

        private final AtomicInteger connectSkiped = new AtomicInteger();

        private final Random random = new Random();
        
        private volatile int connectRandom;

        private void resetSkip() {
            connectSkip.set(0);
            connectSkiped.set(0);
            connectRandom = 0;
        }
        
        private boolean isSkip() {
            int skip = connectSkip.get(); // 跳过次数增长
            if (skip >= 10) { // 如果跳过次数增长超过10，取随机数
                if (connectRandom == 0) {
                    connectRandom = random.nextInt(10);
                }
                skip = 10 + connectRandom;
            }
            if (connectSkiped.getAndIncrement() < skip) { // 检查跳过次数
                return true;
            }
            connectSkip.incrementAndGet();
            connectSkiped.set(0);
            connectRandom = 0;
            return false;
        }
        
        public Notifier(String service) {
            super.setDaemon(true);
            super.setName("DubboRedisSubscribe");
            this.service = service;
        }
        
        @Override
        public void run() {
            while (running) {
                try {
                    if (! isSkip()) {
                        try {
                            jedis = jedisPool.getResource();
                            try {
                                if (service.endsWith(Constants.ANY_VALUE)) {
                                    for (String s : getServices(jedis, service)) {
                                        doNotify(jedis, s, false);
                                    }
                                    resetSkip();
                                    jedis.psubscribe(sub, service);
                                } else {
                                    doNotify(jedis, service, false);
                                    resetSkip();
                                    jedis.subscribe(sub, service);
                                }
                            } finally {
                                jedisPool.returnBrokenResource(jedis);
                            }
                        } catch (Throwable t) {
                            logger.error(t.getMessage(), t);
                            sleep(reconnectPeriod);
                        }
                    }
                } catch (Throwable t) {
                    logger.error(t.getMessage(), t);
                }
            }
        }
        
        public void shutdown() {
            try {
                running = false;
                jedis.disconnect();
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }
        }
        
    }
    
    @Override
    public void doSubscribe(final URL url, final NotifyListener listener) {
        String service = toServicePath(url);
        Notifier notifier = notifiers.get(service);
        if (notifier == null) {
            Notifier newNotifier = new Notifier(service);
            notifiers.putIfAbsent(service, newNotifier);
            notifier = notifiers.get(service);
            if (notifier == newNotifier) {
                notifier.start();
            }
        }
        Jedis jedis = jedisPool.getResource();
        try {
            if (service.endsWith(Constants.ANY_VALUE)) {
                for (String s : getServices(jedis, service)) {
                    doNotify(jedis, s, url, listener);
                }
            } else {
                jedis.hset(toSubscribePath(url), url.toFullString(), String.valueOf(System.currentTimeMillis() + expirePeriod));
                jedis.publish(toServicePath(url), Constants.SUBSCRIBE);
                doNotify(jedis, service, url, listener);
            }
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        if (! Constants.ANY_VALUE.equals(url.getServiceInterface())) {
            Jedis jedis = jedisPool.getResource();
            try {
                jedis.hdel(toSubscribePath(url), url.toFullString());
                jedis.publish(toServicePath(url), Constants.UNSUBSCRIBE);
            } finally {
                jedisPool.returnResource(jedis);
            }
        }
    }

    private Set<String> getServices(Jedis jedis, String service) {
        Set<String> keys = jedis.keys(service);
        Set<String> services = new HashSet<String>();
        if (keys != null && keys.size() > 0) {
            for (String key : keys) {
                int i = key.indexOf(Constants.PATH_SEPARATOR, root.length());
                services.add(i > 0 ? key.substring(0, i) : key);
            }
        }
        return services;
    }

    private void doNotify(Jedis jedis, String service, boolean consumer) {
        for (Map.Entry<String, Set<NotifyListener>> entry : new HashMap<String, Set<NotifyListener>>(getSubscribed()).entrySet()) {
            URL url = URL.valueOf(entry.getKey());
            if (Constants.ANY_VALUE.equals(url.getServiceInterface()) 
                    || (! consumer && toServicePath(url).equals(service))) {
                doNotify(jedis, service, url, new HashSet<NotifyListener>(entry.getValue()));
            }
        }
    }
    
    private void doNotify(Jedis jedis, String service, URL url, NotifyListener listener) {
        doNotify(jedis, service, url, Arrays.asList(listener));
    }

    private void doNotify(Jedis jedis, String service, URL url, Collection<NotifyListener> listeners) {
        Map<String, String> providers;
        providers = jedis.hgetAll(service + Constants.PATH_SEPARATOR + Constants.PROVIDERS);
        if (url.getParameter(Constants.ADMIN_KEY, false)) {
            Map<String, String> consumers = jedis.hgetAll(service + Constants.PATH_SEPARATOR + Constants.CONSUMERS);
            if (consumers != null && consumers.size() > 0) {
                providers = providers == null ? new HashMap<String, String>() : new HashMap<String, String>(providers);
                providers.putAll(consumers);
            }
        }
        if (logger.isInfoEnabled()) {
            logger.info("redis notify: " + service + " = " + providers);
        }
        
        List<URL> urls = new ArrayList<URL>();
        if (providers != null && providers.size() > 0) {
            long now = System.currentTimeMillis();
            for (Map.Entry<String, String> entry : providers.entrySet()) {
                if (Long.parseLong(entry.getValue()) > now) {
                    URL u = URL.valueOf(entry.getKey());
                    if (UrlUtils.isMatch(url, u)) {
                        urls.add(u);
                    }
                }
            }
        }
        if (urls != null && urls.isEmpty() && url.getParameter(Constants.ADMIN_KEY, false)) {
            URL empty = url.setProtocol(Constants.EMPTY_PROTOCOL);
            if (Constants.ANY_VALUE.equals(empty.getServiceInterface())) {
                empty = empty.setServiceInterface(service.substring(root.length()));
            }
            urls.add(empty);
        }
        for (NotifyListener listener : listeners) {
            notify(url, listener, urls);
        }
    }

    private String toServicePath(URL url) {
        String name = url.getServiceInterface();
        if (! Constants.ANY_VALUE.equals(name)) {
            name = URL.encode(name);
        }
        return root + name;
    }

    private String toRegisterPath(URL url) {
        return toServicePath(url) + Constants.PATH_SEPARATOR + Constants.PROVIDERS;
    }

    private String toSubscribePath(URL url) {
        return toServicePath(url) + Constants.PATH_SEPARATOR + Constants.CONSUMERS;
    }

    private class NotifySub extends JedisPubSub {

        @Override
        public void onMessage(String key, String msg) {
            if (logger.isInfoEnabled()) {
                logger.info("redis event: " + key + " = " + msg);
            }
            if (msg.equals(Constants.REGISTER) 
                    || msg.equals(Constants.UNREGISTER)
                    || msg.equals(Constants.SUBSCRIBE) 
                    || msg.equals(Constants.UNSUBSCRIBE)) {
                Jedis jedis = jedisPool.getResource();
                try {
                    doNotify(jedis, key, msg.equals(Constants.SUBSCRIBE) || msg.equals(Constants.UNSUBSCRIBE));
                } finally {
                    jedisPool.returnResource(jedis);
                }
            }
        }

        @Override
        public void onPMessage(String pattern, String key, String msg) {
            onMessage(key, msg);
        }

        @Override
        public void onSubscribe(String key, int num) {
        }

        @Override
        public void onPSubscribe(String pattern, int num) {
        }

        @Override
        public void onUnsubscribe(String key, int num) {
        }

        @Override
        public void onPUnsubscribe(String pattern, int num) {
        }

    }

}
