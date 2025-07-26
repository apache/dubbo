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
package com.alibaba.dubbo.registry.zookeeper;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.support.FailbackRegistry;
import com.alibaba.dubbo.remoting.zookeeper.ChildListener;
import com.alibaba.dubbo.remoting.zookeeper.StateListener;
import com.alibaba.dubbo.remoting.zookeeper.ZookeeperClient;
import com.alibaba.dubbo.remoting.zookeeper.ZookeeperTransporter;
import com.alibaba.dubbo.rpc.RpcException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ZookeeperRegistry
 *
 */
public class ZookeeperRegistry extends FailbackRegistry {

    private final static Logger logger = LoggerFactory.getLogger(ZookeeperRegistry.class);

    //默认端口
    private final static int DEFAULT_ZOOKEEPER_PORT = 2181;

    //默认zk根节点
    private final static String DEFAULT_ROOT = "dubbo";

    //zk根节点
    private final String root;

    //Service接口全名集合
    private final Set<String> anyServices = new ConcurrentHashSet<String>();

    //监听器集合
    private final ConcurrentMap<URL, ConcurrentMap<NotifyListener, ChildListener>> zkListeners = new ConcurrentHashMap<URL, ConcurrentMap<NotifyListener, ChildListener>>();

    //zk客户端
    private final ZookeeperClient zkClient;

    public ZookeeperRegistry(URL url, ZookeeperTransporter zookeeperTransporter) {
        //设置注册中心的URL
        super(url);
        if (url.isAnyHost()) {
            throw new IllegalStateException("registry address == null");
        }
        //获取zookeeper跟节点
        String group = url.getParameter(Constants.GROUP_KEY, DEFAULT_ROOT);
        if (!group.startsWith(Constants.PATH_SEPARATOR)) {
            group = Constants.PATH_SEPARATOR + group;
        }
        this.root = group;
        //创建zk client
        zkClient = zookeeperTransporter.connect(url);
        //添加 StateListener 对象。该监听器，在重连时，调用恢复方法
        zkClient.addStateListener(new StateListener() {
            public void stateChanged(int state) {
                if (state == RECONNECTED) {
                    try {
                        recover();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        });
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

    public boolean isAvailable() {
        return zkClient.isConnected();
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
            String urlPath = toUrlPath(url);
            //是否动态数据。若为false，该数据为持久数据，当注册方退出时，数据依然保存在数据中心
            boolean dynamic = url.getParameter(Constants.DYNAMIC_KEY, true);
            //创建URL节点
            zkClient.create(urlPath, dynamic);
        } catch (Throwable e) {
            throw new RpcException("Failed to register " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    /**
     * 取消注册
     * @param url
     */
    protected void doUnregister(URL url) {
        try {
            zkClient.delete(toUrlPath(url));
        } catch (Throwable e) {
            throw new RpcException("Failed to unregister " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    /**
     * 进行订阅
     * @param url
     * @param listener
     */
    protected void doSubscribe(final URL url, final NotifyListener listener) {
        try {
            //处理对所有Service层的发起订阅，例如监控中心的订阅
            if (Constants.ANY_VALUE.equals(url.getServiceInterface())) {
                String root = toRootPath();//dubbo
                //获取url对应的监听器集合
                ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners.get(url);
                if (listeners == null) {
                    //创建监听器集合
                    zkListeners.putIfAbsent(url, new ConcurrentHashMap<NotifyListener, ChildListener>());
                    listeners = zkListeners.get(url);
                }
                //获取NotifyListener对应的ChildListener
                ChildListener zkListener = listeners.get(listener);
                if (zkListener == null) {
                    //不存在，则进行创建
                    listeners.putIfAbsent(listener, new ChildListener() {
                        public void childChanged(String parentPath, List<String> currentChilds) {
                            for (String child : currentChilds) {
                                child = URL.decode(child);
                                //新增 Service 接口全名时（即新增服务），发起该Service层的订阅
                                if (!anyServices.contains(child)) {
                                    anyServices.add(child);
                                    subscribe(url.setPath(child).addParameters(Constants.INTERFACE_KEY, child,
                                            Constants.CHECK_KEY, String.valueOf(false)), listener);
                                }
                            }
                        }
                    });
                    zkListener = listeners.get(listener);
                }
                //创建root节点(dubbo)，该节点为持久节点
                zkClient.create(root, false);
                //向zookeeper的root节点发起订阅
                List<String> services = zkClient.addChildListener(root, zkListener);
                //首次全量数据获取完成时，循环Service接口全名数组，发起所有Service层的订阅
                if (services != null && !services.isEmpty()) {
                    for (String service : services) {
                        service = URL.decode(service);
                        anyServices.add(service);
                        subscribe(url.setPath(service).addParameters(Constants.INTERFACE_KEY, service,
                                Constants.CHECK_KEY, String.valueOf(false)), listener);
                    }
                }
            } else {//处理指定Service的发起订阅
                //子节点数据数组，及Service下的所有url
                List<URL> urls = new ArrayList<URL>();
                //获得分类路径数据，例如：/dubbo/com.gyf.TestService/providers
                String[] categoriesPath = toCategoriesPath(url);
                for (String path : categoriesPath) {
                    //获取url对应的监听器集合
                    ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners.get(url);
                    if (listeners == null) {
                        //不存在，则进行创建
                        zkListeners.putIfAbsent(url, new ConcurrentHashMap<NotifyListener, ChildListener>());
                        listeners = zkListeners.get(url);
                    }
                    //获取NotifyListener对应的ChildListener
                    //在URL层发生变更时，会调用NotifyListener#notify(url, listener, currentChilds)方法，回调NotifyListener的逻辑。
                    ChildListener zkListener = listeners.get(listener);
                    if (zkListener == null) {
                        //不存在则进行创建
                        listeners.putIfAbsent(listener, new ChildListener() {
                            public void childChanged(String parentPath, List<String> currentChilds) {
                                //变更时，调用 `#notify(...)` 方法，回调 NotifyListener
                                ZookeeperRegistry.this.notify(url, listener, toUrlsWithEmpty(url, parentPath, currentChilds));
                            }
                        });
                        zkListener = listeners.get(listener);
                    }
                    //创建Type路径(/dubbo/com.gyf.TestService/providers)，该节点也是持久路径
                    zkClient.create(path, false);
                    //向zk中对应的type路径发起订阅
                    //当Service下面有新增加的服务提供者
                        //ChildListener会被回调->FailbackRegistry.notify->AbstractRegistry.notify(URL,NotifyListener,List<URL>)
                            //NotifyListener.notify()
                    //List<String> children是分录下的所有url
                    List<String> children = zkClient.addChildListener(path, zkListener);
                    if (children != null) {
                        //添加到urls
                        urls.addAll(toUrlsWithEmpty(url, path, children));
                    }
                }
                //首次全量数据获取完成时，调用notify回调NotifyListener->服务消费者可创建所有的Invoker对象，用于调用服务提供者们
                notify(url, listener, urls);
            }
        } catch (Throwable e) {
            throw new RpcException("Failed to subscribe " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    /**
     * 向zk取消订阅
     * @param url
     * @param listener
     */
    protected void doUnsubscribe(URL url, NotifyListener listener) {
        ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners.get(url);
        if (listeners != null) {
            ChildListener zkListener = listeners.get(listener);
            if (zkListener != null) {
                zkClient.removeChildListener(toUrlPath(url), zkListener);
            }
        }
    }

    /**
     * 查找符合条件的已注册数据，与订阅的推模式相比，这是拉模式
     * @param url 查询条件
     * @return 已注册信息列表，可能为空
     */
    public List<URL> lookup(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("lookup url == null");
        }
        try {
            List<String> providers = new ArrayList<String>();
            //循环分类目录
            for (String path : toCategoriesPath(url)) {
                List<String> children = zkClient.getChildren(path);
                if (children != null) {
                    providers.addAll(children);
                }
            }
            //匹配
            return toUrlsWithoutEmpty(url, providers);
        } catch (Throwable e) {
            throw new RpcException("Failed to lookup " + url + " from zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    /**
     * 获得跟路径
     * @return
     */
    private String toRootDir() {
        if (root.equals(Constants.PATH_SEPARATOR)) {
            return root;
        }
        return root + Constants.PATH_SEPARATOR;
    }

    /**
     * 根路径，默认返回dubbo
     * @return
     */
    private String toRootPath() {
        return root;
    }

    /**
     * /dubbo/com.gyf.TestService
     * 获得服务路径
     * Root + Service
     * @param url
     * @return
     */
    private String toServicePath(URL url) {
        String name = url.getServiceInterface();
        if (Constants.ANY_VALUE.equals(name)) {
            return toRootPath();
        }
        return toRootDir() + URL.encode(name);
    }

    /**
     * 获得分类路径数组
     * @param url
     * @return /dubbo/com.gyf.TestService/providers
     */
    private String[] toCategoriesPath(URL url) {
        //分类数组
        String[] categories;
        if (Constants.ANY_VALUE.equals(url.getParameter(Constants.CATEGORY_KEY))) {
            categories = new String[]{Constants.PROVIDERS_CATEGORY, Constants.CONSUMERS_CATEGORY,
                    Constants.ROUTERS_CATEGORY, Constants.CONFIGURATORS_CATEGORY};
        } else {
            categories = url.getParameter(Constants.CATEGORY_KEY, new String[]{Constants.DEFAULT_CATEGORY});
        }
        String[] paths = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            paths[i] = toServicePath(url) + Constants.PATH_SEPARATOR + categories[i];
        }
        return paths;
    }

    /**
     * 获得分类路径
     * @param url
     * @return 分类路径
     */
    private String toCategoryPath(URL url) {
        //toServicePath(url)/providers
        return toServicePath(url) + Constants.PATH_SEPARATOR + url.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
    }

    /**
     * 获取URL的路径
     * Root + Service + Type + URL
     * @param url
     * @return
     */
    private String toUrlPath(URL url) {
        return toCategoryPath(url) + Constants.PATH_SEPARATOR + URL.encode(url.toFullString());
    }

    /**
     * 获得 providers 中，和 consumer 匹配的 URL 数组
     * @param consumer 用于匹配的URL
     * @param providers 被匹配的URL字符串
     * @return
     */
    private List<URL> toUrlsWithoutEmpty(URL consumer, List<String> providers) {
        List<URL> urls = new ArrayList<URL>();
        if (providers != null && !providers.isEmpty()) {
            for (String provider : providers) {
                provider = URL.decode(provider);
                //是url
                if (provider.contains("://")) {
                    //将字符串转换成URL尝试匹配
                    URL url = URL.valueOf(provider);
                    if (UrlUtils.isMatch(consumer, url)) {
                        urls.add(url);
                    }
                }
            }
        }
        return urls;
    }

    /**
     *
     * @param consumer
     * @param path 分类路径
     * @param providers
     * @return
     */
    private List<URL> toUrlsWithEmpty(URL consumer, String path, List<String> providers) {
        //获得providers中和consumer匹配的URL数组
        List<URL> urls = toUrlsWithoutEmpty(consumer, providers);
        //若不存在匹配，则创建 `empty://` 的 URL返回。通过这样的方式，可以处理类似服务提供者为空的情况。
        if (urls == null || urls.isEmpty()) {
            int i = path.lastIndexOf('/');
            String category = i < 0 ? path : path.substring(i + 1);
            URL empty = consumer.setProtocol(Constants.EMPTY_PROTOCOL).addParameter(Constants.CATEGORY_KEY, category);
            urls.add(empty);
        }
        return urls;
    }

}