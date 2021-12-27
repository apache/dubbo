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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.url.component.DubboServiceAddressURL;
import org.apache.dubbo.common.url.component.ServiceAddressURL;
import org.apache.dubbo.common.url.component.URLAddress;
import org.apache.dubbo.common.url.component.URLParam;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.ProviderFirstParams;
import org.apache.dubbo.rpc.model.ScopeModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.EMPTY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.ENABLE_EMPTY_PROTECTION_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDERS_CATEGORY;

/**
 * Useful for registries who's sdk returns raw string as provider instance, for example, zookeeper and etcd.
 */
public abstract class CacheableFailbackRegistry extends FailbackRegistry {
    private static final Logger logger = LoggerFactory.getLogger(CacheableFailbackRegistry.class);
    private static String[] VARIABLE_KEYS = new String[]{ENCODED_TIMESTAMP_KEY, ENCODED_PID_KEY};

    protected Map<String, URLAddress> stringAddress = new ConcurrentHashMap<>();
    protected Map<String, URLParam> stringParam = new ConcurrentHashMap<>();
    private ScheduledExecutorService cacheRemovalScheduler;
    private int cacheRemovalTaskIntervalInMillis;
    private int cacheClearWaitingThresholdInMillis;
    private Map<ServiceAddressURL, Long> waitForRemove = new ConcurrentHashMap<>();
    private Semaphore semaphore = new Semaphore(1);

    private final Map<String, String> extraParameters;
    protected final Map<URL, Map<String, ServiceAddressURL>> stringUrls = new ConcurrentHashMap<>();

    public CacheableFailbackRegistry(URL url) {
        super(url);
        extraParameters = new HashMap<>(8);
        extraParameters.put(CHECK_KEY, String.valueOf(false));

        cacheRemovalScheduler = url.getOrDefaultApplicationModel().getExtensionLoader(ExecutorRepository.class).getDefaultExtension().nextScheduledExecutor();
        cacheRemovalTaskIntervalInMillis = getIntConfig(url.getScopeModel(), CACHE_CLEAR_TASK_INTERVAL, 2 * 60 * 1000);
        cacheClearWaitingThresholdInMillis = getIntConfig(url.getScopeModel(), CACHE_CLEAR_WAITING_THRESHOLD, 5 * 60 * 1000);
    }

    protected static int getIntConfig(ScopeModel scopeModel, String key, int def) {
        String str = ConfigurationUtils.getProperty(scopeModel, key);
        int result = def;
        if (StringUtils.isNotEmpty(str)) {
            try {
                result = Integer.parseInt(str);
            } catch (NumberFormatException e) {
                logger.warn("Invalid registry properties configuration key " + key + ", value " + str);
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
            if (oldURLs != null && oldURLs.size() > 0) {
                logger.info("Evicting urls for service " + url.getServiceKey() + ", size " + oldURLs.size());
                Long currentTimestamp = System.currentTimeMillis();
                for (Map.Entry<String, ServiceAddressURL> entry : oldURLs.entrySet()) {
                    waitForRemove.put(entry.getValue(), currentTimestamp);
                }
                if (CollectionUtils.isNotEmptyMap(waitForRemove)) {
                    if (semaphore.tryAcquire()) {
                        cacheRemovalScheduler.schedule(new RemovalTask(), cacheRemovalTaskIntervalInMillis, TimeUnit.MILLISECONDS);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to evict url for " + url.getServiceKey(), e);
        }
    }

    protected List<URL> toUrlsWithoutEmpty(URL consumer, Collection<String> providers) {
        // keep old urls
        Map<String, ServiceAddressURL> oldURLs = stringUrls.get(consumer);
        // create new urls
        Map<String, ServiceAddressURL> newURLs;
        URL copyOfConsumer = removeParamsFromConsumer(consumer);
        if (oldURLs == null) {
            newURLs = new HashMap<>((int) (providers.size() / 0.75f + 1));
            for (String rawProvider : providers) {
                rawProvider = stripOffVariableKeys(rawProvider);
                ServiceAddressURL cachedURL = createURL(rawProvider, copyOfConsumer, getExtraParameters());
                if (cachedURL == null) {
                    logger.warn("Invalid address, failed to parse into URL " + rawProvider);
                    continue;
                }
                newURLs.put(rawProvider, cachedURL);
            }
        } else {
            newURLs = new HashMap<>((int) (providers.size() / 0.75f + 1));
            // maybe only default , or "env" + default
            for (String rawProvider : providers) {
                rawProvider = stripOffVariableKeys(rawProvider);
                ServiceAddressURL cachedURL = oldURLs.remove(rawProvider);
                if (cachedURL == null) {
                    cachedURL = createURL(rawProvider, copyOfConsumer, getExtraParameters());
                    if (cachedURL == null) {
                        logger.warn("Invalid address, failed to parse into URL " + rawProvider);
                        continue;
                    }
                }
                newURLs.put(rawProvider, cachedURL);
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
            if (!PROVIDERS_CATEGORY.equals(category) || !getUrl().getParameter(ENABLE_EMPTY_PROTECTION_KEY, true)) {
                if (PROVIDERS_CATEGORY.equals(category)) {
                    logger.warn("Service " + consumer.getServiceKey() + " received empty address list and empty protection is disabled, will clear current available addresses");
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

    protected ServiceAddressURL createURL(String rawProvider, URL consumerURL, Map<String, String> extraParameters) {
        boolean encoded = true;
        // use encoded value directly to avoid URLDecoder.decode allocation.
        int paramStartIdx = rawProvider.indexOf(ENCODED_QUESTION_MARK);
        if (paramStartIdx == -1) {// if ENCODED_QUESTION_MARK does not show, mark as not encoded.
            encoded = false;
        }
        String[] parts = URLStrParser.parseRawURLToArrays(rawProvider, paramStartIdx);
        if (parts.length <= 1) {
            logger.warn("Received url without any parameters " + rawProvider);
            return DubboServiceAddressURL.valueOf(rawProvider, consumerURL);
        }

        String rawAddress = parts[0];
        String rawParams = parts[1];
        boolean isEncoded = encoded;
        URLAddress address = stringAddress.computeIfAbsent(rawAddress, k -> URLAddress.parse(k, getDefaultURLProtocol(), isEncoded));
        address.setTimestamp(System.currentTimeMillis());

        URLParam param = stringParam.computeIfAbsent(rawParams, k -> URLParam.parse(k, isEncoded, extraParameters));
        param.setTimestamp(System.currentTimeMillis());

        ServiceAddressURL cachedURL = createServiceURL(address, param, consumerURL);
        if (isMatch(consumerURL, cachedURL)) {
            return cachedURL;
        }
        return null;
    }


    protected ServiceAddressURL createServiceURL(URLAddress address, URLParam param, URL consumerURL) {
        return new DubboServiceAddressURL(address, param, consumerURL, null);
    }

    protected URL removeParamsFromConsumer(URL consumer) {
        Set<ProviderFirstParams> providerFirstParams = consumer.getOrDefaultApplicationModel().getExtensionLoader(ProviderFirstParams.class).getSupportedExtensionInstances();
        if (CollectionUtils.isEmpty(providerFirstParams)) {
            return consumer;
        }

        for (ProviderFirstParams paramsFilter : providerFirstParams) {
            consumer = consumer.removeParameters(paramsFilter.params());
        }
        return consumer;
    }

    private String stripOffVariableKeys(String rawProvider) {
        String[] keys = getVariableKeys();
        if (keys == null || keys.length == 0) {
            return rawProvider;
        }

        for (String key : keys) {
            int idxStart = rawProvider.indexOf(key);
            if (idxStart == -1) {
                continue;
            }
            int idxEnd = rawProvider.indexOf(ENCODED_AND_MARK, idxStart);
            String part1 = rawProvider.substring(0, idxStart);
            if (idxEnd == -1) {
                rawProvider = part1;
            } else {
                String part2 = rawProvider.substring(idxEnd + ENCODED_AND_MARK.length());
                rawProvider = part1 + part2;
            }
        }

        if (rawProvider.endsWith(ENCODED_AND_MARK)) {
            rawProvider = rawProvider.substring(0, rawProvider.length() - ENCODED_AND_MARK.length());
        }
        if (rawProvider.endsWith(ENCODED_QUESTION_MARK)) {
            rawProvider = rawProvider.substring(0, rawProvider.length() - ENCODED_QUESTION_MARK.length());
        }

        return rawProvider;
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


    private class RemovalTask implements Runnable {
        @Override
        public void run() {
            logger.info("Clearing cached URLs, waiting to clear size " + waitForRemove.size());
            int clearCount = 0;
            try {
                Iterator<Map.Entry<ServiceAddressURL, Long>> it = waitForRemove.entrySet().iterator();
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
                logger.error("Error occurred when clearing cached URLs", t);
            } finally {
                semaphore.release();
            }
            logger.info("Clear cached URLs, size " + clearCount);

            if (CollectionUtils.isNotEmptyMap(waitForRemove)) {
                // move to next schedule
                if (semaphore.tryAcquire()) {
                    cacheRemovalScheduler.schedule(new RemovalTask(), cacheRemovalTaskIntervalInMillis, TimeUnit.MILLISECONDS);
                }
            }
        }
    }
}
