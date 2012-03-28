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
package com.alibaba.dubbo.registry.zookeeper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.support.FailbackRegistry;
import com.alibaba.dubbo.rpc.RpcException;

/**
 * ZookeeperRegistry
 * 
 * @author william.liangf
 */
public class ZookeeperRegistry extends FailbackRegistry {

    private final static Logger logger = LoggerFactory.getLogger(ZookeeperRegistry.class);

    private final static int DEFAULT_ZOOKEEPER_PORT = 2181;
    
    private final static String DEFAULT_ROOT = "dubbo";

    private final String        root;
    
    private final boolean       auth;
    
    private final List<ACL>     acl;

    private final ReentrantLock zookeeperLock = new ReentrantLock();

    private final Set<String> failedWatched = new ConcurrentHashSet<String>();

    private final Set<String> anyServices = new ConcurrentHashSet<String>();
    
    private final ConcurrentMap<String, Set<NotifyListener>> anyNotifyListeners = new ConcurrentHashMap<String, Set<NotifyListener>>();
    
    private volatile ZooKeeper  zookeeper;

    public ZookeeperRegistry(URL url) {
        super(url);
        this.auth = url.getUsername() != null && url.getUsername().length() > 0 
                && url.getPassword() != null && url.getPassword().length() > 0;
        this.acl = auth ? Ids.CREATOR_ALL_ACL : Ids.OPEN_ACL_UNSAFE;
        String group = url.getParameter(Constants.GROUP_KEY, DEFAULT_ROOT);
        if (! group.startsWith(Constants.PATH_SEPARATOR)) {
            group = Constants.PATH_SEPARATOR + group;
        }
        this.root = group;
        initZookeeper();
    }

    @Override
    protected void retry() {
        super.retry();
        initZookeeper();
        if (failedWatched.size() > 0) {
            Set<String> failed = new HashSet<String>(failedWatched);
            if (failed.size() > 0) {
                if (logger.isInfoEnabled()) {
                    logger.info("Retry watch " + failed + " to zookeeper " + getUrl());
                }
                for (String service : failed) {
                    try {
                        getChildren(service);
                        failedWatched.remove(service);
                    } catch (Throwable t) {
                        logger.warn("Failed to retry register " + failed + " to zookeeper " + getUrl() + ", waiting for again, cause: " + t.getMessage(), t);
                    }
                }
            }
        }
    }
    
    private List<String> watch(String service) {
        try {
            ZooKeeper zk = ZookeeperRegistry.this.zookeeper;
            if (zk != null) {
                List<String> result = getChildren(service);
                failedWatched.remove(service);
                return result;
            }
        } catch (Throwable e) {
            logger.warn("Failed to watch path " + service + " to zookeeper" + getUrl() + ", cause: " + e.getMessage(), e);
        }
        failedWatched.add(service);
        return new ArrayList<String>(0);
    }
    
    private void initZookeeper() {
        ZooKeeper zk = this.zookeeper;
        if (zk == null || zk.getState() == null || ! zk.getState().isAlive()) {
            zookeeperLock.lock();
            try {
                zk = this.zookeeper;
                if (zk == null || zk.getState() == null || ! zk.getState().isAlive()) {
                    this.zookeeper = createZookeeper();
                    recover();
                }
                if (zk != null) {
                    zk.close();
                }
            } catch (Exception e) {
                throw new IllegalStateException("Can not connect to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
            } finally {
                zookeeperLock.unlock();
            }
        }
    }
    
    static String appendDefaultPort(String address) {
        if (address != null && address.length() > 0) {
            int i = address.indexOf(':');
            if (i < 0) {
                return address + ":" + DEFAULT_ZOOKEEPER_PORT;
            } else if (Integer.parseInt(address.substring(i + 1)) == 0) {
                return address.substring(0, i + 1) + DEFAULT_ZOOKEEPER_PORT;
            }
        }
        return address;
    }
    
    private ZooKeeper createZookeeper() throws Exception {
        URL url = getUrl();
        StringBuilder address = new StringBuilder(appendDefaultPort(url.getAddress()));
        String[] backups = url.getParameter(Constants.BACKUP_KEY, new String[0]);
        if (backups != null && backups.length > 0) {
            for (String backup : backups) {
                address.append(",");
                address.append(appendDefaultPort(backup));
            }
        }
        ZooKeeper zk = new ZooKeeper(address.toString(), url.getPositiveParameter(
                Constants.SESSION_TIMEOUT_KEY, Constants.DEFAULT_SESSION_TIMEOUT), new Watcher() {
            public void process(WatchedEvent event) {
                try {
                    if (event.getState() == KeeperState.Expired) {
                        initZookeeper();
                    } else if (event.getState() == KeeperState.SyncConnected
                            && event.getType() == EventType.None) {
                        recover();
                    }
                    if (event.getType() != EventType.NodeChildrenChanged) {
                        return;
                    }
                    String path = event.getPath();
                    if (path == null || path.length() == 0) {
                        return;
                    }
                    List<String> children = watch(path);
                    if (path.equals(toRootPath())) {
                        List<String> services = children;
                        if (services != null && services.size() > 0) {
                            for (String service : services) {
                                if (anyServices.contains(service)) {
                                    continue;
                                }
                                anyServices.add(service);
                                for (Map.Entry<String, Set<NotifyListener>> entry : anyNotifyListeners.entrySet()) {
                                    URL subscribeUrl = URL.valueOf(entry.getKey()).setPath(service).addParameters(
                                            Constants.INTERFACE_KEY, service, 
                                            Constants.CHECK_KEY, String.valueOf(false), 
                                            Constants.REGISTER_KEY, String.valueOf(false));
                                    for (NotifyListener listener : entry.getValue()) {
                                        subscribe(subscribeUrl, listener);
                                    }
                                }
                            }
                        }
                    } else {
                        String dir = toRootDir();
                        String action = Constants.PROVIDERS;
                        String service = path;
                        if (service.startsWith(dir)) {
                            service = service.substring(dir.length());
                        }
                        int i = service.indexOf(Constants.PATH_SEPARATOR);
                        if (i >= 0) {
                            action = service.substring(i + 1);
                            service = service.substring(0, i);
                        }
                        service = URL.decode(service);
                        List<String> adminChildren = null;
                        for (Map.Entry<URL, Set<NotifyListener>> entry : getSubscribed().entrySet()) {
                            URL key = entry.getKey();
                            List<String> notifies = children;
                            if (key.getParameter(Constants.ADMIN_KEY, false)) {
                                if (adminChildren == null) {
                                    adminChildren = getChildren(path.substring(0, path.lastIndexOf(Constants.PATH_SEPARATOR) + 1) + (Constants.CONSUMERS.equals(action) ? Constants.PROVIDERS : Constants.CONSUMERS));
                                    adminChildren.addAll(children);
                                }
                                notifies = adminChildren;
                            } else if (Constants.CONSUMERS.equals(action)) {
                                continue;
                            }
                            String subscribeService = key.getServiceInterface();
                            if (service.equals(subscribeService)) {
                                List<URL> list = toUrls(key, notifies);
                                if (logger.isInfoEnabled()) {
                                    logger.info("Zookeeper service changed, service: " + service + ", urls: " + list + ", zookeeper: " + getUrl());
                                }
                                for (NotifyListener listener : entry.getValue()) {
                                    ZookeeperRegistry.this.notify(key, listener, list);
                                }
                            }
                        }
                    }
                } catch (Throwable e) {
                    logger.error("Failed to received event path " + event.getPath() + " from zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
                }
            }
        });
        if (auth) {
            zk.addAuthInfo(url.getUsername(), url.getPassword().getBytes());
        }
        return zk;
    }

    public boolean isAvailable() {
        return zookeeper.getState().isAlive();
    }

    public void destroy() {
        super.destroy();
        try {
            zookeeper.close();
        } catch (Exception e) {
            logger.warn("Failed to close zookeeper client " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }
    
    private boolean exists(String node) {
        try {
            return zookeeper.exists(node, false) != null;
        } catch (Throwable e) {
            return false;
        }
    }
    
    protected void doRegister(URL url) {
        try {
            String root = toRootPath();
            if (root != null && root.length() > 0 && ! Constants.PATH_SEPARATOR.equals(root)
                    && ! exists(root)) {
                try {
                    zookeeper.create(root, new byte[0], acl, CreateMode.PERSISTENT);
                } catch (NodeExistsException e) {
                }
            }
            String service = toServicePath(url);
            if (! exists(service)) {
                try {
                    zookeeper.create(service, new byte[0], acl, CreateMode.PERSISTENT);
                } catch (NodeExistsException e) {
                }
            }
            String category = toCategoryPath(url);
            if (! exists(category)) {
                try {
                    zookeeper.create(category, new byte[0], acl, CreateMode.PERSISTENT);
                } catch (NodeExistsException e) {
                }
            }
            String provider = toProviderPath(url);
            if (exists(provider)) {
                try {
                    zookeeper.delete(provider, -1);
                } catch (NoNodeException e) {
                }
            }
            CreateMode createMode = Constants.ROUTE_PROTOCOL.equals(url.getProtocol()) ? CreateMode.PERSISTENT : CreateMode.EPHEMERAL;
            try {
                zookeeper.create(provider, new byte[0], acl, createMode);
            } catch (NodeExistsException e) {
                try {
                    zookeeper.delete(provider, -1);
                } catch (NoNodeException e2) {
                }
                zookeeper.create(provider, new byte[0], acl, createMode);
            }
        } catch (Throwable e) {
            throw new RpcException("Failed to register " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    protected void doUnregister(URL url) {
        try {
            String provider = toProviderPath(url);
            zookeeper.delete(provider, -1);
        } catch (Throwable e) {
            throw new RpcException("Failed to unregister " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    protected void doSubscribe(URL url, NotifyListener listener) {
        try {
            if (Constants.ANY_VALUE.equals(url.getServiceInterface())) {
                String key = url.toFullString();
                Set<NotifyListener> listeners = anyNotifyListeners.get(key);
                if (listeners == null) {
                    anyNotifyListeners.putIfAbsent(key, new ConcurrentHashSet<NotifyListener>());
                    listeners = anyNotifyListeners.get(key);
                }
                listeners.add(listener);
                String root = toRootPath();
                List<String> services = getChildren(root);
                if (services != null && services.size() > 0) {
                    anyServices.addAll(services);
                    for (String service : services) {
                        subscribe(url.setPath(service).addParameters(Constants.INTERFACE_KEY, service, 
                                Constants.CHECK_KEY, String.valueOf(false), Constants.REGISTER_KEY, String.valueOf(false)), listener);
                    }
                }
            } else {
                if (url.getParameter(Constants.REGISTER_KEY, true)) {
                    register(url);
                }
                String register = toRegisterPath(url);
                List<String> providers = getChildren(register);
                if (url.getParameter(Constants.ADMIN_KEY, false)) {
                    String subscribe = toSubscribePath(url);
                    List<String> consumers = getChildren(subscribe);
                    providers.addAll(consumers);
                }
                List<URL> urls = toUrls(url, providers);
                notify(url, listener, urls);
            }
        } catch (Throwable e) {
            throw new RpcException("Failed to subscribe " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }
    
    private List<String> getChildren(String service) throws KeeperException, InterruptedException {
        try {
            List<String> list = zookeeper.getChildren(service, true);
            if (list == null || list.size() == 0) {
                return new ArrayList<String>(0);
            }
            List<String> result = new ArrayList<String>();
            for (String value : list) {
                result.add(URL.decode(value));
            }
            return result;
        } catch (KeeperException e) {
            if (e instanceof KeeperException.NoNodeException) {
                return new ArrayList<String>(0);
            }
            throw e;
        }
    }
    
    protected void doUnsubscribe(URL url, NotifyListener listener) {
        if (Constants.ANY_VALUE.equals(url.getServiceInterface())) {
            String key = url.toFullString();
            Set<NotifyListener> listeners = anyNotifyListeners.get(key);
            if (listeners != null) {
                listeners.remove(listener);
            }
        } else if (url.getParameter(Constants.REGISTER_KEY, true)) {
            unregister(url);
        }
    }
    
    public List<URL> lookup(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("lookup url == null");
        }
        try {
            String register = toRegisterPath(url);
            List<String> providers = getChildren(register);
            if (url.getParameter(Constants.ADMIN_KEY, false)) {
                String subscribe = toSubscribePath(url);
                List<String> consumers = getChildren(subscribe);
                providers.addAll(consumers);
            }
            return toUrls(url, providers);
        } catch (Throwable e) {
            throw new RpcException("Failed to lookup " + url + " from zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }
    
    private String toRootDir() {
        if (root.equals(Constants.PATH_SEPARATOR)) {
            return root;
        }
        return root + Constants.PATH_SEPARATOR;
    }
    
    private String toRootPath() {
        return root;
    }
    
    private String toServicePath(URL url) {
        String name = url.getServiceInterface();
        if (Constants.ANY_VALUE.equals(name)) {
            return toRootPath();
        }
        return toRootDir() + URL.encode(name);
    }
    
    private String toCategoryPath(URL url) {
        if (Constants.SUBSCRIBE_PROTOCOL.equals(url.getProtocol())) {
            return toSubscribePath(url);
        } else {
            return toRegisterPath(url);
        }
    }

    private String toRegisterPath(URL url) {
        return toServicePath(url) + Constants.PATH_SEPARATOR + Constants.PROVIDERS;
    }

    private String toSubscribePath(URL url) {
        return toServicePath(url) + Constants.PATH_SEPARATOR + Constants.CONSUMERS;
    }

    private String toProviderPath(URL url) {
        return toCategoryPath(url) + Constants.PATH_SEPARATOR + URL.encode(url.toFullString());
    }
    
    private List<URL> toUrls(URL consumer, List<String> providers) throws KeeperException, InterruptedException {
        List<URL> urls = new ArrayList<URL>();
        if (providers != null && providers.size() > 0) {
            for (String provider : providers) {
                provider = URL.decode(provider);
                if (provider.contains("://")) {
                    URL url = URL.valueOf(provider);
                    if (UrlUtils.isMatch(consumer, url)) {
                        urls.add(url);
                    }
                }
            }
        }
        if (urls != null && urls.isEmpty() && consumer.getParameter(Constants.ADMIN_KEY, false)) {
            urls.add(consumer.setProtocol(Constants.EMPTY_PROTOCOL));
        }
        return urls;
    }

}