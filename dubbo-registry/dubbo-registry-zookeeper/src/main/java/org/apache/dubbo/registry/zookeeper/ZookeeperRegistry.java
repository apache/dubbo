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
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.URLStrParser;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.support.FailbackRegistry;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.zookeeper.ChildListener;
import org.apache.dubbo.remoting.zookeeper.StateListener;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter;
import org.apache.dubbo.rpc.RpcException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.CommonConstants.ANY_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_SEPARATOR_ENCODED;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CONFIGURATORS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.CONSUMERS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.DYNAMIC_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.EMPTY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDERS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.ROUTERS_CATEGORY;

/**
 * ZookeeperRegistry
 *
 */
public class ZookeeperRegistry extends FailbackRegistry {

    private final static String DEFAULT_ROOT = "dubbo";

    private final String root;

    private final Set<String> anyServices = new ConcurrentHashSet<>();

    private final ConcurrentMap<URL, ConcurrentMap<NotifyListener, ChildListener>> zkListeners = new ConcurrentHashMap<>();

    private final ZookeeperClient zkClient;

    public ZookeeperRegistry(URL url, ZookeeperTransporter zookeeperTransporter) {
        super(url);
        if (url.isAnyHost()) {
            throw new IllegalStateException("registry address == null");
        }
        String group = url.getParameter(GROUP_KEY, DEFAULT_ROOT);
        if (!group.startsWith(PATH_SEPARATOR)) {
            group = PATH_SEPARATOR + group;
        }
        this.root = group;
        zkClient = zookeeperTransporter.connect(url);
        zkClient.addStateListener((state) -> {
            if (state == StateListener.RECONNECTED) {
                logger.warn("Trying to fetch the latest urls, in case there're provider changes during connection loss.\n" +
                        " Since ephemeral ZNode will not get deleted for a connection lose, " +
                        "there's no need to re-register url of this instance.");
                ZookeeperRegistry.this.fetchLatestAddresses();
            } else if (state == StateListener.NEW_SESSION_CREATED) {
                logger.warn("Trying to re-register urls and re-subscribe listeners of this instance to registry...");
                try {
                    ZookeeperRegistry.this.recover();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            } else if (state == StateListener.SESSION_LOST) {
                logger.warn("Url of this instance will be deleted from registry soon. " +
                        "Dubbo client will try to re-register once a new session is created.");
            } else if (state == StateListener.SUSPENDED) {

            } else if (state == StateListener.CONNECTED) {

            }
        });
    }

    @Override
    public boolean isAvailable() {
        return zkClient.isConnected();
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            zkClient.close();
        } catch (Exception e) {
            logger.warn("Failed to close zookeeper client " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    public void doRegister(URL url) {
        try {
            zkClient.create(toUrlPath(url), url.getParameter(DYNAMIC_KEY, true));
        } catch (Throwable e) {
            throw new RpcException("Failed to register " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    public void doUnregister(URL url) {
        try {
            zkClient.delete(toUrlPath(url));
        } catch (Throwable e) {
            throw new RpcException("Failed to unregister " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    public void doSubscribe(final URL url, final NotifyListener listener) {
        try {
            if (ANY_VALUE.equals(url.getServiceInterface())) {
                String root = toRootPath();
                ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners.computeIfAbsent(url, k -> new ConcurrentHashMap<>());
                ChildListener zkListener = listeners.computeIfAbsent(listener, k -> (parentPath, currentChilds) -> {
                    for (String child : currentChilds) {
                        child = URL.decode(child);
                        if (!anyServices.contains(child)) {
                            anyServices.add(child);
                            subscribe(url.setPath(child).addParameters(INTERFACE_KEY, child,
                                    Constants.CHECK_KEY, String.valueOf(false)), k);
                        }
                    }
                });
                zkClient.create(root, false);
                List<String> services = zkClient.addChildListener(root, zkListener);
                if (CollectionUtils.isNotEmpty(services)) {
                    for (String service : services) {
                        service = URL.decode(service);
                        anyServices.add(service);
                        subscribe(url.setPath(service).addParameters(INTERFACE_KEY, service,
                                Constants.CHECK_KEY, String.valueOf(false)), listener);
                    }
                }
            } else {
                List<URL> urls = new ArrayList<>();
                for (String path : toCategoriesPath(url)) {
                    ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners.computeIfAbsent(url, k -> new ConcurrentHashMap<>());
                    ChildListener zkListener = listeners.computeIfAbsent(listener, k -> (parentPath, currentChilds) -> ZookeeperRegistry.this.notify(url, k, toUrlsWithEmpty(url, parentPath, currentChilds)));
                    zkClient.create(path, false);
                    List<String> children = zkClient.addChildListener(path, zkListener);
                    if (children != null) {
                        urls.addAll(toUrlsWithEmpty(url, path, children));
                    }
                }
                notify(url, listener, urls);
            }
        } catch (Throwable e) {
            throw new RpcException("Failed to subscribe " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners.get(url);
        if (listeners != null) {
            ChildListener zkListener = listeners.get(listener);
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
        }
    }

    @Override
    public List<URL> lookup(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("lookup url == null");
        }
        try {
            List<String> providers = new ArrayList<>();
            for (String path : toCategoriesPath(url)) {
                List<String> children = zkClient.getChildren(path);
                if (children != null) {
                    providers.addAll(children);
                }
            }
            return toUrlsWithoutEmpty(url, providers);
        } catch (Throwable e) {
            throw new RpcException("Failed to lookup " + url + " from zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
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
        if (ANY_VALUE.equals(url.getParameter(CATEGORY_KEY))) {
            categories = new String[]{PROVIDERS_CATEGORY, CONSUMERS_CATEGORY, ROUTERS_CATEGORY, CONFIGURATORS_CATEGORY};
        } else {
            categories = url.getParameter(CATEGORY_KEY, new String[]{DEFAULT_CATEGORY});
        }
        String[] paths = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            paths[i] = toServicePath(url) + PATH_SEPARATOR + categories[i];
        }
        return paths;
    }

    private String toCategoryPath(URL url) {
        return toServicePath(url) + PATH_SEPARATOR + url.getParameter(CATEGORY_KEY, DEFAULT_CATEGORY);
    }

    private String toUrlPath(URL url) {
        return toCategoryPath(url) + PATH_SEPARATOR + URL.encode(url.toFullString());
    }

    private List<URL> toUrlsWithoutEmpty(URL consumer, List<String> providers) {
        List<URL> urls = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(providers)) {
            for (String provider : providers) {
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

    private List<URL> toUrlsWithEmpty(URL consumer, String path, List<String> providers) {
        List<URL> urls = toUrlsWithoutEmpty(consumer, providers);
        if (urls == null || urls.isEmpty()) {
            int i = path.lastIndexOf(PATH_SEPARATOR);
            String category = i < 0 ? path : path.substring(i + 1);
            URL empty = URLBuilder.from(consumer)
                    .setProtocol(EMPTY_PROTOCOL)
                    .addParameter(CATEGORY_KEY, category)
                    .build();
            urls.add(empty);
        }
        return urls;
    }

    /**
     * When zookeeper connection recovered from a connection loss, it need to fetch the latest provider list.
     * re-register watcher is only a side effect and is not mandate.
     */
    private void fetchLatestAddresses() {
        // subscribe
        Map<URL, Set<NotifyListener>> recoverSubscribed = new HashMap<URL, Set<NotifyListener>>(getSubscribed());
        if (!recoverSubscribed.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Fetching the latest urls of " + recoverSubscribed.keySet());
            }
            for (Map.Entry<URL, Set<NotifyListener>> entry : recoverSubscribed.entrySet()) {
                URL url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    addFailedSubscribed(url, listener);
                }
            }
        }
    }

}
