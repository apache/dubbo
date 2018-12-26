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
package org.apache.dubbo.configcenter.support.zookeeper;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.configcenter.AbstractDynamicConfiguration;
import org.apache.dubbo.configcenter.ConfigurationListener;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.apache.curator.framework.CuratorFrameworkFactory.newClient;
import static org.apache.dubbo.common.Constants.CONFIG_NAMESPACE_KEY;

/**
 *
 */
public class ZookeeperDynamicConfiguration extends AbstractDynamicConfiguration<CacheListener> {
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperDynamicConfiguration.class);
    private Executor executor;
    private CuratorFramework client;

    // The final root path would be: /configRootPath/"config"
    private String rootPath;
    private TreeCache treeCache;
    private CountDownLatch initializedLatch;

    private CacheListener cacheListener;

    ZookeeperDynamicConfiguration() {
    }

    ZookeeperDynamicConfiguration(URL url) {
        super(url);
    }

    protected void initWith(URL url) {
        rootPath = "/" + url.getParameter(CONFIG_NAMESPACE_KEY, DEFAULT_GROUP) + "/config";

        RetryPolicy policy = new ExponentialBackoffRetry(1000, 3);
        int sessionTimeout = url.getParameter("config.session.timeout", 60 * 1000);
        int connectTimeout = url.getParameter("config.connect.timeout", 10 * 1000);
        String connectString = url.getBackupAddress();
        client = newClient(connectString, sessionTimeout, connectTimeout, policy);
        client.start();

        try {
            boolean connected = client.blockUntilConnected(3 * connectTimeout, TimeUnit.MILLISECONDS);
            if (!connected) {
                if (url.getParameter(Constants.CONFIG_CHECK_KEY, true)) {
                    throw new IllegalStateException("Failed to connect to config center (zookeeper): "
                            + connectString + " in " + 3 * connectTimeout + "ms.");
                } else {
                    logger.warn("The config center (zookeeper) is not fully initialized in " + 3 * connectTimeout + "ms, address is: " + connectString);
                }
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException("The thread was interrupted unexpectedly when trying connecting to zookeeper "
                    + connectString + " config center, ", e);
        }

        initializedLatch = new CountDownLatch(1);
        this.cacheListener = new CacheListener(rootPath, initializedLatch);
        this.executor = Executors.newFixedThreadPool(1, new NamedThreadFactory(this.getClass().getSimpleName(), true));
        // build local cache
        try {
            this.buildCache();
        } catch (Exception e) {
            logger.warn("Failed to build local cache for config center (zookeeper), address is ." + connectString);
        }
    }

    @Override
    protected String getTargetConfig(String key, String group, long timeout) {
        if (StringUtils.isNotEmpty(group)) {
            key = group + "/" + key;
        } else {
            int i = key.lastIndexOf(".");
            key = key.substring(0, i) + "/" + key.substring(i + 1);
        }

        return (String) getInternalProperty(rootPath + "/" + key);
    }

    /**
     * For service governance, multi group is not supported by this implementation. So group is not used at present.
     *
     * @param key
     * @param group
     * @param cacheListener
     * @param listener
     */
    @Override
    protected void addConfigurationListener(String key, String group, CacheListener cacheListener, ConfigurationListener listener) {
        cacheListener.addListener(key, listener);
    }

    @Override
    protected void removeConfigurationListener(String key, String group, CacheListener cacheListener, ConfigurationListener configurationListener) {
        cacheListener.removeListener(key, configurationListener);
    }

    @Override
    protected CacheListener createTargetListener(String key, String group) {
        return cacheListener;
    }

    /**
     * @param key e.g., {service}.configurators, {service}.tagrouters, {group}.dubbo.properties
     * @return
     */
    @Override
    protected Object getInternalProperty(String key) {
        ChildData childData = treeCache.getCurrentData(key);
        if (childData != null) {
            return new String(childData.getData(), StandardCharsets.UTF_8);
        }
        return null;
    }

    /**
     * Adds a listener to the pathChildrenCache, initializes the cache, then starts the cache-management background
     * thread
     */
    private void buildCache() throws Exception {
        this.treeCache = new TreeCache(client, rootPath);
        // create the watcher for future configuration updates
        treeCache.getListenable().addListener(cacheListener, executor);

        // it's not blocking, so we use an extra latch 'initializedLatch' to make sure cache fully initialized before use.
        treeCache.start();
        initializedLatch.await();
    }
}
