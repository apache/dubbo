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
package com.alibaba.dubbo.registry.redis;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.support.FailbackRegistry;
import com.alibaba.dubbo.rpc.RpcException;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

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

/**
 * 基于Redis事件的注册中心
 */
public class RedisRegistry extends FailbackRegistry {

    private static final Logger logger = LoggerFactory.getLogger(RedisRegistry.class);

    //默认redis端口
    private static final int DEFAULT_REDIS_PORT = 6379;

    //默认根节点
    private final static String DEFAULT_ROOT = "dubbo";

    //Redis Key过期机制执行器
    private final ScheduledExecutorService expireExecutor
        = Executors.newScheduledThreadPool(1, new NamedThreadFactory("DubboRegistryExpireTimer", true));

    //过期周期，单位：ms
    private final int expirePeriod;

    //Redis Key过期机制Future
    private final ScheduledFuture<?> expireFuture;

    //Redis跟节点，默认是/dubbo/
    private final String root;

    /**
     * JedisPool集合
     * key是ip:port
     */
    private final Map<String, JedisPool> jedisPools = new ConcurrentHashMap<String, JedisPool>();

    /**
     * 通知器集合
     * 用于 Redis Publish/Subscribe 机制中的订阅，实时监听数据的变化。
     * key是Root+Service，例如 `/dubbo/com.alibaba.dubbo.demo.DemoService`
     */
    private final ConcurrentMap<String, Notifier> notifiers = new ConcurrentHashMap<String, Notifier>();

    //重连周期，单位：ms
    private final int reconnectPeriod;

    /**
     * 是否监控中心
     * 用于判断脏数据，脏数据由监控中心删除 {@link #clean(Jedis)}
     */
    private volatile boolean admin = false;

    /**
     * 是否复制模式
     * 可通过 <dubbo:registry cluster="replicate" /> 设置 redis 集群策略，缺省为 failover：
     *
     * failover: 只写入和读取任意一台，失败时重试另一台，需要服务器端自行配置数据同步。
     * replicate: 在客户端同时写入所有服务器，只读取单台，服务器端不需要同步，注册中心集群增大，性能压力也会更大。
     */
    private boolean replicate;

    public RedisRegistry(URL url) {
        super(url);
        if (url.isAnyHost()) {
            throw new IllegalStateException("registry address == null");
        }
        //连接池配置
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setTestOnBorrow(url.getParameter("test.on.borrow", true));
        config.setTestOnReturn(url.getParameter("test.on.return", false));
        config.setTestWhileIdle(url.getParameter("test.while.idle", false));
        if (url.getParameter("max.idle", 0) > 0)
            config.setMaxIdle(url.getParameter("max.idle", 0));
        if (url.getParameter("min.idle", 0) > 0)
            config.setMinIdle(url.getParameter("min.idle", 0));
        if (url.getParameter("max.active", 0) > 0)
            config.setMaxTotal(url.getParameter("max.active", 0));
        if (url.getParameter("max.total", 0) > 0)
            config.setMaxTotal(url.getParameter("max.total", 0));
        if (url.getParameter("max.wait", url.getParameter("timeout", 0)) > 0)
            config.setMaxWaitMillis(url.getParameter("max.wait", url.getParameter("timeout", 0)));
        if (url.getParameter("num.tests.per.eviction.run", 0) > 0)
            config.setNumTestsPerEvictionRun(url.getParameter("num.tests.per.eviction.run", 0));
        if (url.getParameter("time.between.eviction.runs.millis", 0) > 0)
            config.setTimeBetweenEvictionRunsMillis(url.getParameter("time.between.eviction.runs.millis", 0));
        if (url.getParameter("min.evictable.idle.time.millis", 0) > 0)
            config.setMinEvictableIdleTimeMillis(url.getParameter("min.evictable.idle.time.millis", 0));

        String cluster = url.getParameter("cluster", "failover");
        if (!"failover".equals(cluster) && !"replicate".equals(cluster)) {
            throw new IllegalArgumentException("Unsupported redis cluster: " + cluster + ". The redis cluster only supported failover or replicate.");
        }
        //是否复制模式
        replicate = "replicate".equals(cluster);

        //创建 JedisPool 对象
        List<String> addresses = new ArrayList<String>();
        addresses.add(url.getAddress());
        String[] backups = url.getParameter(Constants.BACKUP_KEY, new String[0]);
        if (backups != null && backups.length > 0) {
            addresses.addAll(Arrays.asList(backups));
        }

        String password = url.getPassword();
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
            if (StringUtils.isEmpty(password)) {
                //无密码连接
                this.jedisPools.put(address, new JedisPool(config, host, port,
                        url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT)));
            } else {
                //有密码连接
                this.jedisPools.put(address, new JedisPool(config, host, port,
                        url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT), password));
            }
        }

        //重连周期，默认3秒
        this.reconnectPeriod = url.getParameter(Constants.REGISTRY_RECONNECT_PERIOD_KEY, Constants.DEFAULT_REGISTRY_RECONNECT_PERIOD);
        //获得Redis根节点
        String group = url.getParameter(Constants.GROUP_KEY, DEFAULT_ROOT);
        if (!group.startsWith(Constants.PATH_SEPARATOR)) {
            group = Constants.PATH_SEPARATOR + group;
        }
        if (!group.endsWith(Constants.PATH_SEPARATOR)) {
            group = group + Constants.PATH_SEPARATOR;
        }
        //root=/dubbo/
        this.root = group;

        //过期周期，默认一分钟
        this.expirePeriod = url.getParameter(Constants.SESSION_TIMEOUT_KEY, Constants.DEFAULT_SESSION_TIMEOUT);
        //创建实现Redis Key过期机制的任务
        //①延长未过期的key；②删除过期的key
        this.expireFuture = expireExecutor.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                try {
                    deferExpired(); // Extend the expiration time
                } catch (Throwable t) { // Defensive fault tolerance
                    logger.error("Unexpected exception occur at defer expire time, cause: " + t.getMessage(), t);
                }
            }
        }, expirePeriod / 2, expirePeriod / 2, TimeUnit.MILLISECONDS);//任务间隔为 expirePeriod 的一半，避免过于频繁，对 Redis 的压力过大；同时，避免过于不频繁，每次执行时，都过期了。
    }

    private void deferExpired() {
        for (Map.Entry<String, JedisPool> entry : jedisPools.entrySet()) {
            JedisPool jedisPool = entry.getValue();
            try {
                Jedis jedis = jedisPool.getResource();
                try {
                    for (URL url : new HashSet<URL>(getRegistered())) {
                        //只有动态节点需要延长事件
                        if (url.getParameter(Constants.DYNAMIC_KEY, true)) {
                            // /dubbo/com.gyf.TestProvider/providers or /dubbo/com.gyf.TestProvider/consumers
                            String key = toCategoryPath(url);
                            //重置过期事件，如果返回的值为1，说明Map中该键对应的值不存在，则发布registry事件
                            if (jedis.hset(key, url.toFullString(), String.valueOf(System.currentTimeMillis() + expirePeriod)) == 1) {
                                //发布注册事件
                                jedis.publish(key, Constants.REGISTER);
                            }
                        }
                    }
                    //删除过期的脏数据
                    if (admin) {
                        clean(jedis);
                    }
                    //非复制模式，就只操作一个就ok
                    if (!replicate) {
                        break;//  If the server side has synchronized data, just write a single machine
                    }
                } finally {
                    jedis.close();
                }
            } catch (Throwable t) {
                logger.warn("Failed to write provider heartbeat to redis registry. registry: " + entry.getKey() + ", cause: " + t.getMessage(), t);
            }
        }
    }

    // The monitoring center is responsible for deleting outdated dirty data
    private void clean(Jedis jedis) {
        //获取所有服务
        Set<String> keys = jedis.keys(root + Constants.ANY_VALUE);
        if (keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                //获取所有URL
                Map<String, String> values = jedis.hgetAll(key);
                if (values != null && values.size() > 0) {
                    boolean delete = false;
                    long now = System.currentTimeMillis();
                    for (Map.Entry<String, String> entry : values.entrySet()) {
                        URL url = URL.valueOf(entry.getKey());
                        //动态节点
                        if (url.getParameter(Constants.DYNAMIC_KEY, true)) {
                            long expire = Long.parseLong(entry.getValue());
                            //过期则删除
                            if (expire < now) {
                                jedis.hdel(key, entry.getKey());
                                //标记删除
                                delete = true;
                                if (logger.isWarnEnabled()) {
                                    logger.warn("Delete expired key: " + key + " -> value: " + entry.getKey() + ", expire: " + new Date(expire) + ", now: " + new Date(now));
                                }
                            }
                        }
                    }
                    if (delete) {
                        //发布unregistry事件
                        jedis.publish(key, Constants.UNREGISTER);
                    }
                }
            }
        }
    }

    //redis集群是否可用：有一个节点可用就是可用
    public boolean isAvailable() {
        for (JedisPool jedisPool : jedisPools.values()) {
            try {
                Jedis jedis = jedisPool.getResource();
                try {
                    if (jedis.isConnected()) {
                        return true; // At least one single machine is available.
                    }
                } finally {
                    jedis.close();
                }
            } catch (Throwable t) {
            }
        }
        return false;
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            //关闭定时任务
            expireFuture.cancel(true);
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
        try {
            for (Notifier notifier : notifiers.values()) {
                //关闭通知器
                notifier.shutdown();
            }
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
        //关闭连接池
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
        //获得分类路径作为key：/dubbo/com.gyf.TestService/providers
        String key = toCategoryPath(url);
        //获取URL字符串作为value -> dubbo://ip:port/TestService
        String value = url.toFullString();
        //计算过期时间
        String expire = String.valueOf(System.currentTimeMillis() + expirePeriod);
        boolean success = false;
        RpcException exception = null;
        //向redis注册
        for (Map.Entry<String, JedisPool> entry : jedisPools.entrySet()) {
            JedisPool jedisPool = entry.getValue();
            try {
                Jedis jedis = jedisPool.getResource();
                try {
                    //写入redis Map键，过期时间作为Map中的值
                    jedis.hset(key, value, expire);
                    //发布Redis注册事件，订阅该key的消费者和监控中心，就会实时从Redis读取该服务的最新数据。
                    jedis.publish(key, Constants.REGISTER);
                    success = true;
                    //如果由redis自己同步数据，只需写入单台机器。
                    if (!replicate) {
                        break;
                    }
                } finally {
                    jedis.close();
                }
            } catch (Throwable t) {
                exception = new RpcException("Failed to register service to redis registry. registry: " + entry.getKey() + ", service: " + url + ", cause: " + t.getMessage(), t);
            }
        }
        if (exception != null) {
            //只要有一台成功，就当作成功，否则抛出异常
            if (success) {
                logger.warn(exception.getMessage(), exception);
            } else {
                throw exception;
            }
        }
    }

    //方服务消费者和服务提供者关闭时，会调用该方法取消注册
    //删除Map中的key，以及发布unregistry事件，实时通知订阅者
    @Override
    public void doUnregister(URL url) {
        // /dubbo/com.gyf.TestService/providers
        String key = toCategoryPath(url);
        //dubbo://ip:port/barService
        String value = url.toFullString();
        RpcException exception = null;
        boolean success = false;
        //向Redis取消注册
        for (Map.Entry<String, JedisPool> entry : jedisPools.entrySet()) {
            JedisPool jedisPool = entry.getValue();
            try {
                Jedis jedis = jedisPool.getResource();
                try {
                    //删除Redis Map键
                    jedis.hdel(key, value);
                    //发布Redis取消事件，正常情况下就无需监控中心做脏数据删除工作
                    jedis.publish(key, Constants.UNREGISTER);
                    success = true;
                    if (!replicate) {
                        break; //  If the server side has synchronized data, just write a single machine
                    }
                } finally {
                    jedis.close();
                }
            } catch (Throwable t) {
                exception = new RpcException("Failed to unregister service to redis registry. registry: " + entry.getKey() + ", service: " + url + ", cause: " + t.getMessage(), t);
            }
        }
        //处理异常
        if (exception != null) {
            if (success) {
                logger.warn(exception.getMessage(), exception);
            } else {
                throw exception;
            }
        }
    }

    @Override
    public void doSubscribe(final URL url, final NotifyListener listener) {
        //获得服务路径，例如：`/dubbo/com.alibaba.dubbo.demo.DemoService` or `/dubbo/*`
        String service = toServicePath(url);
        //获取通知器Notifier
        Notifier notifier = notifiers.get(service);
        //创建通知器
        if (notifier == null) {
            Notifier newNotifier = new Notifier(service);
            notifiers.putIfAbsent(service, newNotifier);
            notifier = notifiers.get(service);
            if (notifier == newNotifier) {//保证并发的情况下只有一个启动
                notifier.start();
            }
        }
        //if(notifier == null){
        //    synchronized (service){
        //        if(notifiers.get(service) == null){
        //            notifiers.put(service,new Notifier(service));
        //        }
        //    }
        //    notifiers.get(service).start();
        //}
        boolean success = false;
        RpcException exception = null;
        //循环 `jedisPools` ，仅向一个 Redis 发起订阅，直到一个成功
        for (Map.Entry<String, JedisPool> entry : jedisPools.entrySet()) {
            JedisPool jedisPool = entry.getValue();
            try {
                Jedis jedis = jedisPool.getResource();
                try {
                    //处理监控中心 -> /dubbo/*
                    if (service.endsWith(Constants.ANY_VALUE)) {
                        //只有注册中心，才清理脏数据
                        admin = true;
                        // 获得分类层集合，例如：`/dubbo/com.alibaba.dubbo.demo.DemoService/providers`
                        Set<String> keys = jedis.keys(service);//keys()只返回键名，可以进行模式匹配
                        if (keys != null && !keys.isEmpty()) {
                            //Map<"/dubbo/com.TestService",
                            //      ["/dubbo/com.TestService/providers","/dubbo/com.TestService/consumers"]>
                            Map<String, Set<String>> serviceKeys = new HashMap<String, Set<String>>();
                            // key —> /dubbo/com.gyf.TestService/providers
                            for (String key : keys) {
                                //serviceKey=/dubbo/com.gyf.TestService
                                String serviceKey = toServicePath(key);
                                Set<String> sk = serviceKeys.get(serviceKey);
                                if (sk == null) {
                                    sk = new HashSet<String>();
                                    serviceKeys.put(serviceKey, sk);
                                }
                                sk.add(key);
                            }
                            //循环 serviceKeys ，按照每个 Service 层的发起通知
                            for (Set<String> sk : serviceKeys.values()) {
                                //按照每个 Service 层，通知监听器，初始的数据
                                //sk=["/dubbo/com.TestService/providers","/dubbo/com.TestService/consumers"]
                                doNotify(jedis, sk, url, Arrays.asList(listener));
                            }
                        }
                    } else {
                        //适用服务提供者和服务消费者，处理指定 Service 层的初始化数据
                        //获得/dubbo/com.alibaba.dubbo.demo.DemoService/*样式的所有key
                        Set<String> keys = jedis.keys(service + Constants.PATH_SEPARATOR + Constants.ANY_VALUE);
                        //通知监听器，初始的数据
                        doNotify(jedis, keys, url, Arrays.asList(listener));
                    }
                    //标记成功
                    success = true;
                    //仅从一台服务器读取数据
                    break; // Just read one server's data
                } finally {
                    jedis.close();
                }
            } catch (Throwable t) { // Try the next server
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
    }

    /**
     * @param jedis
     * @param key key 通知的事件key
     */
    private void doNotify(Jedis jedis, String key) {
        //获取所有监听器
        Map<URL, Set<NotifyListener>> subscribed = getSubscribed();
        for (Map.Entry<URL, Set<NotifyListener>> entry : new HashMap<URL, Set<NotifyListener>>(subscribed).entrySet()) {
            //对每一个URL都进行通知
            doNotify(
                jedis,
                Arrays.asList(key), //
                entry.getKey(), //订阅者URL(比如消费者URL)
                new HashSet<NotifyListener>(entry.getValue())//监听器
            );
        }
    }

    /**
     * @param jedis
     * @param keys @params keys ["/dubbo/com.TestService/providers","/dubbo/com.TestService/consumers"]
     * @param subscribeUrl 订阅者URL(比如消费者URL)
     * @param listeners
     */
    private void doNotify(Jedis jedis, Collection<String> keys, URL subscribeUrl, Collection<NotifyListener> listeners) {
        if (keys == null || keys.isEmpty()
                || listeners == null || listeners.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        //提供者列表
        List<URL> result = new ArrayList<URL>();
        /**
         * 获取订阅的分类
         * 服务消费者，关注 providers configurations routes 。
         * 服务提供者，关注 consumers 。
         * 监控中心，关注所有。
         */
        List<String> categories = Arrays.asList(subscribeUrl.getParameter(Constants.CATEGORY_KEY, new String[0]));
        //订阅的服务接口
        String consumerService = subscribeUrl.getServiceInterface();
        //循环分类
        for (String key : keys) {
            if (!Constants.ANY_VALUE.equals(consumerService)) {
                String prvoiderService = toServiceName(key);
                //服务的接口！=订阅的接口
                if (!prvoiderService.equals(consumerService)) {
                    continue;
                }
            }

            //分类
            String category = toCategoryName(key);
            //订阅的分类是否匹配
            if (!categories.contains(Constants.ANY_VALUE) && !categories.contains(category)) {
                continue;
            }
            List<URL> urls = new ArrayList<URL>();
            //获取该分类下的所有URL，<"dubbo://ip:port/barService",过期时间>
            Map<String, String> values = jedis.hgetAll(key);
            if (values != null && values.size() > 0) {
                for (Map.Entry<String, String> entry : values.entrySet()) {
                    URL u = URL.valueOf(entry.getKey());
                    //过滤掉过期动态节点
                    if (!u.getParameter(Constants.DYNAMIC_KEY, true)
                            || Long.parseLong(entry.getValue()) >= now) {
                        if (UrlUtils.isMatch(subscribeUrl, u)) {
                            urls.add(u);
                        }
                    }
                }
            }
            // 若不存在匹配，则创建 `empty://` 的 URL返回，用于清空该服务的该分类。
            if (urls.isEmpty()) {
                urls.add(subscribeUrl.setProtocol(Constants.EMPTY_PROTOCOL)
                        .setAddress(Constants.ANYHOST_VALUE)
                        .setPath(toServiceName(key))
                        .addParameter(Constants.CATEGORY_KEY, category));
            }
            result.addAll(urls);
            if (logger.isInfoEnabled()) {
                logger.info("redis notify: " + key + " = " + urls);
            }
        }
        if (result == null || result.isEmpty()) {
            return;
        }
        //通知监听器
        for (NotifyListener listener : listeners) {
            notify(
                subscribeUrl,//订阅者URL
                listener,
                result//提供者列表
            );
        }
    }

    /**
     * 接口名
     * @param categoryPath
     * @return com.TestService
     */
    private String toServiceName(String categoryPath) {
        String servicePath = toServicePath(categoryPath);
        return servicePath.startsWith(root) ? servicePath.substring(root.length()) : servicePath;
    }

    /**
     * 获取分类名
     * @param categoryPath
     * @return providers
     */
    private String toCategoryName(String categoryPath) {
        int i = categoryPath.lastIndexOf(Constants.PATH_SEPARATOR);
        return i > 0 ? categoryPath.substring(i + 1) : categoryPath;
    }

    /**
     * 获得服务路径，主要截掉多余的部分
     *
     * Root + Type
     *
     * @param categoryPath 分类路径  /dubbo/com.gyf.TestService/providers
     * @return 服务路径 /dubbo/com.gyf.TestService
     */
    private String toServicePath(String categoryPath) {
        int i;
        if (categoryPath.startsWith(root)) {
            //  /dubbo/com.gyf.TestService/providers -> i = providers前面的斜杠的位置
            i = categoryPath.indexOf(Constants.PATH_SEPARATOR, root.length());
        } else {
            i = categoryPath.indexOf(Constants.PATH_SEPARATOR);
        }
        //  /dubbo/com.gyf.TestService
        return i > 0 ? categoryPath.substring(0, i) : categoryPath;
    }

    /**
     * 获取服务路径，root+type
     * @param url
     * @return 服务路径 /dubbo/com.gyf.TestService
     */
    private String toServicePath(URL url) {
        return root + url.getServiceInterface();
    }

    /**
     * 获取分类路径：/dubbo/com.gyf.TestService/providers
     * @param url
     * @return
     */
    private String toCategoryPath(URL url) {
        return toServicePath(url) + Constants.PATH_SEPARATOR + url.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
    }

    /**
     * 通知订阅实现类
     */
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
            //收到registry和unregistry调用doNotify通知监听器变化，从而实现实时更新
            if (msg.equals(Constants.REGISTER)
                    || msg.equals(Constants.UNREGISTER)) {
                try {
                    Jedis jedis = jedisPool.getResource();
                    try {
                        doNotify(jedis, key);
                    } finally {
                        jedis.close();
                    }
                } catch (Throwable t) { // TODO Notification failure does not restore mechanism guarantee
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
        /**
         * root + service
         * /dubbo/com.alibaba.dubbo.demo.DemoService
         */
        private final String service;

        //需要忽略连接的次数
        private final AtomicInteger connectSkip = new AtomicInteger();

        //已经忽略连接的次数
        private final AtomicInteger connectSkiped = new AtomicInteger();

        private final Random random = new Random();
        private volatile Jedis jedis;

        //是否首次
        private volatile boolean first = true;

        //是否运行中
        private volatile boolean running = true;

        //连接次数随机数
        private volatile int connectRandom;

        public Notifier(String service) {
            super.setDaemon(true);
            super.setName("DubboRedisSubscribe");
            this.service = service;
        }

        /**
         * 重置重试信息
         */
        private void resetSkip() {
            connectSkip.set(0);
            connectSkiped.set(0);
            connectRandom = 0;
        }

        /**
         * 判断是否忽略本次对Redis的连接
         * 达到的目的就是重试一次比一次隔得久
         * @return
         */
        private boolean isSkip() {
            //获取需要忽略连接的次数
            int skip = connectSkip.get(); // Growth of skipping times
            if (skip >= 10) { // If the number of skipping times increases by more than 10, take the random number
                if (connectRandom == 0) {
                    connectRandom = random.nextInt(10);
                }
                skip = 10 + connectRandom;
            }
            //自增已经忽略次数，如果忽略次数不够，则继续忽略
            if (connectSkiped.getAndIncrement() < skip) { // Check the number of skipping times
                return true;
            }
            //增加需要忽略的次数
            connectSkip.incrementAndGet();
            //已经忽略次数置0
            connectSkiped.set(0);
            connectRandom = 0;
            return false;
        }

        @Override
        public void run() {
            //循环执行，知道关闭
            while (running) {
                try {
                    //是否跳过本次Redis连接
                    if (!isSkip()) {
                        try {
                            //循环连接池，有一个成功就break
                            for (Map.Entry<String, JedisPool> entry : jedisPools.entrySet()) {
                                JedisPool jedisPool = entry.getValue();
                                try {
                                    jedis = jedisPool.getResource();
                                    try {
                                        //监控中心
                                        if (service.endsWith(Constants.ANY_VALUE)) {
                                            if (!first) {
                                                first = false;
                                                Set<String> keys = jedis.keys(service);
                                                if (keys != null && !keys.isEmpty()) {
                                                    for (String s : keys) {
                                                        doNotify(jedis, s);
                                                    }
                                                }
                                                resetSkip();
                                            }
                                            //批订阅
                                            jedis.psubscribe(new NotifySub(jedisPool), service); // blocking
                                        } else {
                                            if (!first) {
                                                first = false;
                                                doNotify(jedis, service);
                                                resetSkip();
                                            }
                                            //批订阅
                                            jedis.psubscribe(new NotifySub(jedisPool), service + Constants.PATH_SEPARATOR + Constants.ANY_VALUE); // blocking
                                        }
                                        break;
                                    } finally {
                                        jedis.close();
                                    }
                                } catch (Throwable t) { // Retry another server
                                    logger.warn("Failed to subscribe service from redis registry. registry: " + entry.getKey() + ", cause: " + t.getMessage(), t);
                                    // If you only have a single redis, you need to take a rest to avoid overtaking a lot of CPU resources
                                    sleep(reconnectPeriod);
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
                //停止运行
                running = false;
                //断开连接
                jedis.disconnect();
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }
        }

    }

}
