/*
 * Copyright 1999-2011 Alibaba Group.
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
package com.alibaba.dubbo.registry.support;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
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
import java.util.concurrent.atomic.AtomicLong;

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

/**
 * AbstractRegistry. (SPI, Prototype, ThreadSafe)
 * 
 * @author chao.liuc
 * @author william.liangf
 */
public abstract class AbstractRegistry implements Registry {

    // 日志输出
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    // URL地址分隔符，用于文件缓存中，服务提供者URL分隔
    private static final char URL_SEPARATOR = ' ';

    // URL地址分隔正则表达式，用于解析文件缓存中服务提供者URL列表
    private static final String URL_SPLIT = "\\s+";

    private URL registryUrl;

    // 本地磁盘缓存文件
    private File file;

    // 本地磁盘缓存，其中特殊的key值.registies记录注册中心列表，其它均为notified服务提供者列表
    private final Properties properties = new Properties();

    // 文件缓存定时写入
    private final ExecutorService registryCacheExecutor = Executors.newFixedThreadPool(1, new NamedThreadFactory("DubboSaveRegistryCache", true));

    //是否是同步保存文件
    private final boolean syncSaveFile ;
    
    private final AtomicLong lastCacheChanged = new AtomicLong();

    private final Set<URL> registered = new ConcurrentHashSet<URL>();

    private final ConcurrentMap<URL, Set<NotifyListener>> subscribed = new ConcurrentHashMap<URL, Set<NotifyListener>>();

    private final ConcurrentMap<URL, List<URL>> notified = new ConcurrentHashMap<URL, List<URL>>();

    public AbstractRegistry(URL url) {
        setUrl(url);
        // 启动文件保存定时器
        syncSaveFile = url.getParameter(Constants.REGISTRY_FILESAVE_SYNC_KEY, false);
        String filename = url.getParameter(Constants.FILE_KEY, System.getProperty("user.home") + "/.dubbo/dubbo-registry-" + url.getHost() + ".cache");
        File file = null;
        if (ConfigUtils.isNotEmpty(filename)) {
            file = new File(filename);
            if(! file.exists() && file.getParentFile() != null && ! file.getParentFile().exists()){
                if(! file.getParentFile().mkdirs()){
                    throw new IllegalArgumentException("Invalid registry store file " + file + ", cause: Failed to create directory " + file.getParentFile() + "!");
                }
            }
        }
        this.file = file;
        loadProperties();
    }

    protected void setUrl(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("registry url == null");
        }
        this.registryUrl = url;
    }

    public URL getUrl() {
        return registryUrl;
    }

    public Set<URL> getRegistered() {
        return registered;
    }

    public Map<URL, Set<NotifyListener>> getSubscribed() {
        return subscribed;
    }

    public Map<URL, List<URL>> getNotified() {
        return notified;
    }

    public File getCacheFile() {
        return file;
    }

    public Properties getCacheProperties() {
        return properties;
    }

    public AtomicLong getLastCacheChanged(){
        return lastCacheChanged;
    }

    private class SaveProperties implements Runnable{
        private long version;
        private SaveProperties(long version){
            this.version = version;
        }
        public void run() {
            doSaveProperties(version);
        }
    }
    
    public void doSaveProperties(long version) {
        if(version < lastCacheChanged.get()){
            return;
        }
        if (file == null) {
            return;
        }
        Properties newProperties = new Properties();
        // 保存之前先读取一遍，防止多个注册中心之间冲突
        InputStream in = null;
        try {
            if (file.exists()) {
                in = new FileInputStream(file);
                newProperties.load(in);
            }
        } catch (Throwable e) {
            logger.warn("Failed to load registry store file, cause: " + e.getMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }
        newProperties.putAll(properties);
        // 保存
        try {
            if (! file.exists()) {
                file.createNewFile();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (logger.isInfoEnabled()) {
                logger.info("Save registry store file " + file + ", data: " + properties);
            }
            try {
                newProperties.store(out, "Dubbo Registry Cache");
                out.flush();
            } finally {
                out.close();
            }
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            try {
                FileChannel channel = raf.getChannel();
                try {
                    FileLock lock = channel.tryLock();
                    if (lock == null) {
                        throw new IOException("Can not lock the registry cache file " + file.getAbsolutePath() + ", ignore and retry later, maybe multi java process use the file, please config: dubbo.regitry.file=xxx.properties");
                    }
                    try {
                        channel.write(ByteBuffer.wrap(out.toByteArray()));
                    } finally {
                        lock.release();
                    }
                } finally {
                    channel.close();
                }
            } finally {
                raf.close();
            }
        } catch (Throwable e) {
            if (version < lastCacheChanged.get()) {
                return;
            } else {
                registryCacheExecutor.execute(new SaveProperties(lastCacheChanged.incrementAndGet()));
            }
            logger.warn("Failed to save registry store file, cause: " + e.getMessage(), e);
        }
    }

    private void loadProperties() {
        if (file != null && file.exists()) {
            InputStream in = null;
            try {
                in = new FileInputStream(file);
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

    public List<URL> getCacheUrls(URL url) {
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (key != null && key.length() > 0 && key.equals(url.getServiceKey())
                    && (Character.isLetter(key.charAt(0)) || key.charAt(0) == '_')
                    && value != null && value.length() > 0) {
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

    public List<URL> lookup(URL url) {
        List<URL> urls= new ArrayList<URL>();
        List<URL> notifiedUrls = getNotified().get(url);
        if (notifiedUrls != null && notifiedUrls.size() > 0) {
            urls.addAll(notifiedUrls);
        }
        if (urls == null || urls.size() == 0) {
            List<URL> cacheUrls = getCacheUrls(url);
            if (cacheUrls != null && cacheUrls.size() > 0) {
                urls.addAll(cacheUrls);
            }
        }
        if (urls == null || urls.size() == 0) {
            for (URL u: getRegistered()) {
                if (UrlUtils.isMatch(url, u)) {
                    urls.add(u);
                }
            }
        }
        if (url.getParameter(Constants.ADMIN_KEY, false)) {
            for (URL u: getSubscribed().keySet()) {
                if (UrlUtils.isMatch(url, u)) {
                    urls.add(u);
                }
            }
        }
        return urls;
    }

    public void register(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("register url == null");
        }
        if (logger.isInfoEnabled()){
            logger.info("Register: " + url);
        }
        registered.add(url);
    }

    public void unregister(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("unregister url == null");
        }
        if (logger.isInfoEnabled()){
            logger.info("Unregister: " + url);
        }
        registered.remove(url);
    }

    public void subscribe(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("subscribe url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("subscribe listener == null");
        }
        if (logger.isInfoEnabled()){
            logger.info("Subscribe: " + url);
        }
        Set<NotifyListener> listeners = subscribed.get(url);
        if (listeners == null) {
            subscribed.putIfAbsent(url, new ConcurrentHashSet<NotifyListener>());
            listeners = subscribed.get(url);
        }
        listeners.add(listener);
    }

    public void unsubscribe(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("unsubscribe url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("unsubscribe listener == null");
        }
        if (logger.isInfoEnabled()){
            logger.info("Unsubscribe: " + url);
        }
        Set<NotifyListener> listeners = subscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    protected void recover() throws Exception {
        // register
        Set<URL> recoverRegistered = new HashSet<URL>(getRegistered());
        if (! recoverRegistered.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover register url " + recoverRegistered);
            }
            for (URL url : recoverRegistered) {
                register(url);
            }
        }
        // subscribe
        Map<URL, Set<NotifyListener>> recoverSubscribed = new HashMap<URL, Set<NotifyListener>>(getSubscribed());
        if (! recoverSubscribed.isEmpty()) {
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

    protected void notify(URL url, NotifyListener listener, List<URL> urls) {
        List<URL> result = new ArrayList<URL>();
        StringBuilder buf = new StringBuilder();
        if (urls != null && urls.size() > 0) {
            for (URL u: urls) {
                if (UrlUtils.isMatch(url, u)) {
                    result.add(u);
                    if (buf.length() > 0) {
                        buf.append(URL_SEPARATOR);
                    }
                    buf.append(u.toFullString());
                }
            }
        }
        properties.setProperty(url.getServiceKey(), buf.toString());
        long version = lastCacheChanged.incrementAndGet();
        if (syncSaveFile) {
            doSaveProperties(version);
        } else {
            registryCacheExecutor.execute(new SaveProperties(version));
        }
        notified.put(url, urls);
        listener.notify(urls);
    }

    public void destroy() {
        if (logger.isInfoEnabled()){
            logger.info("Destroy registry:" + getUrl());
        }
        Set<URL> destroyRegistered = new HashSet<URL>(getRegistered());
        if (! destroyRegistered.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Destroy unregister url " + destroyRegistered);
            }
            for (URL url : new HashSet<URL>(getRegistered())) {
                try {
                    unregister(url);
                } catch (Throwable t) {
                    logger.warn("Failed to unregister url " + url + " to registry " + getUrl() + " on destroy, cause: " + t.getMessage(), t);
                }
            }
        }
        Map<URL, Set<NotifyListener>> destroySubscribed = new HashMap<URL, Set<NotifyListener>>(getSubscribed());
        if (! destroySubscribed.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Destroy unsubscribe url " + destroySubscribed.keySet());
            }
            for (Map.Entry<URL, Set<NotifyListener>> entry : destroySubscribed.entrySet()) {
                URL url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    try {
                        unsubscribe(url, listener);
                    } catch (Throwable t) {
                        logger.warn("Failed to unsubscribe url " + url + " to registry " + getUrl() + " on destroy, cause: " +t.getMessage(), t);
                    }
                }
            }
        }
    }

    public String toString() {
        return getUrl().toString();
    }

}