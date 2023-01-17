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
package org.apache.dubbo.registry.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.ANY_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.FILE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REGISTRY_LOCAL_FILE_CACHE_ENABLED;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_EMPTY_ADDRESS;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_NOTIFY_EVENT;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_DESTROY_UNREGISTER_URL;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_READ_WRITE_CACHE_FILE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_DELETE_LOCKFILE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;
import static org.apache.dubbo.common.constants.RegistryConstants.ACCEPTS_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.DYNAMIC_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.EMPTY_PROTOCOL;
import static org.apache.dubbo.registry.Constants.CACHE;
import static org.apache.dubbo.registry.Constants.DUBBO_REGISTRY;
import static org.apache.dubbo.registry.Constants.REGISTRY_FILESAVE_SYNC_KEY;
import static org.apache.dubbo.registry.Constants.USER_HOME;

/**
 * <p>
 * Provides a fail-safe registry service backed by cache file. The consumer/provider can still find each other when registry center crashed.
 * <p>
 * (SPI, Prototype, ThreadSafe)
 */
public abstract class AbstractRegistry implements Registry {

    // URL address separator, used in file cache, service provider URL separation
    private static final char URL_SEPARATOR = ' ';
    // URL address separated regular expression for parsing the service provider URL list in the file cache
    private static final String URL_SPLIT = "\\s+";
    // Max times to retry to save properties to local cache file
    private static final int MAX_RETRY_TIMES_SAVE_PROPERTIES = 3;
    // Default interval in millisecond for saving properties to local cache file
    private static final long DEFAULT_INTERVAL_SAVE_PROPERTIES = 500L;

    // Log output
    protected final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    // Local disk cache, where the special key value.registries records the list of registry centers, and the others are the list of notified service providers
    private final Properties properties = new Properties();
    // File cache timing writing
    private final ScheduledExecutorService registryCacheExecutor;
    private final AtomicLong lastCacheChanged = new AtomicLong();
    private final AtomicInteger savePropertiesRetryTimes = new AtomicInteger();
    private final Set<URL> registered = new ConcurrentHashSet<>();
    private final ConcurrentMap<URL, Set<NotifyListener>> subscribed = new ConcurrentHashMap<>();
    private final ConcurrentMap<URL, Map<String, List<URL>>> notified = new ConcurrentHashMap<>();
    // Is it synchronized to save the file
    private boolean syncSaveFile;
    private URL registryUrl;
    // Local disk cache file
    private File file;
    private final boolean localCacheEnabled;
    protected RegistryManager registryManager;
    protected ApplicationModel applicationModel;

    private static final String CAUSE_MULTI_DUBBO_USING_SAME_FILE = "multiple Dubbo instance are using the same file";

    protected AbstractRegistry(URL url) {
        setUrl(url);
        registryManager = url.getOrDefaultApplicationModel().getBeanFactory().getBean(RegistryManager.class);
        localCacheEnabled = url.getParameter(REGISTRY_LOCAL_FILE_CACHE_ENABLED, true);
        registryCacheExecutor = url.getOrDefaultFrameworkModel().getBeanFactory()
            .getBean(FrameworkExecutorRepository.class).getSharedScheduledExecutor();
        if (localCacheEnabled) {
            // Start file save timer
            syncSaveFile = url.getParameter(REGISTRY_FILESAVE_SYNC_KEY, false);

            String defaultFilename = System.getProperty(USER_HOME) + DUBBO_REGISTRY + url.getApplication() +
                "-" + url.getAddress().replaceAll(":", "-") + CACHE;

            String filename = url.getParameter(FILE_KEY, defaultFilename);
            File file = null;

            if (ConfigUtils.isNotEmpty(filename)) {
                file = new File(filename);
                if (!file.exists() && file.getParentFile() != null && !file.getParentFile().exists()) {
                    if (!file.getParentFile().mkdirs()) {

                        IllegalArgumentException illegalArgumentException = new IllegalArgumentException(
                            "Invalid registry cache file " + file + ", cause: Failed to create directory " + file.getParentFile() + "!");

                        if (logger != null) {
                            // 1-9 failed to read / save registry cache file.

                            logger.error(REGISTRY_FAILED_READ_WRITE_CACHE_FILE, "cache directory inaccessible",
                                "Try adjusting permission of the directory.",
                                "failed to create directory", illegalArgumentException);
                        }

                        throw illegalArgumentException;
                    }
                }
            }

            this.file = file;

            // When starting the subscription center,
            // we need to read the local cache file for future Registry fault tolerance processing.
            loadProperties();
            notify(url.getBackupUrls());
        }
    }

    protected static List<URL> filterEmpty(URL url, List<URL> urls) {
        if (CollectionUtils.isEmpty(urls)) {
            List<URL> result = new ArrayList<>(1);
            result.add(url.setProtocol(EMPTY_PROTOCOL));
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
        return Collections.unmodifiableSet(registered);
    }

    public Map<URL, Set<NotifyListener>> getSubscribed() {
        return Collections.unmodifiableMap(subscribed);
    }

    public Map<URL, Map<String, List<URL>>> getNotified() {
        return Collections.unmodifiableMap(notified);
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

    public void doSaveProperties(long version) {
        if (version < lastCacheChanged.get()) {
            return;
        }
        if (file == null) {
            return;
        }
        // Save
        File lockfile = null;
        try {
            lockfile = new File(file.getAbsolutePath() + ".lock");
            if (!lockfile.exists()) {
                lockfile.createNewFile();
            }

            try (RandomAccessFile raf = new RandomAccessFile(lockfile, "rw");
                 FileChannel channel = raf.getChannel()) {
                FileLock lock = channel.tryLock();
                if (lock == null) {

                    IOException ioException = new IOException("Can not lock the registry cache file " + file.getAbsolutePath() + ", " +
                        "ignore and retry later, maybe multi java process use the file, please config: dubbo.registry.file=xxx.properties");

                    // 1-9 failed to read / save registry cache file.
                    logger.warn(REGISTRY_FAILED_READ_WRITE_CACHE_FILE, CAUSE_MULTI_DUBBO_USING_SAME_FILE, "",
                        "Adjust dubbo.registry.file.", ioException);

                    throw ioException;
                }

                // Save
                try {
                    if (!file.exists()) {
                        file.createNewFile();
                    }

                    Properties tmpProperties;
                    if (syncSaveFile) {
                        // When syncReport = true, properties.setProperty and properties.store are called from the same
                        // thread(reportCacheExecutor), so deep copy is not required
                        tmpProperties = properties;
                    } else {
                        // Using properties.setProperty and properties.store method will cause lock contention
                        // under multi-threading, so deep copy a new container
                        tmpProperties = new Properties();
                        Set<Map.Entry<Object, Object>> entries = properties.entrySet();
                        for (Map.Entry<Object, Object> entry : entries) {
                            tmpProperties.setProperty((String) entry.getKey(), (String) entry.getValue());
                        }
                    }

                    try (FileOutputStream outputFile = new FileOutputStream(file)) {
                        tmpProperties.store(outputFile, "Dubbo Registry Cache");
                    }
                } finally {
                    lock.release();
                }
            }
        } catch (Throwable e) {
            savePropertiesRetryTimes.incrementAndGet();

            if (savePropertiesRetryTimes.get() >= MAX_RETRY_TIMES_SAVE_PROPERTIES) {
                if (e instanceof OverlappingFileLockException) {
                    // fix #9341, ignore OverlappingFileLockException
                    logger.info("Failed to save registry cache file for file overlapping lock exception, file name " + file.getName());
                } else {
                    // 1-9 failed to read / save registry cache file.
                    logger.warn(REGISTRY_FAILED_READ_WRITE_CACHE_FILE, CAUSE_MULTI_DUBBO_USING_SAME_FILE, "",
                        "Failed to save registry cache file after retrying " + MAX_RETRY_TIMES_SAVE_PROPERTIES + " times, cause: " + e.getMessage(), e);
                }

                savePropertiesRetryTimes.set(0);
                return;
            }

            if (version < lastCacheChanged.get()) {
                savePropertiesRetryTimes.set(0);
                return;
            } else {
                registryCacheExecutor.schedule(() -> doSaveProperties(lastCacheChanged.incrementAndGet()), DEFAULT_INTERVAL_SAVE_PROPERTIES, TimeUnit.MILLISECONDS);
            }

            if (!(e instanceof OverlappingFileLockException)) {
                logger.warn(REGISTRY_FAILED_READ_WRITE_CACHE_FILE, CAUSE_MULTI_DUBBO_USING_SAME_FILE,
                    "However, the retrying count limit is not exceeded. Dubbo will still try.",
                    "Failed to save registry cache file, will retry, cause: " + e.getMessage(), e);
            }
        } finally {
            if (lockfile != null) {
                if (!lockfile.delete()) {
                    // 1-10 Failed to delete lock file.
                    logger.warn(REGISTRY_FAILED_DELETE_LOCKFILE, "", "",
                        String.format("Failed to delete lock file [%s]", lockfile.getName()));
                }
            }
        }
    }

    private void loadProperties() {
        if (file == null || !file.exists()) {
            return;
        }
        try (InputStream in = Files.newInputStream(file.toPath())) {
            properties.load(in);
            if (logger.isInfoEnabled()) {
                logger.info("Loaded registry cache file " + file);
            }
        } catch (IOException e) {
            // 1-9 failed to read / save registry cache file.
            logger.warn(REGISTRY_FAILED_READ_WRITE_CACHE_FILE, CAUSE_MULTI_DUBBO_USING_SAME_FILE, "",
                e.getMessage(), e);

        } catch (Throwable e) {
            // 1-9 failed to read / save registry cache file.
            logger.warn(REGISTRY_FAILED_READ_WRITE_CACHE_FILE, CAUSE_MULTI_DUBBO_USING_SAME_FILE, "",
                "Failed to load registry cache file " + file, e);
        }
    }

    public List<URL> getCacheUrls(URL url) {
        Map<String, List<URL>> categoryNotified = notified.get(url);
        if (CollectionUtils.isNotEmptyMap(categoryNotified)) {
            List<URL> urls = categoryNotified.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
            return urls;
        }

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (StringUtils.isNotEmpty(key) && key.equals(url.getServiceKey()) && (Character.isLetter(key.charAt(0))
                || key.charAt(0) == '_') && StringUtils.isNotEmpty(value)) {
                String[] arr = value.trim().split(URL_SPLIT);
                List<URL> urls = new ArrayList<>();
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
        List<URL> result = new ArrayList<>();
        Map<String, List<URL>> notifiedUrls = getNotified().get(url);
        if (CollectionUtils.isNotEmptyMap(notifiedUrls)) {
            for (List<URL> urls : notifiedUrls.values()) {
                for (URL u : urls) {
                    if (!EMPTY_PROTOCOL.equals(u.getProtocol())) {
                        result.add(u);
                    }
                }
            }
        } else {
            final AtomicReference<List<URL>> reference = new AtomicReference<>();
            NotifyListener listener = reference::set;
            subscribe(url, listener); // Subscribe logic guarantees the first notify to return
            List<URL> urls = reference.get();
            if (CollectionUtils.isNotEmpty(urls)) {
                for (URL u : urls) {
                    if (!EMPTY_PROTOCOL.equals(u.getProtocol())) {
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
        if (url.getPort() != 0) {
            if (logger.isInfoEnabled()) {
                logger.info("Register: " + url);
            }
        }
        registered.add(url);
    }

    @Override
    public void unregister(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("unregister url == null");
        }
        if (url.getPort() != 0) {
            if (logger.isInfoEnabled()) {
                logger.info("Unregister: " + url);
            }
        }
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
        Set<NotifyListener> listeners = subscribed.computeIfAbsent(url, n -> new ConcurrentHashSet<>());
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
        Set<NotifyListener> listeners = subscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }

        // do not forget remove notified
        notified.remove(url);
    }

    protected void recover() throws Exception {
        // register
        Set<URL> recoverRegistered = new HashSet<>(getRegistered());
        if (!recoverRegistered.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover register url " + recoverRegistered);
            }
            for (URL url : recoverRegistered) {
                register(url);
            }
        }
        // subscribe
        Map<URL, Set<NotifyListener>> recoverSubscribed = new HashMap<>(getSubscribed());
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

    protected void notify(List<URL> urls) {
        if (CollectionUtils.isEmpty(urls)) {
            return;
        }

        for (Map.Entry<URL, Set<NotifyListener>> entry : getSubscribed().entrySet()) {
            URL url = entry.getKey();

            if (!UrlUtils.isMatch(url, urls.get(0))) {
                continue;
            }

            Set<NotifyListener> listeners = entry.getValue();
            if (listeners != null) {
                for (NotifyListener listener : listeners) {
                    try {
                        notify(url, listener, filterEmpty(url, urls));
                    } catch (Throwable t) {
                        // 1-7: Failed to notify registry event.
                        logger.error(REGISTRY_FAILED_NOTIFY_EVENT, "consumer is offline", "",
                            "Failed to notify registry event, urls: " + urls + ", cause: " + t.getMessage(), t);
                    }
                }
            }
        }
    }

    /**
     * Notify changes from the provider side.
     *
     * @param url      consumer side url
     * @param listener listener
     * @param urls     provider latest urls
     */
    protected void notify(URL url, NotifyListener listener, List<URL> urls) {
        if (url == null) {
            throw new IllegalArgumentException("notify url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("notify listener == null");
        }
        if ((CollectionUtils.isEmpty(urls)) && !ANY_VALUE.equals(url.getServiceInterface())) {
            // 1-4 Empty address.
            logger.warn(REGISTRY_EMPTY_ADDRESS, "", "", "Ignore empty notify urls for subscribe url " + url);
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("Notify urls for subscribe url " + url + ", url size: " + urls.size());
        }
        // keep every provider's category.
        Map<String, List<URL>> result = new HashMap<>();
        for (URL u : urls) {
            if (UrlUtils.isMatch(url, u)) {
                String category = u.getCategory(DEFAULT_CATEGORY);
                List<URL> categoryList = result.computeIfAbsent(category, k -> new ArrayList<>());
                categoryList.add(u);
            }
        }
        if (result.size() == 0) {
            return;
        }
        Map<String, List<URL>> categoryNotified = notified.computeIfAbsent(url, u -> new ConcurrentHashMap<>());
        for (Map.Entry<String, List<URL>> entry : result.entrySet()) {
            String category = entry.getKey();
            List<URL> categoryList = entry.getValue();
            categoryNotified.put(category, categoryList);
            listener.notify(categoryList);

            // We will update our cache file after each notification.
            // When our Registry has a subscribed failure due to network jitter, we can return at least the existing cache URL.
            if (localCacheEnabled) {
                saveProperties(url);
            }
        }
    }

    private void saveProperties(URL url) {
        if (file == null) {
            return;
        }

        try {
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
            properties.setProperty(url.getServiceKey(), buf.toString());
            long version = lastCacheChanged.incrementAndGet();
            if (syncSaveFile) {
                doSaveProperties(version);
            } else {
                registryCacheExecutor.schedule(() -> doSaveProperties(version), DEFAULT_INTERVAL_SAVE_PROPERTIES, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            logger.warn(INTERNAL_ERROR, "unknown error in registry module", "", t.getMessage(), t);
        }
    }

    @Override
    public void destroy() {
        if (logger.isInfoEnabled()) {
            logger.info("Destroy registry:" + getUrl());
        }
        Set<URL> destroyRegistered = new HashSet<>(getRegistered());
        if (!destroyRegistered.isEmpty()) {
            for (URL url : new HashSet<>(destroyRegistered)) {
                if (url.getParameter(DYNAMIC_KEY, true)) {
                    try {
                        unregister(url);
                        if (logger.isInfoEnabled()) {
                            logger.info("Destroy unregister url " + url);
                        }
                    } catch (Throwable t) {
                        // 1-8: Failed to unregister / unsubscribe url on destroy.
                        logger.warn(REGISTRY_FAILED_DESTROY_UNREGISTER_URL, "", "",
                            "Failed to unregister url " + url + " to registry " + getUrl() + " on destroy, cause: " + t.getMessage(), t);
                    }
                }
            }
        }
        Map<URL, Set<NotifyListener>> destroySubscribed = new HashMap<>(getSubscribed());
        if (!destroySubscribed.isEmpty()) {
            for (Map.Entry<URL, Set<NotifyListener>> entry : destroySubscribed.entrySet()) {
                URL url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    try {
                        unsubscribe(url, listener);
                        if (logger.isInfoEnabled()) {
                            logger.info("Destroy unsubscribe url " + url);
                        }
                    } catch (Throwable t) {
                        // 1-8: Failed to unregister / unsubscribe url on destroy.
                        logger.warn(REGISTRY_FAILED_DESTROY_UNREGISTER_URL, "", "",
                            "Failed to unsubscribe url " + url + " to registry " + getUrl() + " on destroy, cause: " + t.getMessage(), t);
                    }
                }
            }
        }
        registryManager.removeDestroyedRegistry(this);
    }

    protected boolean acceptable(URL urlToRegistry) {
        String pattern = registryUrl.getParameter(ACCEPTS_KEY);
        if (StringUtils.isEmpty(pattern)) {
            return true;
        }

        String[] accepts = COMMA_SPLIT_PATTERN.split(pattern);

        Set<String> allow = Arrays.stream(accepts).filter(p -> !p.startsWith("-")).collect(Collectors.toSet());
        Set<String> disAllow = Arrays.stream(accepts).filter(p -> p.startsWith("-")).map(p -> p.substring(1)).collect(Collectors.toSet());

        if (CollectionUtils.isNotEmpty(allow)) {
            // allow first
            return allow.contains(urlToRegistry.getProtocol());
        } else if (CollectionUtils.isNotEmpty(disAllow)) {
            // contains disAllow, deny
            return !disAllow.contains(urlToRegistry.getProtocol());
        } else {
            // default allow
            return true;
        }
    }

    @Override
    public String toString() {
        return getUrl().toString();
    }
}
