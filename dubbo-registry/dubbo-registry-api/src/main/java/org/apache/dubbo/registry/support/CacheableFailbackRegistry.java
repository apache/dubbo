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
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.URLStrParser;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.common.url.component.DubboServiceAddressURL;
import org.apache.dubbo.common.url.component.ServiceAddressURL;
import org.apache.dubbo.common.url.component.URLAddress;
import org.apache.dubbo.common.url.component.URLParam;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.rpc.model.ScopeModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.URLStrParser.ENCODED_AND_MARK;
import static org.apache.dubbo.common.URLStrParser.ENCODED_PID_KEY;
import static org.apache.dubbo.common.URLStrParser.ENCODED_QUESTION_MARK;
import static org.apache.dubbo.common.URLStrParser.ENCODED_TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CACHE_CLEAR_TASK_INTERVAL;
import static org.apache.dubbo.common.constants.CommonConstants.CACHE_CLEAR_WAITING_THRESHOLD;
import static org.apache.dubbo.common.constants.CommonConstants.CHECK_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_SEPARATOR_ENCODED;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_PROPERTY_TYPE_MISMATCH;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_EMPTY_ADDRESS;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_CLEAR_CACHED_URLS;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_URL_EVICTING;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_NO_PARAMETERS_URL;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_ENABLE_EMPTY_PROTECTION;
import static org.apache.dubbo.common.constants.RegistryConstants.EMPTY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.ENABLE_EMPTY_PROTECTION_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDERS_CATEGORY;

/**
 * <p>
 * Based on FailbackRegistry, it adds a URLAddress and URLParam cache to save RAM space.
 *
 * <p>
 * It's useful for registries whose sdk returns raw string as provider instance. For example, Zookeeper and etcd.
 *
 * @see org.apache.dubbo.registry.support.FailbackRegistry
 * @see org.apache.dubbo.registry.support.AbstractRegistry
 */
public abstract class CacheableFailbackRegistry extends FailbackRegistry {
    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(CacheableFailbackRegistry.class);

    private static String[] VARIABLE_KEYS = new String[] {ENCODED_TIMESTAMP_KEY, ENCODED_PID_KEY};

    protected ConcurrentMap<String, URLAddress> stringAddress = new ConcurrentHashMap<>();
    protected ConcurrentMap<String, URLParam> stringParam = new ConcurrentHashMap<>();

    private ScheduledExecutorService cacheRemovalScheduler;
    private int cacheRemovalTaskIntervalInMillis;
    private int cacheClearWaitingThresholdInMillis;

    private Map<ServiceAddressURL, Long> waitForRemove = new ConcurrentHashMap<>();
    private Semaphore semaphore = new Semaphore(1);

    private final Map<String, String> extraParameters;
    protected final Map<URL, Map<String, ServiceAddressURL>> stringUrls = new ConcurrentHashMap<>();

    protected CacheableFailbackRegistry(URL url) {
        super(url);
        extraParameters = new HashMap<>(8);
        extraParameters.put(CHECK_KEY, String.valueOf(false));

        cacheRemovalScheduler = url.getOrDefaultFrameworkModel()
                .getBeanFactory()
                .getBean(FrameworkExecutorRepository.class)
                .nextScheduledExecutor();
        cacheRemovalTaskIntervalInMillis = getIntConfig(url.getScopeModel(), CACHE_CLEAR_TASK_INTERVAL, 2 * 60 * 1000);
        cacheClearWaitingThresholdInMillis =
                getIntConfig(url.getScopeModel(), CACHE_CLEAR_WAITING_THRESHOLD, 5 * 60 * 1000);
    }

    protected static int getIntConfig(ScopeModel scopeModel, String key, int def) {
        String str = ConfigurationUtils.getProperty(scopeModel, key);
        int result = def;
        if (StringUtils.isNotEmpty(str)) {
            try {
                result = Integer.parseInt(str);
            } catch (NumberFormatException e) {
                // 0-2 Property type mismatch.

                logger.warn(
                        COMMON_PROPERTY_TYPE_MISMATCH,
                        "typo in property value",
                        "This property requires an integer value.",
                        "Invalid registry properties configuration key " + key + ", value " + str);
            }
        }
        return result;
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        this.evictURLCache(url);
    }

    protected void evictURLCache(URL url) {
        Map<String, ServiceAddressURL> oldURLs = stringUrls.remove(url);
        try {
            if (CollectionUtils.isNotEmptyMap(oldURLs)) {
                logger.info("Evicting urls for service " + url.getServiceKey() + ", size " + oldURLs.size());
                Long currentTimestamp = System.currentTimeMillis();
                for (Map.Entry<String, ServiceAddressURL> entry : oldURLs.entrySet()) {
                    waitForRemove.put(entry.getValue(), currentTimestamp);
                }
                if (CollectionUtils.isNotEmptyMap(waitForRemove)) {
                    if (semaphore.tryAcquire()) {
                        cacheRemovalScheduler.schedule(
                                new RemovalTask(), cacheRemovalTaskIntervalInMillis, TimeUnit.MILLISECONDS);
                    }
                }
            }
        } catch (Exception e) {
            // It seems that the most possible statement that causes exception is the 'schedule()' method.

            // The executor that FrameworkExecutorRepository.nextScheduledExecutor() method returns
            // is made by Executors.newSingleThreadScheduledExecutor().

            // After observing the code of ScheduledThreadPoolExecutor.delayedExecute,
            // it seems that it only throws RejectedExecutionException when the thread pool is shutdown.

            // When? FrameworkExecutorRepository gets destroyed.

            // 1-3: URL evicting failed.
            logger.warn(
                    REGISTRY_FAILED_URL_EVICTING,
                    "thread pool getting destroyed",
                    "",
                    "Failed to evict url for " + url.getServiceKey(),
                    e);
        }
    }

    protected List<URL> toUrlsWithoutEmpty(URL consumer, Collection<String> providers) {
        // keep old urls
        Map<String, ServiceAddressURL> oldURLs = stringUrls.get(consumer);

        // create new urls
        // The key of newURLs is the normalized rawProvider (variable keys removed for deduplication),
        // but the cached ServiceAddressURL is built from the original rawProvider (with all parameters preserved).
        Map<String, ServiceAddressURL> newURLs = new HashMap<>((int) (providers.size() / 0.75f + 1));

        // remove 'release', 'dubbo', 'methods', timestamp, 'dubbo.tag' parameter
        // in consumer URL.
        URL copyOfConsumer = removeParamsFromConsumer(consumer);

        if (oldURLs == null) {
            for (String rawProvider : providers) {
                // Normalize the rawProvider by removing VARIABLE_KEYS(timestamp, pid..) for deduplication purposes.
                // The normalized key is used to avoid duplicate instances of the same provider.
                String normalizedKey = stripOffVariableKeys(rawProvider);

                // Create DubboServiceAddress object using the ORIGINAL rawProvider (with all parameters intact),
                // so that timestamp and other parameters are preserved for correct parameter parsing.
                // This ensures that when multiple providers normalize to the same key (e.g., different timestamps),
                // the latest one's timestamp will be used.
                ServiceAddressURL cachedURL = createURL(rawProvider, copyOfConsumer, getExtraParameters());
                if (cachedURL == null) {
                    continue;
                }

                // Use normalized key for deduplication: if multiple providers normalize to the same key,
                // the last one (with the latest timestamp) will be kept.
                newURLs.put(normalizedKey, cachedURL);
            }
        } else {
            // Reuse or create URLs based on both normalized key and original rawProvider.
            // The normalized key is used to find potential cache entries for reuse (deduplication),
            // but we always create a new ServiceAddressURL from the original rawProvider to ensure
            // the timestamp and other parameters are up-to-date.
            for (String rawProvider : providers) {
                // Normalize the rawProvider for cache key matching and deduplication.
                String normalizedKey = stripOffVariableKeys(rawProvider);
                ServiceAddressURL cachedURL = oldURLs.remove(normalizedKey);
                if (cachedURL == null) {
                    // Create new URL using the original rawProvider (all parameters preserved including timestamp).
                    cachedURL = createURL(rawProvider, copyOfConsumer, getExtraParameters());
                    if (cachedURL == null) {
                        continue;
                    }
                }
                // Use normalized key for storage: if multiple providers normalize to the same key,
                // the last one will be kept, ensuring no duplicate instances with outdated timestamps.
                newURLs.put(normalizedKey, cachedURL);
            }
        }

        evictURLCache(consumer);
        stringUrls.put(consumer, newURLs);

        return new ArrayList<>(newURLs.values());
    }

    protected List<URL> toUrlsWithEmpty(URL consumer, String path, Collection<String> providers) {
        List<URL> urls = new ArrayList<>(1);
        boolean isProviderPath = path.endsWith(PROVIDERS_CATEGORY);

        if (isProviderPath) {
            if (CollectionUtils.isNotEmpty(providers)) {
                urls = toUrlsWithoutEmpty(consumer, providers);
            } else {
                // clear cache on empty notification: unsubscribe or provider offline
                evictURLCache(consumer);
            }
        } else {
            if (CollectionUtils.isNotEmpty(providers)) {
                urls = toConfiguratorsWithoutEmpty(consumer, providers);
            }
        }

        if (urls.isEmpty()) {
            int i = path.lastIndexOf(PATH_SEPARATOR);
            String category = i < 0 ? path : path.substring(i + 1);
            if (!PROVIDERS_CATEGORY.equals(category)
                    || !getUrl().getParameter(ENABLE_EMPTY_PROTECTION_KEY, DEFAULT_ENABLE_EMPTY_PROTECTION)) {
                if (PROVIDERS_CATEGORY.equals(category)) {
                    logger.warn(
                            REGISTRY_EMPTY_ADDRESS,
                            "",
                            "",
                            "Service " + consumer.getServiceKey()
                                    + " received empty address list and empty protection is disabled, will clear current available addresses");
                }
                URL empty = URLBuilder.from(consumer)
                        .setProtocol(EMPTY_PROTOCOL)
                        .addParameter(CATEGORY_KEY, category)
                        .build();
                urls.add(empty);
            }
        }

        return urls;
    }

    /**
     * Create DubboServiceAddress object using provider url, consumer url, and extra parameters.
     *
     * @param rawProvider     provider url string
     * @param consumerURL     URL object of consumer
     * @param extraParameters extra parameters
     * @return created DubboServiceAddressURL object
     */
    protected ServiceAddressURL createURL(String rawProvider, URL consumerURL, Map<String, String> extraParameters) {

        boolean encoded = true;

        // use encoded value directly to avoid URLDecoder.decode allocation.
        int paramStartIdx = rawProvider.indexOf(ENCODED_QUESTION_MARK);

        if (paramStartIdx == -1) {
            // if ENCODED_QUESTION_MARK does not show, mark as not encoded.
            encoded = false;
        }

        // split by (encoded) question mark.
        // part[0] => protocol + ip address + interface.
        // part[1] => parameters (metadata).
        String[] parts = URLStrParser.parseRawURLToArrays(rawProvider, paramStartIdx);

        if (parts.length <= 1) {
            // 1-5 Received URL without any parameters.
            logger.warn(REGISTRY_NO_PARAMETERS_URL, "", "", "Received url without any parameters " + rawProvider);

            return DubboServiceAddressURL.valueOf(rawProvider, consumerURL);
        }

        String rawAddress = parts[0];
        String rawParams = parts[1];

        // Workaround for 'effectively final': duplicate the encoded variable.
        boolean isEncoded = encoded;

        // PathURLAddress if it's using dubbo protocol.
        URLAddress address = ConcurrentHashMapUtils.computeIfAbsent(
                stringAddress, rawAddress, k -> URLAddress.parse(k, getDefaultURLProtocol(), isEncoded));
        address.setTimestamp(System.currentTimeMillis());

        URLParam param = ConcurrentHashMapUtils.computeIfAbsent(
                stringParam, rawParams, k -> URLParam.parse(k, isEncoded, extraParameters));
        param.setTimestamp(System.currentTimeMillis());

        // create service URL using cached address, param, and consumer URL.
        ServiceAddressURL cachedServiceAddressURL = createServiceURL(address, param, consumerURL);

        if (isMatch(consumerURL, cachedServiceAddressURL)) {
            return cachedServiceAddressURL;
        }

        return null;
    }

    protected ServiceAddressURL createServiceURL(URLAddress address, URLParam param, URL consumerURL) {
        return new DubboServiceAddressURL(address, param, consumerURL, null);
    }

    private String stripOffVariableKeys(String rawProvider) {
        String[] keys = getVariableKeys();
        if (keys == null || keys.length == 0) {
            return rawProvider;
        }

        int encodedIdx = rawProvider.indexOf(ENCODED_QUESTION_MARK);
        int plainIdx = rawProvider.indexOf('?');
        boolean encoded;
        int questionIdx;
        if (encodedIdx >= 0 && (plainIdx < 0 || encodedIdx < plainIdx)) {
            encoded = true;
            questionIdx = encodedIdx;
        } else if (plainIdx >= 0) {
            encoded = false;
            questionIdx = plainIdx;
        } else {
            return rawProvider;
        }

        String prefix = rawProvider.substring(0, questionIdx);
        String params = rawProvider.substring(questionIdx + (encoded ? ENCODED_QUESTION_MARK.length() : 1));
        if (params.isEmpty()) {
            return rawProvider;
        }

        String andMark = encoded ? ENCODED_AND_MARK : "&";
        String eqMark = encoded ? "%3D" : "=";

        // Build a set of normalized variable names for efficient matching.
        HashSet<String> variableNames = new HashSet<>(keys.length);
        for (String key : keys) {
            if (key != null) {
                variableNames.add(normalizeVariableName(key));
            }
        }

        List<String> remaining = new ArrayList<>();
        boolean removed = false;
        for (String param : params.split(andMark)) {
            if (param.isEmpty()) {
                continue;
            }
            int eqIdx = param.indexOf(eqMark);
            if (eqIdx < 0) {
                remaining.add(param);
                continue;
            }
            String name = param.substring(0, eqIdx);
            String normalizedName = normalizeVariableName(name);

            // Check if this parameter name exactly matches a variable key that should be removed.
            // Only exact match is performed (e.g., "timestamp" matches "timestamp", but not "remote.timestamp").
            if (variableNames.contains(normalizedName)) {
                removed = true;
                continue;
            }
            remaining.add(param);
        }

        if (!removed) {
            return rawProvider;
        }
        if (remaining.isEmpty()) {
            return prefix;
        }

        return prefix + (encoded ? ENCODED_QUESTION_MARK : "?") + String.join(andMark, remaining);
    }

    private String normalizeVariableName(String key) {
        if (key == null) {
            return null;
        }
        if (key.endsWith("%3D")) {
            return key.substring(0, key.length() - 3);
        }
        if (key.endsWith("=")) {
            return key.substring(0, key.length() - 1);
        }
        return key;
    }

    private List<URL> toConfiguratorsWithoutEmpty(URL consumer, Collection<String> configurators) {
        List<URL> urls = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(configurators)) {
            for (String provider : configurators) {
                if (provider.contains(PROTOCOL_SEPARATOR_ENCODED)) {
                    URL url = URLStrParser.parseEncodedStr(provider);
                    if (UrlUtils.isMatch(consumer, url)) {
                        urls.add(url);
                    }
                }
            }
        }
        return urls;
    }

    protected Map<String, String> getExtraParameters() {
        return extraParameters;
    }

    protected String[] getVariableKeys() {
        return VARIABLE_KEYS;
    }

    protected String getDefaultURLProtocol() {
        return DUBBO;
    }

    /**
     * This method is for unit test to see if the RemovalTask has completed or not.<br />
     * <strong>Please do not call this method in other places.</strong>
     */
    @Deprecated
    protected Semaphore getSemaphore() {
        return semaphore;
    }

    protected abstract boolean isMatch(URL subscribeUrl, URL providerUrl);

    /**
     * The cached URL removal task, which will be run on a scheduled thread pool. (It will be run after a delay.)
     */
    private class RemovalTask implements Runnable {
        @Override
        public void run() {
            logger.info("Clearing cached URLs, waiting to clear size " + waitForRemove.size());
            int clearCount = 0;
            try {
                Iterator<Map.Entry<ServiceAddressURL, Long>> it =
                        waitForRemove.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<ServiceAddressURL, Long> entry = it.next();
                    ServiceAddressURL removeURL = entry.getKey();
                    long removeTime = entry.getValue();
                    long current = System.currentTimeMillis();
                    if (current - removeTime >= cacheClearWaitingThresholdInMillis) {
                        URLAddress urlAddress = removeURL.getUrlAddress();
                        URLParam urlParam = removeURL.getUrlParam();
                        if (current - urlAddress.getTimestamp() >= cacheClearWaitingThresholdInMillis) {
                            stringAddress.remove(urlAddress.getRawAddress());
                        }
                        if (current - urlParam.getTimestamp() >= cacheClearWaitingThresholdInMillis) {
                            stringParam.remove(urlParam.getRawParam());
                        }
                        it.remove();
                        clearCount++;
                    }
                }
            } catch (Throwable t) {
                // 1-6 Error when clearing cached URLs.

                logger.error(REGISTRY_FAILED_CLEAR_CACHED_URLS, "", "", "Error occurred when clearing cached URLs", t);

            } finally {
                semaphore.release();
            }
            logger.info("Clear cached URLs, size " + clearCount);

            if (CollectionUtils.isNotEmptyMap(waitForRemove)) {
                // move to next schedule
                if (semaphore.tryAcquire()) {
                    cacheRemovalScheduler.schedule(
                            new RemovalTask(), cacheRemovalTaskIntervalInMillis, TimeUnit.MILLISECONDS);
                }
            }
        }
    }
}
