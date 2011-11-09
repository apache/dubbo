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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
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
    
    private final static String SEPARATOR = "/";

    private final String        root;
    
    private final boolean       auth;

    private final ReentrantLock zookeeperLock = new ReentrantLock();

    private volatile ZooKeeper  zookeeper;

    public ZookeeperRegistry(URL url) {
        super(url);
        this.auth = url.getUsername() != null && url.getUsername().length() > 0 
                && url.getPassword() != null && url.getPassword().length() > 0;
        String group = url.getParameter(Constants.GROUP_KEY);
        if (group != null && group.length() > 0) {
            group = SEPARATOR + group;
            this.root = group;
        } else {
            this.root = "";
        }
        initZookeeper();
    }

    @Override
    protected void doRetry() {
        initZookeeper();
    }

    private void initZookeeper() {
        ZooKeeper zk = this.zookeeper;
        if (zk == null || zk.getState() == null || ! zk.getState().isAlive()) {
            zookeeperLock.lock();
            try {
                zk = this.zookeeper;
                if (zk == null || zk.getState() == null || ! zk.getState().isAlive()) {
                    this.zookeeper = createZookeeper();
                }
                if (zk != null) {
                    zk.close();
                }
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            } finally {
                zookeeperLock.unlock();
            }
        }
    }
    
    private ZooKeeper createZookeeper() throws Exception {
        URL url = getUrl();
        String address = url.getAddress();
        String backup = url.getParameter(Constants.BACKUP_KEY);
        if (backup != null && backup.length() > 0) {
            address = address + "," + backup;
        }
        ZooKeeper zk = new ZooKeeper(address, url.getPositiveParameter(
                Constants.TIMEOUT_KEY, Integer.MAX_VALUE), new Watcher() {
            public void process(WatchedEvent event) {
                try {
                    if (event.getState() == KeeperState.Expired) {
                        initZookeeper();
                    }
                    if (event.getType() != EventType.NodeChildrenChanged) {
                        return;
                    }
                    String path = event.getPath();
                    if (path == null || path.length() == 0) {
                        return;
                    }
                    ZooKeeper zk = ZookeeperRegistry.this.zookeeper;
                    List<String> providers = zk.getChildren(path, true);
                    String service = path;
                    int i = service.lastIndexOf(SEPARATOR);
                    if (i >= 0) {
                        service = service.substring(i + 1);
                    }
                    service = URL.decode(service);
                    for (Map.Entry<String, Set<NotifyListener>> entry : getSubscribed().entrySet()) {
                        String key = entry.getKey();
                        URL subscribe = URL.valueOf(key);
                        String subscribeService = subscribe.getServiceName();
                        if (service.equals(subscribeService)) {
                            List<URL> list = toUrls(subscribe, providers);
                            if (logger.isInfoEnabled()) {
                                logger.info("Zookeeper service changed, service: " + service + ", urls: " + list);
                            }
                            for (NotifyListener listener : entry.getValue()) {
                                ZookeeperRegistry.this.notify(subscribe, listener, list);
                            }
                        }
                    }
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
        if (auth) {
            zk.addAuthInfo(url.getUsername(), url.getPassword().getBytes());
        }
        if (root != null && root.length() > 0 && zk.exists(root, false) == null) {
            zk.create(root, new byte[0], auth ? Ids.CREATOR_ALL_ACL : Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
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
            logger.warn(e.getMessage(), e);
        }
    }
    
    public List<URL> lookup(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("lookup url == null");
        }
        try {
            String service = toServicePath(url);
            List<String> providers = zookeeper.getChildren(service, true);
            return toUrls(url, providers);
        } catch (Throwable e) {
            throw new RpcException("Failed to lookup " + url + ", cause: " + e.getMessage(), e);
        }
    }
    
    protected void doRegister(URL url) {
        try {
            String service = toServicePath(url);
            String provider = service + toProviderPath(url);
            if (zookeeper.exists(service, false) == null) {
                zookeeper.create(service, new byte[0], auth ? Ids.CREATOR_ALL_ACL : Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            if (zookeeper.exists(provider, false) == null) {
                zookeeper.create(provider, new byte[0], auth ? Ids.CREATOR_ALL_ACL : Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            }
        } catch (Throwable e) {
            throw new RpcException("Failed to register " + url + ", cause: " + e.getMessage(), e);
        }
    }

    protected void doUnregister(URL url) {
        try {
            String service = toServicePath(url);
            String provider = service + toProviderPath(url);
            zookeeper.delete(provider, -1);
        } catch (Throwable e) {
            throw new RpcException("Failed to unregister " + url + ", cause: " + e.getMessage(), e);
        }
    }

    protected void doSubscribe(URL url, NotifyListener listener) {
        try {
            String service = toServicePath(url);
            List<String> providers = zookeeper.getChildren(service, true);
            List<URL> urls = toUrls(url, providers);
            if (urls != null && urls.size() > 0) {
                notify(url, listener, urls);
            }
        } catch (Throwable e) {
            throw new RpcException("Failed to subscribe " + url + ", cause: " + e.getMessage(), e);
        }
    }

    protected void doUnsubscribe(URL url, NotifyListener listener) {
    }
    
    private String toServicePath(URL url) {
        return root + SEPARATOR + URL.encode(url.getServiceName());
    }
    
    private String toProviderPath(URL url) {
        return SEPARATOR + URL.encode(url.toFullString());
    }
    
    private List<URL> toUrls(URL consumer, List<String> providers) throws KeeperException, InterruptedException {
        List<URL> urls = new ArrayList<URL>();
        for (String provider : providers) {
            URL url = URL.valueOf(URL.decode(provider));
            if (UrlUtils.isMatch(consumer, url)) {
                urls.add(url);
            }
        }
        return urls;
    }

}