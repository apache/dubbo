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
package org.apache.dubbo.registry.domain;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.support.FailbackRegistry;
import org.apache.dubbo.rpc.RpcException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_NOTIFY_EVENT;
import static org.apache.dubbo.common.constants.RegistryConstants.FALLBACK_MODE_ALL;
import static org.apache.dubbo.common.constants.RegistryConstants.FALLBACK_MODE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.FALLBACK_MODE_NONE;
import static org.apache.dubbo.common.constants.RegistryConstants.FALLBACK_MODE_SPECIFIED;
import static org.apache.dubbo.common.constants.RegistryConstants.FALLBACK_SERVICES_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTER_CONSUMER_URL_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_KEY;

/**
 * Domain-based Registry implementation that supports both direct domain names and Kubernetes service DNS.
 * Provides DNS-based service discovery with fallback to traditional registry mechanisms.
 */
@SPI(value = "domain", scope = ExtensionScope.APPLICATION)
public class DomainRegistry extends FailbackRegistry {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(DomainRegistry.class);
    private static final int DEFAULT_DNS_REFRESH_PERIOD = 30;
    private static final String DNS_REFRESH_PERIOD_KEY = "dns.refresh.period";
    private static final String ENABLE_FALLBACK_KEY = "domain.registry.fallback";
    private static final boolean DEFAULT_ENABLE_FALLBACK = true;

    // Track registered services and their domain mappings
    private final Set<URL> registeredServices = new ConcurrentHashSet<>();
    // Map of URL to its domain resolution task
    private final ConcurrentMap<URL, ScheduledFuture<?>> domainResolutionTasks = new ConcurrentHashMap<>();
    // Map of URL to its current resolved addresses
    private final ConcurrentMap<URL, List<URL>> resolvedAddresses = new ConcurrentHashMap<>();
    private final AtomicBoolean destroyed = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler;

    /**
     * Determines if fallback should be used for the given URL based on configuration
     */
    private boolean shouldFallback(URL url) {
        if (!getUrl().getParameter(ENABLE_FALLBACK_KEY, DEFAULT_ENABLE_FALLBACK)
                || REGISTRY_KEY.equals(url.getProtocol())) {
            return false;
        }

        String fallbackMode = getUrl().getParameter(FALLBACK_MODE_KEY, DEFAULT_FALLBACK_MODE);
        if (FALLBACK_MODE_NONE.equals(fallbackMode)) {
            return false;
        }
        if (FALLBACK_MODE_ALL.equals(fallbackMode)) {
            return true;
        }
        if (FALLBACK_MODE_SPECIFIED.equals(fallbackMode)) {
            String services = getUrl().getParameter(FALLBACK_SERVICES_KEY, "");
            String serviceInterface = url.getParameter(INTERFACE_KEY);
            if (serviceInterface == null) {
                return false;
            }
            String[] allowedServices = services.split(",");
            for (String service : allowedServices) {
                if (serviceInterface.equals(service)) {
                    return true;
                }
            }
            return false;
        }
        return true; // Default to allowing fallback for unknown modes
    }

    public DomainRegistry(URL url) {
        super(url);
        this.scheduler = url.getOrDefaultFrameworkModel()
                .getBeanFactory()
                .getBean(FrameworkExecutorRepository.class)
                .getSharedScheduledExecutor();
    }

    @Override
    public boolean isAvailable() {
        return !destroyed.get();
    }

    @Override
    public void doRegister(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("register url == null");
        }
        try {
            // Only register if it's a provider or consumer registration is explicitly enabled
            if (PROVIDER_SIDE.equals(url.getSide()) || getUrl().getParameter(REGISTER_CONSUMER_URL_KEY, false)) {
                // Store the registration for fallback support
                registeredServices.add(url);
                // If the URL uses a domain name, attempt to resolve it
                String host = url.getHost();
                if (!NetUtils.isIP(host)) {
                    List<URL> resolvedUrls = resolveDomain(url);
                    if (!resolvedUrls.isEmpty()) {
                        // Successfully resolved domain, register all IP addresses
                        for (URL resolvedUrl : resolvedUrls) {
                            registeredServices.add(resolvedUrl);
                        }
                    } else {
                        // Check if fallback should be used for this URL
                        if (shouldFallback(url)) {
                            logger.info("DNS resolution failed, falling back to traditional registry for: " + url);
                            super.doRegister(url);
                        } else {
                            logger.warn("Failed to resolve domain for registration: " + url
                                    + ". Registration will proceed with domain name.");
                        }
                    }
                }
            } else {
                logger.info("Skip registration since it's neither provider nor consumer registration enabled: " + url);
            }
        } catch (Throwable e) {
            throw new RpcException(
                    "Failed to register " + url + " to domain registry " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    public void doUnregister(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("unregister url == null");
        }
        try {
            // Remove both the original URL and any resolved IP-based URLs
            registeredServices.remove(url);
            // If it's a domain-based URL, also remove any resolved IP addresses
            String host = url.getHost();
            if (!NetUtils.isIP(host)) {
                List<URL> resolvedUrls = resolveDomain(url);
                for (URL resolvedUrl : resolvedUrls) {
                    registeredServices.remove(resolvedUrl);
                }
            }
        } catch (Throwable e) {
            throw new RpcException(
                    "Failed to unregister " + url + " from domain registry " + getUrl() + ", cause: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public void doSubscribe(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("subscribe url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("subscribe listener == null");
        }

        String host = url.getHost();
        if (NetUtils.isIP(host)) {
            // If the URL already contains an IP address, notify directly
            List<URL> urls = new ArrayList<>();
            urls.add(url);
            notify(url, listener, urls);
            return;
        }

        // Schedule periodic DNS resolution
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                () -> {
                    try {
                        List<URL> resolvedUrls = resolveDomain(url);
                        if (!resolvedUrls.isEmpty()) {
                            resolvedAddresses.put(url, resolvedUrls);
                            notify(url, listener, resolvedUrls);
                        } else {
                            // If DNS resolution fails, try fallback
                            List<URL> cachedUrls = resolvedAddresses.get(url);
                            if (cachedUrls != null && !cachedUrls.isEmpty()) {
                                notify(url, listener, cachedUrls);
                            } else {
                                // Check if fallback should be used for this URL
                                if (shouldFallback(url)) {
                                    logger.info(
                                            "DNS resolution failed, falling back to traditional registry for: " + url);
                                    super.doSubscribe(url, listener);
                                } else {
                                    notify(url, listener, new ArrayList<>());
                                }
                            }
                        }
                    } catch (Throwable e) {
                        logger.error(
                                REGISTRY_FAILED_NOTIFY_EVENT,
                                "",
                                "",
                                "Failed to resolve domain for " + url + ", cause: " + e.getMessage(),
                                e);
                    }
                },
                0,
                url.getParameter(DNS_REFRESH_PERIOD_KEY, DEFAULT_DNS_REFRESH_PERIOD),
                TimeUnit.SECONDS);

        domainResolutionTasks.put(url, future);
    }

    private List<URL> resolveDomain(URL url) {
        List<URL> urls = new ArrayList<>();
        try {
            String host = url.getHost();
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                urls.add(url.setHost(address.getHostAddress()));
            }
        } catch (UnknownHostException e) {
            logger.warn(
                    REGISTRY_FAILED_NOTIFY_EVENT,
                    "",
                    "",
                    "Failed to resolve domain " + url.getHost() + ", cause: " + e.getMessage());
        }
        return urls;
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("unsubscribe url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("unsubscribe listener == null");
        }

        // Cancel the DNS resolution task
        ScheduledFuture<?> future = domainResolutionTasks.remove(url);
        if (future != null) {
            future.cancel(true);
        }

        // Clean up resolved addresses
        resolvedAddresses.remove(url);
    }

    @Override
    public void destroy() {
        if (!destroyed.compareAndSet(false, true)) {
            return;
        }

        // Cancel all DNS resolution tasks
        for (ScheduledFuture<?> future : domainResolutionTasks.values()) {
            future.cancel(true);
        }
        domainResolutionTasks.clear();
        resolvedAddresses.clear();

        super.destroy();
    }
}
