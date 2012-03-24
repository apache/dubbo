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
import java.util.Date;
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
import com.alibaba.dubbo.rpc.RpcException;

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

    private final Map<String, JedisPool> jedisPools = new ConcurrentHashMap<String, JedisPool>();

    private final ConcurrentMap<String, Notifier> notifiers = new ConcurrentHashMap<String, Notifier>();
    
    private final int reconnectPeriod;

    private final int expirePeriod;
    
    private volatile boolean admin = false;

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
        
        List<String> addresses = new ArrayList<String>();
        addresses.add(url.getAddress());
        String[] backups = url.getParameter(Constants.BACKUP_KEY, new String[0]);
        if (backups != null && backups.length > 0) {
            addresses.addAll(Arrays.asList(backups));
        }
        for (String address : addresses) {
            int i = address.indexOf(':');
            String host;
            int port;
            if (i > 0) {
                host = address.substring(0, i);
                port = Integer.parseInt(address.substring(i + 1));
            } else {
                host = address;
                port = DEFAULT_REDIS_PORT;
            }
            this.jedisPools.put(address, new JedisPool(config, host, port, 
                    url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT)));
        }
        
        this.reconnectPeriod = url.getParameter(Constants.REGISTRY_RECONNECT_PERIOD_KEY, Constants.DEFAULT_REGISTRY_RECONNECT_PERIOD);
        String group = url.getParameter(Constants.GROUP_KEY, DEFAULT_ROOT);
        if (! group.startsWith(Constants.PATH_SEPARATOR)) {
            group = Constants.PATH_SEPARATOR + group;
        }
        if (! group.endsWith(Constants.PATH_SEPARATOR)) {
            group = group + Constants.PATH_SEPARATOR;
        }
        this.root = group;
        
        this.expirePeriod = url.getParameter(Constants.SESSION_TIMEOUT_KEY, Constants.DEFAULT_SESSION_TIMEOUT);
        this.expireFuture = expireExecutor.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                try {
                    deferExpired(); // 延长过期时间
                } catch (Throwable t) { // 防御性容错
                    logger.error("Unexpected exception occur at defer expire time, cause: " + t.getMessage(), t);
                }
            }
        }, expirePeriod / 2, expirePeriod / 2, TimeUnit.MILLISECONDS);
    }
    
    private void deferExpired() {
        for (Map.Entry<String, JedisPool> entry : jedisPools.entrySet()) {
            JedisPool jedisPool = entry.getValue();
            try {
                Jedis jedis = jedisPool.getResource();
                try {
                    for (String provider : new HashSet<String>(getRegistered())) {
                        String key = toProviderPath(URL.valueOf(provider));
                        if (jedis.hset(key, provider, String.valueOf(System.currentTimeMillis() + expirePeriod)) == 0) {
                            jedis.publish(key, Constants.REGISTER);
                        }
                    }
                    for (String consumer : new HashSet<String>(getSubscribed().keySet())) {
                        URL url = URL.valueOf(consumer);
                        if (! Constants.ANY_VALUE.equals(url.getServiceInterface())) {
                            String key = toConsumerPath(url);
                            if (jedis.hset(key, consumer, String.valueOf(System.currentTimeMillis() + expirePeriod)) == 0) {
                                jedis.publish(key, Constants.SUBSCRIBE);
                            }
                        }
                    }
                    if (admin) {
                        clean(jedis);
                    }
                } finally {
                    jedisPool.returnResource(jedis);
                }
            } catch (Throwable t) {
                logger.warn("Failed to write provider heartbeat to redis registry. registry: " + entry.getKey() + ", cause: " + t.getMessage(), t);
            }
        }
    }
    
    // 监控中心负责删除过期脏数据
    private void clean(Jedis jedis) {
        Set<String> keys = jedis.keys(root + Constants.ANY_VALUE);
        if (keys != null && keys.size() > 0) {
            for (String key : keys) {
                Map<String, String> values = jedis.hgetAll(key);
                if (values != null && values.size() > 0) {
                    boolean delete = false;
                    long now = System.currentTimeMillis();
                    for (Map.Entry<String, String> entry : values.entrySet()) {
                        long expire = Long.parseLong(entry.getValue());
                        if (expire < now) {
                            jedis.hdel(key, entry.getKey());
                            delete = true;
                            if (logger.isWarnEnabled()) {
                                logger.warn("Delete expired key: " + key + " -> value: " + entry.getKey() + ", expire: " + new Date(expire) + ", now: " + new Date(now));
                            }
                        }
                    }
                    if (delete) {
                        if (key.endsWith(Constants.CONSUMERS)) {
                            jedis.publish(key, Constants.UNSUBSCRIBE);
                        } else {
                            jedis.publish(key, Constants.UNREGISTER);
                        }
                    }
                }
            }
        }
    }

    public boolean isAvailable() {
        for (JedisPool jedisPool : jedisPools.values()) {
            try {
                Jedis jedis = jedisPool.getResource();
                try {
                    if (! jedis.isConnected()) {
                        return false;
                    }
                } finally {
                    jedisPool.returnResource(jedis);
                }
            } catch (Throwable t) {
                return false;
            }
        }
        return true;
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
        for (Map.Entry<String, JedisPool> entry : jedisPools.entrySet()) {
            JedisPool jedisPool = entry.getValue();
            try {
                jedisPool.destroy();
            } catch (Throwable t) {
                logger.warn("Failed to destroy the redis registry client. registry: " + entry.getKey() + ", cause: " + t.getMessage(), t);
            }
        }
    }

    @Override
    public void doRegister(URL url) {
        String key = toProviderPath(url);
        String value = url.toFullString();
        String expire = String.valueOf(System.currentTimeMillis() + expirePeriod);
        boolean success = false;
        RpcException exception = null;
        for (Map.Entry<String, JedisPool> entry : jedisPools.entrySet()) {
            JedisPool jedisPool = entry.getValue();
            try {
                Jedis jedis = jedisPool.getResource();
                try {
                    jedis.hset(key, value, expire);
                    jedis.publish(key, Constants.REGISTER);
                    success = true;
                } finally {
                    jedisPool.returnResource(jedis);
                }
            } catch (Throwable t) {
                exception = new RpcException("Failed to register service to redis registry. registry: " + entry.getKey() + ", service: " + url + ", cause: " + t.getMessage(), t);
            }
        }
        if (exception != null) {
            if (success) {
                logger.warn(exception.getMessage(), exception);
            } else {
                throw exception;
            }
        }
    }

    @Override
    public void doUnregister(URL url) {
        String key = toProviderPath(url);
        String value = url.toFullString();
        RpcException exception = null;
        for (Map.Entry<String, JedisPool> entry : jedisPools.entrySet()) {
            JedisPool jedisPool = entry.getValue();
            try {
                Jedis jedis = jedisPool.getResource();
                try {
                    jedis.hdel(key, value);
                    jedis.publish(key, Constants.UNREGISTER);
                } finally {
                    jedisPool.returnResource(jedis);
                }
            } catch (Throwable t) {
                exception = new RpcException("Failed to unregister service to redis registry. registry: " + entry.getKey() + ", service: " + url + ", cause: " + t.getMessage(), t);
            }
        }
        if (exception != null) {
            throw exception;
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
        boolean success = false;
        RpcException exception = null;
        for (Map.Entry<String, JedisPool> entry : jedisPools.entrySet()) {
            JedisPool jedisPool = entry.getValue();
            try {
                Jedis jedis = jedisPool.getResource();
                try {
                    if (service.endsWith(Constants.ANY_VALUE)) {
                        admin = true;
                        for (String s : getServices(jedis, service)) {
                            doNotify(jedis, s, url, listener);
                        }
                    } else {
                        String key = toConsumerPath(url);
                        String value = url.toFullString();
                        String expire = String.valueOf(System.currentTimeMillis() + expirePeriod);
                        jedis.hset(key, value, expire);
                        jedis.publish(key, Constants.SUBSCRIBE);
                        doNotify(jedis, service, url, listener);
                    }
                    success = true;
                    break; // 只需读一个服务器的数据
                } finally {
                    jedisPool.returnResource(jedis);
                }
            } catch(Throwable t) { // 尝试下一个服务器
                exception = new RpcException("Failed to subscribe service from redis registry. registry: " + entry.getKey() + ", service: " + url + ", cause: " + t.getMessage(), t);
            }
        }
        if (exception != null) {
            if (success) {
                logger.warn(exception.getMessage(), exception);
            } else {
                throw exception;
            }
        }
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        if (! Constants.ANY_VALUE.equals(url.getServiceInterface())) {
            String key = toConsumerPath(url);
            String value = url.toFullString();
            RpcException exception = null;
            for (Map.Entry<String, JedisPool> entry : jedisPools.entrySet()) {
                JedisPool jedisPool = entry.getValue();
                try {
                    Jedis jedis = jedisPool.getResource();
                    try {
                        jedis.hdel(key, value);
                        jedis.publish(key, Constants.UNSUBSCRIBE);
                    } finally {
                        jedisPool.returnResource(jedis);
                    }
                } catch (Throwable t) {
                    exception = new RpcException("Failed to unsubscribe service to redis registry. registry: " + entry.getKey() + ", service: " + url + ", cause: " + t.getMessage(), t);
                }
            }
            if (exception != null) {
                throw exception;
            }
        }
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
                URL u = URL.valueOf(entry.getKey());
                if (Long.parseLong(entry.getValue()) >= now) {
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
    
    private String getService(Jedis jedis, String key) {
        int i = key.indexOf(Constants.PATH_SEPARATOR, root.length());
        return i > 0 ? key.substring(0, i) : key;
    }

    private Set<String> getServices(Jedis jedis, String pattern) {
        Set<String> keys = jedis.keys(pattern);
        Set<String> services = new HashSet<String>();
        if (keys != null && keys.size() > 0) {
            for (String key : keys) {
                services.add(getService(jedis, key));
            }
        }
        return services;
    }

    private String toServicePath(URL url) {
        return root + url.getServiceInterface();
    }

    private String toProviderPath(URL url) {
        return toServicePath(url) + Constants.PATH_SEPARATOR + Constants.PROVIDERS;
    }

    private String toConsumerPath(URL url) {
        return toServicePath(url) + Constants.PATH_SEPARATOR + Constants.CONSUMERS;
    }

    private class NotifySub extends JedisPubSub {
        
        private final JedisPool jedisPool;

        public NotifySub(JedisPool jedisPool) {
            this.jedisPool = jedisPool;
        }

        @Override
        public void onMessage(String key, String msg) {
            if (logger.isInfoEnabled()) {
                logger.info("redis event: " + key + " = " + msg);
            }
            if (msg.equals(Constants.REGISTER) 
                    || msg.equals(Constants.UNREGISTER)
                    || msg.equals(Constants.SUBSCRIBE) 
                    || msg.equals(Constants.UNSUBSCRIBE)) {
                try {
                    Jedis jedis = jedisPool.getResource();
                    try {
                        doNotify(jedis, getService(jedis, key), msg.equals(Constants.SUBSCRIBE) || msg.equals(Constants.UNSUBSCRIBE));
                    } finally {
                        jedisPool.returnResource(jedis);
                    }
                } catch (Throwable t) { // TODO 通知失败没有恢复机制保障
                    logger.error(t.getMessage(), t);
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

    private class Notifier extends Thread {

        private final String service;

        private volatile Jedis jedis;

        private volatile boolean first = true;
        
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
                            for (Map.Entry<String, JedisPool> entry : jedisPools.entrySet()) {
                                JedisPool jedisPool = entry.getValue();
                                try {
                                    jedis = jedisPool.getResource();
                                    try {
                                        if (service.endsWith(Constants.ANY_VALUE)) {
                                            if (! first) {
                                                first = false;
                                                for (String s : getServices(jedis, service)) {
                                                    doNotify(jedis, s, false);
                                                }
                                                resetSkip();
                                            }
                                            jedis.psubscribe(new NotifySub(jedisPool), service); // 阻塞
                                        } else {
                                            if (! first) {
                                                first = false;
                                                doNotify(jedis, service, false);
                                                resetSkip();
                                            }
                                            jedis.subscribe(new NotifySub(jedisPool), service + Constants.PATH_SEPARATOR + Constants.PROVIDERS); // 阻塞
                                        }
                                        break;
                                    } finally {
                                        jedisPool.returnBrokenResource(jedis);
                                    }
                                } catch (Throwable t) { // 重试另一台
                                    logger.warn("Failed to subscribe service from redis registry. registry: " + entry.getKey() + ", cause: " + t.getMessage(), t);
                                }
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

}
