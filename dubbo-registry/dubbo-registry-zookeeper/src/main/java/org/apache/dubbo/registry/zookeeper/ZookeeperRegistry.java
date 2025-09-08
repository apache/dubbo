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
package org.apache.dubbo.registry.zookeeper;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.support.CacheableFailbackRegistry;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.zookeeper.ChildListener;
import org.apache.dubbo.remoting.zookeeper.StateListener;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter;
import org.apache.dubbo.rpc.RpcException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

import static org.apache.dubbo.common.constants.CommonConstants.ANY_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.CHECK_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_ERROR_DESERIALIZE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ZOOKEEPER_EXCEPTION;
import static org.apache.dubbo.common.constants.RegistryConstants.CONFIGURATORS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.CONSUMERS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.DYNAMIC_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDERS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.ROUTERS_CATEGORY;

/**
 * ZookeeperRegistry
 */
public class ZookeeperRegistry extends CacheableFailbackRegistry {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ZookeeperRegistry.class);

    private static final String DEFAULT_ROOT = "dubbo";

    private final String root;

    private final Set<String> anyServices = new ConcurrentHashSet<>();

    private final ConcurrentMap<URL, ConcurrentMap<NotifyListener, ChildListener>> zkListeners =
            new ConcurrentHashMap<>();

    private ZookeeperClient zkClient;

    public ZookeeperRegistry(URL url, ZookeeperTransporter zookeeperTransporter) {
        super(url);

        if (url.isAnyHost()) {
            throw new IllegalStateException("registry address == null");
        }

        String group = url.getGroup(DEFAULT_ROOT);
        if (!group.startsWith(PATH_SEPARATOR)) {
            group = PATH_SEPARATOR + group;
        }

        this.root = group;
        this.zkClient = zookeeperTransporter.connect(url);

        this.zkClient.addStateListener((state) -> {
            if (state == StateListener.RECONNECTED) {
                logger.warn(
                        REGISTRY_ZOOKEEPER_EXCEPTION,
                        "",
                        "",
                        "Trying to fetch the latest urls, in case there are provider changes during connection loss.\n"
                                + " Since ephemeral ZNode will not get deleted for a connection lose, "
                                + "there's no need to re-register url of this instance.");
                ZookeeperRegistry.this.fetchLatestAddresses();
            } else if (state == StateListener.NEW_SESSION_CREATED) {
                logger.warn(
                        REGISTRY_ZOOKEEPER_EXCEPTION,
                        "",
                        "",
                        "Trying to re-register urls and re-subscribe listeners of this instance to registry...");

                try {
                    ZookeeperRegistry.this.recover();
                } catch (Exception e) {
                    logger.error(REGISTRY_ZOOKEEPER_EXCEPTION, "", "", e.getMessage(), e);
                }
            } else if (state == StateListener.SESSION_LOST) {
                logger.warn(
                        REGISTRY_ZOOKEEPER_EXCEPTION,
                        "",
                        "",
                        "Url of this instance will be deleted from registry soon. "
                                + "Dubbo client will try to re-register once a new session is created.");
            } else if (state == StateListener.SUSPENDED) {

            } else if (state == StateListener.CONNECTED) {

            }
        });
    }

    @Override
    public boolean isAvailable() {
        return zkClient != null && zkClient.isConnected();
    }

    @Override
    public void destroy() {
        super.destroy();

        // remove child listener
        Set<URL> urls = zkListeners.keySet();
        for (URL url : urls) {
            ConcurrentMap<NotifyListener, ChildListener> map = zkListeners.get(url);
            if (CollectionUtils.isEmptyMap(map)) {
                continue;
            }
            Collection<ChildListener> childListeners = map.values();
            if (CollectionUtils.isEmpty(childListeners)) {
                continue;
            }
            if (ANY_VALUE.equals(url.getServiceInterface())) {
                String root = toRootPath();
                childListeners.stream().forEach(childListener -> zkClient.removeChildListener(root, childListener));
            } else {
                for (String path : toCategoriesPath(url)) {
                    childListeners.stream().forEach(childListener -> zkClient.removeChildListener(path, childListener));
                }
            }
        }
        zkListeners.clear();

        // Just release zkClient reference, but can not close zk client here for zk client is shared somewhere else.
        // See org.apache.dubbo.remoting.zookeeper.AbstractZookeeperTransporter#destroy()
        zkClient = null;
    }

    private void checkDestroyed() {
        if (zkClient == null) {
            throw new IllegalStateException("registry is destroyed");
        }
    }

    @Override
    public void doRegister(URL url) {
        try {
            checkDestroyed();
            zkClient.create(toUrlPath(url), url.getParameter(DYNAMIC_KEY, true), true);
        } catch (Throwable e) {
            throw new RpcException(
                    "Failed to register " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    public void doUnregister(URL url) {
        try {
            checkDestroyed();
            zkClient.delete(toUrlPath(url));
        } catch (Throwable e) {
            throw new RpcException(
                    "Failed to unregister " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    public void doSubscribe(final URL url, final NotifyListener listener) {
        try {
            checkDestroyed();
            if (ANY_VALUE.equals(url.getServiceInterface())) {
                String root = toRootPath();
                boolean check = url.getParameter(CHECK_KEY, false);
                ConcurrentMap<NotifyListener, ChildListener> listeners =
                        ConcurrentHashMapUtils.computeIfAbsent(zkListeners, url, k -> new ConcurrentHashMap<>());

                ChildListener zkListener = ConcurrentHashMapUtils.computeIfAbsent(
                        listeners, listener, k -> (parentPath, currentChildren) -> {
                            for (String child : currentChildren) {
                                try {
                                    child = URL.decode(child);
                                    if (!(JsonUtils.checkJson(child))) {
                                        throw new Exception("dubbo-admin subscribe " + child + " failed,beacause "
                                                + child + "is root path in " + url);
                                    }
                                } catch (Exception e) {
                                    logger.warn(PROTOCOL_ERROR_DESERIALIZE, "", "", e.getMessage());
                                }
                                if (!anyServices.contains(child)) {
                                    anyServices.add(child);
                                    subscribe(
                                            url.setPath(child)
                                                    .addParameters(
                                                            INTERFACE_KEY,
                                                            child,
                                                            Constants.CHECK_KEY,
                                                            String.valueOf(check)),
                                            k);
                                }
                            }
                        });

                zkClient.create(root, false, true);

                List<String> services = zkClient.addChildListener(root, zkListener);
                if (CollectionUtils.isNotEmpty(services)) {
                    for (String service : services) {
                        service = URL.decode(service);
                        anyServices.add(service);
                        subscribe(
                                url.setPath(service)
                                        .addParameters(
                                                INTERFACE_KEY, service, Constants.CHECK_KEY, String.valueOf(check)),
                                listener);
                    }
                }
            } else {
                CountDownLatch latch = new CountDownLatch(1);

                try {
                    List<URL> urls = new ArrayList<>();

                    /*
                        Iterate over the category value in URL.
                        With default settings, the path variable can be when url is a consumer URL:

                            /dubbo/[service name]/providers,
                            /dubbo/[service name]/configurators
                            /dubbo/[service name]/routers
                    */
                    for (String path : toCategoriesPath(url)) {
                        ConcurrentMap<NotifyListener, ChildListener> listeners = ConcurrentHashMapUtils.computeIfAbsent(
                                zkListeners, url, k -> new ConcurrentHashMap<>());
                        ChildListener zkListener = ConcurrentHashMapUtils.computeIfAbsent(
                                listeners, listener, k -> new RegistryChildListenerImpl(url, k, latch));

                        if (zkListener instanceof RegistryChildListenerImpl) {
                            ((RegistryChildListenerImpl) zkListener).setLatch(latch);
                        }

                        // create "directories".
                        zkClient.create(path, false, true);

                        // Add children (i.e. service items).
                        List<String> children = zkClient.addChildListener(path, zkListener);
                        if (children != null) {
                            // The invocation point that may cause 1-1.
                            urls.addAll(toUrlsWithEmpty(url, path, children));
                        }
                    }

                    notify(url, listener, urls);
                } finally {
                    // tells the listener to run only after the sync notification of main thread finishes.
                    latch.countDown();
                }
            }
        } catch (Throwable e) {
            throw new RpcException(
                    "Failed to subscribe " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        super.doUnsubscribe(url, listener);
        checkDestroyed();
        ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners.get(url);
        if (listeners != null) {
            ChildListener zkListener = listeners.remove(listener);
            if (zkListener != null) {
                if (ANY_VALUE.equals(url.getServiceInterface())) {
                    String root = toRootPath();
                    zkClient.removeChildListener(root, zkListener);
                } else {
                    for (String path : toCategoriesPath(url)) {
                        zkClient.removeChildListener(path, zkListener);
                    }
                }
            }

            if (listeners.isEmpty()) {
                zkListeners.remove(url);
            }
        }
    }

    @Override
    public List<URL> lookup(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("lookup url == null");
        }
        try {
            checkDestroyed();
            List<String> providers = new ArrayList<>();
            for (String path : toCategoriesPath(url)) {
                List<String> children = zkClient.getChildren(path);
                if (children != null) {
                    providers.addAll(children);
                }
            }
            return toUrlsWithoutEmpty(url, providers);
        } catch (Throwable e) {
            throw new RpcException(
                    "Failed to lookup " + url + " from zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    private String toRootDir() {
        if (root.equals(PATH_SEPARATOR)) {
            return root;
        }
        return root + PATH_SEPARATOR;
    }

    private String toRootPath() {
        return root;
    }

    private String toServicePath(URL url) {
        String name = url.getServiceInterface();
        if (ANY_VALUE.equals(name)) {
            return toRootPath();
        }
        return toRootDir() + URL.encode(name);
    }

    private String[] toCategoriesPath(URL url) {
        String[] categories;
        if (ANY_VALUE.equals(url.getCategory())) {
            categories =
                    new String[] {PROVIDERS_CATEGORY, CONSUMERS_CATEGORY, ROUTERS_CATEGORY, CONFIGURATORS_CATEGORY};
        } else {
            categories = url.getCategory(new String[] {DEFAULT_CATEGORY});
        }
        String[] paths = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            paths[i] = toServicePath(url) + PATH_SEPARATOR + categories[i];
        }
        return paths;
    }

    private String toCategoryPath(URL url) {
        return toServicePath(url) + PATH_SEPARATOR + url.getCategory(DEFAULT_CATEGORY);
    }

    private String toUrlPath(URL url) {
        return toCategoryPath(url) + PATH_SEPARATOR + URL.encode(url.toFullString());
    }

    /**
     * When zookeeper connection recovered from a connection loss, it needs to fetch the latest provider list.
     * re-register watcher is only a side effect and is not mandate.
     */
    private void fetchLatestAddresses() {
        // subscribe
        Map<URL, Set<NotifyListener>> recoverSubscribed = new HashMap<>(getSubscribed());
        if (!recoverSubscribed.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Fetching the latest urls of " + recoverSubscribed.keySet());
            }
            for (Map.Entry<URL, Set<NotifyListener>> entry : recoverSubscribed.entrySet()) {
                URL url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    removeFailedSubscribed(url, listener);
                    addFailedSubscribed(url, listener);
                }
            }
        }
    }

    @Override
    protected boolean isMatch(URL subscribeUrl, URL providerUrl) {
        return UrlUtils.isMatch(subscribeUrl, providerUrl);
    }

    /**
     * Triggered when children get changed. It will be invoked by implementation of CuratorWatcher.
     * <p>
     * 'org.apache.dubbo.remoting.zookeeper.curator5.Curator5ZookeeperClient.CuratorWatcherImpl' (Curator 5)
     */
    private class RegistryChildListenerImpl implements ChildListener {
        private final ZookeeperRegistryNotifier notifier;
        private volatile CountDownLatch latch;

        public RegistryChildListenerImpl(URL consumerUrl, NotifyListener listener, CountDownLatch latch) {
            this.latch = latch;
            this.notifier = new ZookeeperRegistryNotifier(consumerUrl, listener, ZookeeperRegistry.this.getDelay());
        }

        public void setLatch(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void childChanged(String path, List<String> children) {
            // Notify 'notifiers' one by one.
            try {
                latch.await();
            } catch (InterruptedException e) {
                logger.warn(
                        REGISTRY_ZOOKEEPER_EXCEPTION,
                        "",
                        "",
                        "Zookeeper children listener thread was interrupted unexpectedly, may cause race condition with the main thread.");
            }

            notifier.notify(path, children);
        }
    }

    /**
     * Customized Registry Notifier for zookeeper registry.
     */
    public class ZookeeperRegistryNotifier {
        private long lastExecuteTime;
        private final URL consumerUrl;
        private final NotifyListener listener;
        private final long delayTime;

        public ZookeeperRegistryNotifier(URL consumerUrl, NotifyListener listener, long delayTime) {
            this.consumerUrl = consumerUrl;
            this.listener = listener;
            this.delayTime = delayTime;
        }

        public void notify(String path, Object rawAddresses) {
            // notify immediately if it's notification of governance rules.
            if (path.endsWith(CONFIGURATORS_CATEGORY) || path.endsWith(ROUTERS_CATEGORY)) {
                this.doNotify(path, rawAddresses);
            }

            // if address notification, check if delay is necessary.
            if (delayTime <= 0) {
                this.doNotify(path, rawAddresses);
            } else {
                long interval = delayTime - (System.currentTimeMillis() - lastExecuteTime);
                if (interval > 0) {
                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
                lastExecuteTime = System.currentTimeMillis();
                this.doNotify(path, rawAddresses);
            }
        }

        protected void doNotify(String path, Object rawAddresses) {
            ZookeeperRegistry.this.notify(
                    consumerUrl, listener, ZookeeperRegistry.this.toUrlsWithEmpty(consumerUrl, path, (List<String>)
                            rawAddresses));
        }
    }
}
