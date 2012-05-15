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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.Watcher.Event.KeeperState;

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
    
    // private final boolean       auth;
    
    // private final List<ACL>     acl; // zkclient 0.1.0 unsupported

    private final Set<String> anyServices = new ConcurrentHashSet<String>();

    private final ConcurrentMap<NotifyListener, IZkChildListener> zkListeners = new ConcurrentHashMap<NotifyListener, IZkChildListener>();
    
    private final ZkClient zkClient;
    
    private volatile KeeperState zkState;

    public ZookeeperRegistry(URL url) {
        super(url);
        // this.auth = url.getUsername() != null && url.getUsername().length() > 0 
        //         && url.getPassword() != null && url.getPassword().length() > 0;
        // this.acl = auth ? Ids.CREATOR_ALL_ACL : Ids.OPEN_ACL_UNSAFE;
        String group = url.getParameter(Constants.GROUP_KEY, DEFAULT_ROOT);
        if (! group.startsWith(Constants.PATH_SEPARATOR)) {
            group = Constants.PATH_SEPARATOR + group;
        }
        this.root = group;
        StringBuilder address = new StringBuilder(appendDefaultPort(url.getAddress()));
        String[] backups = url.getParameter(Constants.BACKUP_KEY, new String[0]);
        if (backups != null && backups.length > 0) {
            for (String backup : backups) {
                address.append(",");
                address.append(appendDefaultPort(backup));
            }
        }
        zkClient = new ZkClient(address.toString());
        zkClient.subscribeStateChanges(new IZkStateListener() {
            public void handleStateChanged(KeeperState state) throws Exception {
                ZookeeperRegistry.this.zkState = state;
            }
            public void handleNewSession() throws Exception {
            }
        });
    }

    public boolean isAvailable() {
        return zkState == KeeperState.SyncConnected;
    }

    public void destroy() {
        super.destroy();
        try {
            zkClient.close();
        } catch (Exception e) {
            logger.warn("Failed to close zookeeper client " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    protected void doRegister(URL url) {
        try {
            if (url.getParameter(Constants.DYNAMIC_KEY, true)) {
                zkClient.createPersistent(toCategoryPath(url), true);
                zkClient.createEphemeral(toUrlPath(url));
            } else {
                zkClient.createPersistent(toUrlPath(url), true);
            }
        } catch (Throwable e) {
            throw new RpcException("Failed to register " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    protected void doUnregister(URL url) {
        try {
            zkClient.delete(toUrlPath(url));
        } catch (Throwable e) {
            throw new RpcException("Failed to unregister " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    protected void doSubscribe(final URL url, final NotifyListener listener) {
        try {
            if (Constants.ANY_VALUE.equals(url.getServiceInterface())) {
                String root = toRootPath();
                IZkChildListener zkListener = zkListeners.get(listener);
                if (zkListener == null) {
                    zkListeners.putIfAbsent(listener, new IZkChildListener() {
                        public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
                            for (String child : currentChilds) {
                                if (! anyServices.contains(child)) {
                                    anyServices.add(child);
                                    subscribe(url.setPath(child).addParameters(Constants.INTERFACE_KEY, child, 
                                            Constants.CHECK_KEY, String.valueOf(false), Constants.REGISTER_KEY, String.valueOf(false)), listener);
                                }
                            }
                        }
                    });
                    zkListener = zkListeners.get(listener);
                }
                List<String> services = zkClient.subscribeChildChanges(root, zkListener);
                if (services != null && services.size() > 0) {
                    anyServices.addAll(services);
                    for (String service : services) {
                        subscribe(url.setPath(service).addParameters(Constants.INTERFACE_KEY, service, 
                                Constants.CHECK_KEY, String.valueOf(false), Constants.REGISTER_KEY, String.valueOf(false)), listener);
                    }
                }
            } else {
                List<String> providers = new ArrayList<String>();
                for (String path : toCategoriesPath(url)) {
                    IZkChildListener zkListener = zkListeners.get(listener);
                    if (zkListener == null) {
                        zkListeners.putIfAbsent(listener, new IZkChildListener() {
                            public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
                                ZookeeperRegistry.this.notify(url, listener, toUrls(url, currentChilds));
                            }
                        });
                        zkListener = zkListeners.get(listener);
                    }
                    List<String> children = zkClient.subscribeChildChanges(path, zkListener);
                    if (children != null) {
                        providers.addAll(children);
                    }
                }
                List<URL> urls = toUrls(url, providers);
                notify(url, listener, urls);
            }
        } catch (Throwable e) {
            throw new RpcException("Failed to subscribe " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    protected void doUnsubscribe(URL url, NotifyListener listener) {
        zkClient.unsubscribeChildChanges(toUrlPath(url), zkListeners.get(listener));
    }

    public List<URL> lookup(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("lookup url == null");
        }
        try {
            List<String> providers = new ArrayList<String>();
            for (String path : toCategoriesPath(url)) {
                List<String> children = zkClient.getChildren(path);
                if (children != null) {
                    providers.addAll(children);
                }
            }
            return toUrls(url, providers);
        } catch (NoNodeException e) {
            return new ArrayList<URL>(0);
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

    private String[] toCategoriesPath(URL url) {
        String[] categroies = url.getParameter(Constants.CATEGORY_KEY, new String[] {Constants.DEFAULT_CATEGORY});
        String[] paths = new String[categroies.length];
        for (int i = 0; i < categroies.length; i ++) {
            paths[i] = toServicePath(url) + Constants.PATH_SEPARATOR + categroies[i];
        }
        return paths;
    }

    private String toCategoryPath(URL url) {
        return toServicePath(url) + Constants.PATH_SEPARATOR + url.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
    }

    private String toUrlPath(URL url) {
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
        if (urls != null && urls.isEmpty() && Constants.ANY_VALUE.equals(consumer.getServiceInterface())) {
            urls.add(consumer.setProtocol(Constants.EMPTY_PROTOCOL));
        }
        return urls;
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

}