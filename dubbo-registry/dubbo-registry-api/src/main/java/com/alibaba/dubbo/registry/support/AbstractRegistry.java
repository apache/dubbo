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
package com.alibaba.dubbo.registry.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.Registry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AbstractRegistry. (SPI, Prototype, ThreadSafe)
 *
 * Registry 抽象类，实现了如下方法：
 *
 * 1、通用的注册、订阅、查询、通知等方法
 * 2、读取和持久化注册数据到文件，以 properties 格式存储
 */
public abstract class AbstractRegistry implements Registry {

    // URL地址分隔符，用于文件缓存中，服务提供者URL分隔
    // URL address separator, used in file cache, service provider URL separation
    private static final char URL_SEPARATOR = ' ';
    // URL地址分隔正则表达式，用于解析文件缓存中服务提供者URL列表
    // URL address separated regular expression for parsing the service provider URL list in the file cache
    private static final String URL_SPLIT = "\\s+";

    // Log output
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     *  本地磁盘缓存。
     *
     *  1. 其中特殊的 key 值 .registies 记录注册中心列表 TODO 8019 芋艿，特殊的 key 是
     *  2. 其它均为 {@link #notified} 服务提供者列表
     */
    // Local disk cache, where the special key value.registies records the list of registry centers, and the others are the list of notified service providers
    private final Properties properties = new Properties();
    /**
     * 注册中心缓存写入执行器。
     *
     * 线程数=1
     */
    // File cache timing writing
    private final ExecutorService registryCacheExecutor = Executors.newFixedThreadPool(1, new NamedThreadFactory("DubboSaveRegistryCache", true));
    /**
     * 是否同步保存文件
     */
    // Is it synchronized to save the file
    private final boolean syncSaveFile;
    /**
     * 数据版本号
     *
     * {@link #properties}
     */
    private final AtomicLong lastCacheChanged = new AtomicLong();
    /**
     * 已注册 URL 集合。
     *
     * 注意，注册的 URL 不仅仅可以是服务提供者的，也可以是服务消费者的
     */
    private final Set<URL> registered = new ConcurrentHashSet<URL>();
    /**
     * 订阅 URL 的监听器集合
     *
     * key：消费者的 URL ，例如消费者的 URL
     */
    private final ConcurrentMap<URL, Set<NotifyListener>> subscribed = new ConcurrentHashMap<URL, Set<NotifyListener>>();
    /**
     * 被通知的 URL 集合
     *
     * key1：消费者的 URL ，例如消费者的 URL ，和 {@link #subscribed} 的键一致
     * key2：分类，例如：providers、consumers、routes、configurators。【实际无 consumers ，因为消费者不会去订阅另外的消费者的列表】
     *            在 {@link Constants} 中，以 "_CATEGORY" 结尾
     */
    private final ConcurrentMap<URL, Map<String, List<URL>>> notified = new ConcurrentHashMap<URL, Map<String, List<URL>>>();
    /**
     * 注册中心 URL
     */
    private URL registryUrl;
    /**
     * 本地磁盘缓存文件，缓存注册中心的数据
     */
    // Local disk cache file
    private File file;
    /**
     * 是否销毁
     */
    private AtomicBoolean destroyed = new AtomicBoolean(false);

    public AbstractRegistry(URL url) {
        setUrl(url);
        // Start file save timer
        syncSaveFile = url.getParameter(Constants.REGISTRY_FILESAVE_SYNC_KEY, false);
        // 获得 `file`
        String filename = url.getParameter(Constants.FILE_KEY, System.getProperty("user.home") + "/.dubbo/dubbo-registry-" + url.getParameter(Constants.APPLICATION_KEY) + "-" + url.getAddress() + ".cache");
        File file = null;
        if (ConfigUtils.isNotEmpty(filename)) {
            file = new File(filename);
            if (!file.exists() && file.getParentFile() != null && !file.getParentFile().exists()) {
                if (!file.getParentFile().mkdirs()) {
                    throw new IllegalArgumentException("Invalid registry store file " + file + ", cause: Failed to create directory " + file.getParentFile() + "!");
                }
            }
        }
        this.file = file;
        // 加载本地磁盘缓存文件到内存缓存
        loadProperties();
        // 通知监听器，URL 变化结果
        notify(url.getBackupUrls()); // 【TODO 8020】为什么构造方法，要通知，连监听器都没注册
    }

    protected static List<URL> filterEmpty(URL url, List<URL> urls) {
        if (urls == null || urls.isEmpty()) {
            List<URL> result = new ArrayList<URL>(1);
            result.add(url.setProtocol(Constants.EMPTY_PROTOCOL));
            return result;
        }
        return urls;
    }

    @Override
    public URL getUrl() {
        return registryUrl;
    }

    protected void setUrl(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("registry url == null");
        }
        this.registryUrl = url;
    }

    public Set<URL> getRegistered() {
        return registered;
    }

    public Map<URL, Set<NotifyListener>> getSubscribed() {
        return subscribed;
    }

    public Map<URL, Map<String, List<URL>>> getNotified() {
        return notified;
    }

    public File getCacheFile() {
        return file;
    }

    public Properties getCacheProperties() {
        return properties;
    }

    public AtomicLong getLastCacheChanged() {
        return lastCacheChanged;
    }

    /**
     * 保存内存缓存到本地磁盘缓存文件，即 {@link #properties} => {@link #file}
     *
     * @param version 数据版本号
     */
    public void doSaveProperties(long version) {
        if (version < lastCacheChanged.get()) {
            return;
        }
        if (file == null) {
            return;
        }
        // Save
        try {
            // 创建 .lock 文件
            File lockfile = new File(file.getAbsolutePath() + ".lock");
            if (!lockfile.exists()) {
                lockfile.createNewFile();
            }
            // 随机读写文件操作
            RandomAccessFile raf = new RandomAccessFile(lockfile, "rw");
            try {
                FileChannel channel = raf.getChannel();
                try {
                    // 获得文件锁
                    FileLock lock = channel.tryLock();
                    // 获取失败
                    if (lock == null) {
                        throw new IOException("Can not lock the registry cache file " + file.getAbsolutePath() + ", ignore and retry later, maybe multi java process use the file, please config: dubbo.registry.file=xxx.properties");
                    }
                    // 获取成功，进行保存
                    // Save
                    try {
                        if (!file.exists()) {
                            file.createNewFile();
                        }
                        FileOutputStream outputFile = new FileOutputStream(file);
                        try {
                            properties.store(outputFile, "Dubbo Registry Cache");
                        } finally {
                            outputFile.close();
                        }
                    // 释放文件锁
                    } finally {
                        lock.release();
                    }
                // 释放文件 Channel
                } finally {
                    channel.close();
                }
            // 释放随机读写文件操作
            } finally {
                raf.close();
            }
        } catch (Throwable e) {
            // 版本号过小，不保存
            if (version < lastCacheChanged.get()) {
                return;
            // 重新异步保存，一般情况下为上面的获取锁失败抛出的异常。通过这样的方式，达到保存成功。
            } else {
                registryCacheExecutor.execute(new SaveProperties(lastCacheChanged.incrementAndGet()));
            }
            logger.warn("Failed to save registry store file, cause: " + e.getMessage(), e);
        }
    }

    /**
     * 加载本地磁盘缓存文件到内存缓存，即 {@link #file} => {@link #properties}
     */
    private void loadProperties() {
        if (file != null && file.exists()) {
            InputStream in = null;
            try {
                // 文件流
                in = new FileInputStream(file);
                // 读取文件流
                properties.load(in);
                if (logger.isInfoEnabled()) {
                    logger.info("Load registry store file " + file + ", data: " + properties);
                }
            } catch (Throwable e) {
                logger.warn("Failed to load registry store file " + file, e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            }
        }
    }

    /**
     * 从 `properties` 中获得缓存的 URL 集合
     *
     * @param url URL
     * @return URL 集合
     */
    public List<URL> getCacheUrls(URL url) {
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (key != null && key.length() > 0 // 非空
                    && key.equals(url.getServiceKey()) // 服务键匹配
                    && (Character.isLetter(key.charAt(0)) || key.charAt(0) == '_') // TODO 芋艿，_ 是什么
                    && value != null && value.length() > 0) { // 值非空
                String[] arr = value.trim().split(URL_SPLIT);
                List<URL> urls = new ArrayList<URL>();
                for (String u : arr) {
                    urls.add(URL.valueOf(u));
                }
                return urls;
            }
        }
        return null;
    }

    @Override
    public List<URL> lookup(URL url) {
        List<URL> result = new ArrayList<URL>();
        Map<String, List<URL>> notifiedUrls = getNotified().get(url);
        // 有数据，遍历数据获得
        if (notifiedUrls != null && notifiedUrls.size() > 0) {
            // 遍历
            for (List<URL> urls : notifiedUrls.values()) {
                for (URL u : urls) {
                    if (!Constants.EMPTY_PROTOCOL.equals(u.getProtocol())) {
                        result.add(u);
                    }
                }
            }
        // 无数据，通过发起订阅的方式得到数据后，遍历数据获得
        } else {
            // 创建 NotifyListener 对象
            final AtomicReference<List<URL>> reference = new AtomicReference<List<URL>>();
            NotifyListener listener = new NotifyListener() {
                public void notify(List<URL> urls) {
                    reference.set(urls);
                }
            };
            // 订阅获得数据
            subscribe(url, listener); // Subscribe logic guarantees the first notify to return
            List<URL> urls = reference.get();
            // 遍历
            if (urls != null && !urls.isEmpty()) {
                for (URL u : urls) {
                    if (!Constants.EMPTY_PROTOCOL.equals(u.getProtocol())) {
                        result.add(u);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void register(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("register url == null");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Register: " + url);
        }
        // 添加到 registered 集合
        registered.add(url);
    }

    @Override
    public void unregister(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("unregister url == null");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Unregister: " + url);
        }
        // 移除出 registered 集合
        registered.remove(url);
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("subscribe url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("subscribe listener == null");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Subscribe: " + url);
        }
        // 添加到 subscribed 集合
        Set<NotifyListener> listeners = subscribed.get(url);
        if (listeners == null) {
            subscribed.putIfAbsent(url, new ConcurrentHashSet<NotifyListener>());
            listeners = subscribed.get(url);
        }
        listeners.add(listener);
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("unsubscribe url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("unsubscribe listener == null");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Unsubscribe: " + url);
        }
        // 移除出 subscribed 集合
        Set<NotifyListener> listeners = subscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * 恢复注册和订阅
     *
     * @throws Exception 发生异常
     */
    protected void recover() throws Exception {
        // register 恢复注册
        Set<URL> recoverRegistered = new HashSet<URL>(getRegistered());
        if (!recoverRegistered.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover register url " + recoverRegistered);
            }
            for (URL url : recoverRegistered) {
                register(url);
            }
        }
        // subscribe 恢复订阅
        Map<URL, Set<NotifyListener>> recoverSubscribed = new HashMap<URL, Set<NotifyListener>>(getSubscribed());
        if (!recoverSubscribed.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover subscribe url " + recoverSubscribed.keySet());
            }
            for (Map.Entry<URL, Set<NotifyListener>> entry : recoverSubscribed.entrySet()) {
                URL url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    subscribe(url, listener);
                }
            }
        }
    }

    /**
     * 通知监听器，URL 变化结果。
     *
     * @param urls 通知的 URL 变化结果（全量数据）
     */
    protected void notify(List<URL> urls) {
        if (urls == null || urls.isEmpty()) return;
        // 循环 `subscribed` ，通知监听器们
        for (Map.Entry<URL, Set<NotifyListener>> entry : getSubscribed().entrySet()) {
            URL url = entry.getKey();
            // 匹配
            if (!UrlUtils.isMatch(url, urls.get(0))) {
                continue;
            }
            // 通知监听器
            Set<NotifyListener> listeners = entry.getValue();
            if (listeners != null) {
                for (NotifyListener listener : listeners) {
                    try {
                        notify(url, listener, filterEmpty(url, urls));
                    } catch (Throwable t) {
                        logger.error("Failed to notify registry event, urls: " + urls + ", cause: " + t.getMessage(), t);
                    }
                }
            }
        }
    }

    /**
     * 通知监听器，URL 变化结果。
     *
     * 数据流向 `urls` => {@link #notified} => {@link #properties} => {@link #file}
     *
     * @param url 消费者 URL
     * @param listener 监听器
     * @param urls 通知的 URL 变化结果（全量数据）
     */
    protected void  notify(URL url, NotifyListener listener, List<URL> urls) {
        if (url == null) {
            throw new IllegalArgumentException("notify url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("notify listener == null");
        }
        if ((urls == null || urls.isEmpty())
                && !Constants.ANY_VALUE.equals(url.getServiceInterface())) {
            logger.warn("Ignore empty notify urls for subscribe url " + url);
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("Notify urls for subscribe url " + url + ", urls: " + urls);
        }
        // 将 `urls` 按照 `url.parameter.category` 分类，添加到集合
        // 注意，特殊情况，使用 curator 连接 Zookeeper 时，若是服务消费者，连接断开，会出现 category=providers,configurations,routes
        Map<String, List<URL>> result = new HashMap<String, List<URL>>();
        for (URL u : urls) {
            if (UrlUtils.isMatch(url, u)) {
                String category = u.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
                List<URL> categoryList = result.get(category);
                if (categoryList == null) {
                    categoryList = new ArrayList<URL>();
                    result.put(category, categoryList);
                }
                categoryList.add(u);
            }
        }
        if (result.size() == 0) {
            return;
        }
        // 获得消费者 URL 对应的在 `notified` 中，通知的 URL 变化结果（全量数据）
        Map<String, List<URL>> categoryNotified = notified.get(url);
        if (categoryNotified == null) {
            notified.putIfAbsent(url, new ConcurrentHashMap<String, List<URL>>());
            categoryNotified = notified.get(url);
        }
        // 处理通知的 URL 变化结果（全量数据）
        for (Map.Entry<String, List<URL>> entry : result.entrySet()) {
            String category = entry.getKey();
            List<URL> categoryList = entry.getValue();
            // 覆盖到 `notified`
            // 当某个分类的数据为空时，会依然有 urls 。其中 `urls[0].protocol = empty` ，通过这样的方式，处理所有服务提供者为空的情况。
            categoryNotified.put(category, categoryList);
            // 保存到文件
            saveProperties(url);
            // 通知监听器
            listener.notify(categoryList);
        }
    }

    /**
     * 保存单个消费者 URL 对应，在 `notified` 的数据，到文件。
     *
     * @param url 消费者 URL
     */
    private void saveProperties(URL url) {
        if (file == null) {
            return;
        }

        try {
            // 拼接 URL
            StringBuilder buf = new StringBuilder();
            Map<String, List<URL>> categoryNotified = notified.get(url);
            if (categoryNotified != null) {
                for (List<URL> us : categoryNotified.values()) {
                    for (URL u : us) {
                        if (buf.length() > 0) {
                            buf.append(URL_SEPARATOR);
                        }
                        buf.append(u.toFullString());
                    }
                }
            }
            // 设置到 properties 中
            properties.setProperty(url.getServiceKey(), buf.toString());
            // 增加数据版本号
            long version = lastCacheChanged.incrementAndGet();
            // 保存到文件
            if (syncSaveFile) {
                doSaveProperties(version);
            } else {
                registryCacheExecutor.execute(new SaveProperties(version));
            }
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
    }

    /**
     * 取消注册和订阅
     */
    public void destroy() {
        // 已销毁，跳过
        if (!destroyed.compareAndSet(false, true)) {
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("Destroy registry:" + getUrl());
        }
        // 取消注册
        Set<URL> destroyRegistered = new HashSet<URL>(getRegistered());
        if (!destroyRegistered.isEmpty()) {
            for (URL url : new HashSet<URL>(getRegistered())) {
                if (url.getParameter(Constants.DYNAMIC_KEY, true)) {
                    try {
                        unregister(url); // 取消
                        if (logger.isInfoEnabled()) {
                            logger.info("Destroy unregister url " + url);
                        }
                    } catch (Throwable t) {
                        logger.warn("Failed to unregister url " + url + " to registry " + getUrl() + " on destroy, cause: " + t.getMessage(), t);
                    }
                }
            }
        }
        // 取消订阅
        Map<URL, Set<NotifyListener>> destroySubscribed = new HashMap<URL, Set<NotifyListener>>(getSubscribed());
        if (!destroySubscribed.isEmpty()) {
            for (Map.Entry<URL, Set<NotifyListener>> entry : destroySubscribed.entrySet()) {
                URL url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    try {
                        unsubscribe(url, listener); // 取消
                        if (logger.isInfoEnabled()) {
                            logger.info("Destroy unsubscribe url " + url);
                        }
                    } catch (Throwable t) {
                        logger.warn("Failed to unsubscribe url " + url + " to registry " + getUrl() + " on destroy, cause: " + t.getMessage(), t);
                    }
                }
            }
        }
    }

    public String toString() {
        return getUrl().toString();
    }

    /**
     * 保存配置的 Runnable
     */
    private class SaveProperties implements Runnable {

        /**
         * 数据版本号
         */
        private long version;

        private SaveProperties(long version) {
            this.version = version;
        }

        public void run() {
            doSaveProperties(version);
        }
    }

}