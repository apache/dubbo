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
package org.apache.dubbo.registry.nacos.util;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.nacos.NacosConnectionManager;
import org.apache.dubbo.registry.nacos.NacosNamingServiceWrapper;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.utils.NamingUtils;

import static com.alibaba.nacos.api.common.Constants.DEFAULT_GROUP;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_NACOS_EXCEPTION;

/**
 * The utilities class for {@link NamingService}
 *
 * @since 2.7.5
 */
public class NacosNamingServiceUtils {

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(NacosNamingServiceUtils.class);
    private static final String NACOS_GROUP_KEY = "nacos.group";

    private static final String NACOS_RETRY_KEY = "nacos.retry";

    private static final String NACOS_RETRY_WAIT_KEY = "nacos.retry-wait";

    private static final String NACOS_CHECK_KEY = "nacos.check";

    // Keeps track of the shared wrapper instance and how many registries are using it.
    private static final class NacosNamingServiceHolder {
        final NacosNamingServiceWrapper wrapper;

        // Atomic counter to track usage.
        final AtomicInteger refCount;

        NacosNamingServiceHolder(NacosNamingServiceWrapper wrapper) {
            this.wrapper = wrapper;
            this.refCount = new AtomicInteger(1);
        }
    }

    private static final ConcurrentMap<String, NacosNamingServiceHolder> SERVICE_CACHE = new ConcurrentHashMap<>();

    private NacosNamingServiceUtils() {
        throw new IllegalStateException("NacosNamingServiceUtils should not be instantiated");
    }

    /**
     * Convert the {@link ServiceInstance} to {@link Instance}
     *
     * @param serviceInstance {@link ServiceInstance}
     * @return non-null
     * @since 2.7.5
     */
    public static Instance toInstance(ServiceInstance serviceInstance) {
        Instance instance = new Instance();
        instance.setServiceName(serviceInstance.getServiceName());
        instance.setIp(serviceInstance.getHost());
        instance.setPort(serviceInstance.getPort());
        instance.setMetadata(serviceInstance.getSortedMetadata());
        instance.setEnabled(serviceInstance.isEnabled());
        instance.setHealthy(serviceInstance.isHealthy());
        return instance;
    }

    /**
     * Convert the {@link Instance} to {@link ServiceInstance}
     *
     * @param instance {@link Instance}
     * @return non-null
     * @since 2.7.5
     */
    public static ServiceInstance toServiceInstance(URL registryUrl, Instance instance) {
        DefaultServiceInstance serviceInstance = new DefaultServiceInstance(
                NamingUtils.getServiceName(instance.getServiceName()),
                instance.getIp(),
                instance.getPort(),
                ScopeModelUtil.getApplicationModel(registryUrl.getScopeModel()));
        serviceInstance.setMetadata(instance.getMetadata());
        serviceInstance.setEnabled(instance.isEnabled());
        serviceInstance.setHealthy(instance.isHealthy());
        return serviceInstance;
    }

    /**
     * The group of {@link NamingService} to register
     *
     * @param connectionURL {@link URL connection url}
     * @return non-null, "default" as default
     * @since 2.7.5
     */
    public static String getGroup(URL connectionURL) {
        // Compatible with nacos grouping via group.
        String group = connectionURL.getParameter(GROUP_KEY, DEFAULT_GROUP);
        return connectionURL.getParameter(NACOS_GROUP_KEY, group);
    }

    // This ensures that different registry groups sharing the same Nacos server and namespace reuse a single
    // physical connection.
    private static String createNamingServiceCacheKey(URL connectionURL) {
        URL normalized = normalizeConnectionURL(connectionURL);
        return normalized.toFullString();
    }

    /**
     * Create or obtain a shared {@link NacosNamingServiceWrapper} for the given connection URL.
     *
     * @param connectionURL the registry connection URL
     * @return a shared {@link NacosNamingServiceWrapper}
     * @since 2.7.5
     */
    public static NacosNamingServiceWrapper createNamingService(URL connectionURL) {
        String key = createNamingServiceCacheKey(connectionURL);

        // Create or retrieve the shared service holder.
        NacosNamingServiceHolder holder = SERVICE_CACHE.compute(key, (k, v) -> {
            if (v == null) {
                logger.info("Creating shared NacosNamingService for key: {}", key);
                NacosNamingServiceWrapper newWrapper = createWrapperInternal(connectionURL);
                return new NacosNamingServiceHolder(newWrapper);
            }
            v.refCount.incrementAndGet();
            return v;
        });

        return holder.wrapper;
    }

    /**
     * Release a previously acquired {@link NacosNamingServiceWrapper} reference.
     *
     * <p>This method decrements the reference count associated with the normalized
     * Nacos connection key. When the reference count reaches zero, the underlying
     * {@link NacosNamingServiceWrapper} is shut down and removed from the cache.</p>
     *
     * <p>If the reference count becomes negative, it indicates a lifecycle bug
     * (i.e. {@code releaseNamingService} was called more times than
     * {@code createNamingService}).</p>
     *
     * @param connectionURL the registry connection URL used to identify the shared naming service
     */
    public static void releaseNamingService(URL connectionURL) {
        String key = createNamingServiceCacheKey(connectionURL);

        SERVICE_CACHE.compute(key, (k, v) -> {
            if (v == null) {
                return null;
            }

            int left = v.refCount.decrementAndGet();

            // If the count hits zero, this is the last user so we close the physical connection.
            if (left == 0) {
                try {
                    logger.info("Destroying shared NacosNamingService for key: {}", key);
                    v.wrapper.shutdown();
                } catch (Exception e) {
                    logger.warn(
                            REGISTRY_NACOS_EXCEPTION, "", "", "Failed to destroy naming service for key: " + key, e);
                }
                return null;
            }

            // Error case: more releases than creates (unbalanced lifecycle)
            if (left < 0) {
                logger.warn(
                        "releaseNamingService called more times than createNamingService for key: {} (refCount={})."
                                + " This indicates a bug in caller lifecycle management.",
                        key,
                        left);
                try {
                    v.wrapper.shutdown();
                } catch (Exception e) {
                    logger.warn(
                            REGISTRY_NACOS_EXCEPTION, "", "", "Failed to destroy naming service for key: " + key, e);
                }
                v.refCount.set(0);
                return null;
            }

            return v;
        });
    }

    /**
     * Create a new {@link NacosNamingServiceWrapper} using the normalized
     * connection URL and configured retry options.
     */
    private static NacosNamingServiceWrapper createWrapperInternal(URL connectionURL) {

        // Use of normalized URL for connection identity / cache key. This ensures
        // registry.group differences don't create separate physical connections.
        URL normalized = normalizeConnectionURL(connectionURL);

        // We do NOT embed them into the cache key because they represent per-creation behavior,
        // not part of the identity of the physical Nacos server/namespace.
        boolean check = connectionURL.getParameter(NACOS_CHECK_KEY, true);
        int retryTimes = connectionURL.getPositiveParameter(NACOS_RETRY_KEY, 10);
        int sleepMsBetweenRetries = connectionURL.getPositiveParameter(NACOS_RETRY_WAIT_KEY, 10);

        if (check && !UrlUtils.isCheck(connectionURL)) {
            check = false;
        }

        NacosConnectionManager nacosConnectionManager =
                new NacosConnectionManager(normalized, check, retryTimes, sleepMsBetweenRetries);
        return new NacosNamingServiceWrapper(nacosConnectionManager, retryTimes, sleepMsBetweenRetries);
    }

    /**
     * Normalize a Nacos registry connection URL for connection reuse.
     *
     * <p>This method produces a standard form of the connection URL that is
     * suitable for use as a cache key. It performs the following normalization steps:</p>
     *
     * <p>Removes group-related parameters, since grouping affects only service
     * registration semantics and not the underlying physical Nacos connection.</p>
     *
     * <p>Standardizes the server address list by trimming, sorting, and rejoining
     * addresses, ensuring that different address orders map to the same connection.</p>
     *
     * @param connectionURL the original registry connection URL
     * @return a normalized URL representing the unique physical Nacos connection
     */
    private static URL normalizeConnectionURL(URL connectionURL) {

        // Start from the original URL to preserve all parameters
        URL normalized = connectionURL.removeParameter(GROUP_KEY).removeParameter(NACOS_GROUP_KEY);

        // Standardize server addresses for stable cache keys
        String serverAddr = normalized.getParameter("serverAddr", normalized.getAddress());
        if (serverAddr != null) {
            String canonical = Arrays.stream(serverAddr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .sorted()
                    .collect(Collectors.joining(","));
            normalized = normalized.addParameter("serverAddr", canonical);
        }
        return normalized;
    }

    static void clearCacheForTest() {
        SERVICE_CACHE.clear();
    }

    static int getCacheSizeForTest() {
        return SERVICE_CACHE.size();
    }
}
