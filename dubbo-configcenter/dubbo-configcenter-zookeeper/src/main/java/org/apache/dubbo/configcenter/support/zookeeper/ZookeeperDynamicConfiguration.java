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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigItem;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.config.configcenter.TreePathDynamicConfiguration;
import org.apache.dubbo.common.threadpool.support.AbortPolicyWithReport;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter;

import org.apache.zookeeper.data.Stat;

import java.util.Collection;
<<<<<<< HEAD
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
=======
import java.util.Map;
import java.util.concurrent.Executor;
>>>>>>> origin/3.2
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FAILED_CONNECT_REGISTRY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ZOOKEEPER_EXCEPTION;

public class ZookeeperDynamicConfiguration extends TreePathDynamicConfiguration {

    private Executor executor;
<<<<<<< HEAD
    // The final root path would be: /configRootPath/"config"
    private String rootPath;
    private final ZookeeperClient zkClient;

    private CacheListener cacheListener;
    private URL url;
=======
    private ZookeeperClient zkClient;

    private CacheListener cacheListener;
>>>>>>> origin/3.2
    private static final int DEFAULT_ZK_EXECUTOR_THREADS_NUM = 1;
    private static final int DEFAULT_QUEUE = 10000;
    private static final Long THREAD_KEEP_ALIVE_TIME = 0L;

    ZookeeperDynamicConfiguration(URL url, ZookeeperTransporter zookeeperTransporter) {
        super(url);

<<<<<<< HEAD
        this.cacheListener = new CacheListener(rootPath);

        final String threadName = this.getClass().getSimpleName();
        this.executor = new ThreadPoolExecutor(DEFAULT_ZK_EXECUTOR_THREADS_NUM, DEFAULT_ZK_EXECUTOR_THREADS_NUM,
                THREAD_KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(DEFAULT_QUEUE),
                new NamedThreadFactory(threadName, true),
                new AbortPolicyWithReport(threadName, url));
=======
        this.cacheListener = new CacheListener();

        final String threadName = this.getClass().getSimpleName();
        this.executor = new ThreadPoolExecutor(DEFAULT_ZK_EXECUTOR_THREADS_NUM, DEFAULT_ZK_EXECUTOR_THREADS_NUM,
            THREAD_KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(DEFAULT_QUEUE),
            new NamedThreadFactory(threadName, true),
            new AbortPolicyWithReport(threadName, url));
>>>>>>> origin/3.2

        zkClient = zookeeperTransporter.connect(url);
        boolean isConnected = zkClient.isConnected();
        if (!isConnected) {
<<<<<<< HEAD
            throw new IllegalStateException("Failed to connect with zookeeper, pls check if url " + url + " is correct.");
=======

            IllegalStateException illegalStateException =
                new IllegalStateException("Failed to connect with zookeeper, pls check if url " + url + " is correct.");

            if (logger != null) {
                logger.error(CONFIG_FAILED_CONNECT_REGISTRY, "configuration server offline", "",
                    "Failed to connect with zookeeper", illegalStateException);
            }

            throw illegalStateException;
>>>>>>> origin/3.2
        }
    }

    /**
     * @param key e.g., {service}.configurators, {service}.tagrouters, {group}.dubbo.properties
     * @return
     */
    @Override
    public String getInternalProperty(String key) {
        return zkClient.getContent(buildPathKey("", key));
    }

    @Override
    protected void doClose() throws Exception {
<<<<<<< HEAD
        zkClient.close();
        if (executor instanceof ExecutorService) {
            ExecutorService executorService = (ExecutorService) executor;
            if (!executorService.isShutdown()) {
                executorService.shutdown();
            }
        }
        cacheListener.removeAllListeners();
=======
        // remove data listener
        Map<String, ZookeeperDataListener> pathKeyListeners = cacheListener.getPathKeyListeners();
        for (Map.Entry<String, ZookeeperDataListener> entry : pathKeyListeners.entrySet()) {
            zkClient.removeDataListener(entry.getKey(), entry.getValue());
        }
        cacheListener.clear();

        // zkClient is shared in framework, should not close it here
        // zkClient.close();
        // See: org.apache.dubbo.remoting.zookeeper.AbstractZookeeperTransporter#destroy()
        // All zk clients is created and destroyed in ZookeeperTransporter.
        zkClient = null;
>>>>>>> origin/3.2
    }

    @Override
    protected boolean doPublishConfig(String pathKey, String content) throws Exception {
        zkClient.createOrUpdate(pathKey, content, false);
        return true;
    }

    @Override
    public boolean publishConfigCas(String key, String group, String content, Object ticket) {
        try {
            if (ticket != null && !(ticket instanceof Stat)) {
                throw new IllegalArgumentException("zookeeper publishConfigCas requires stat type ticket");
            }
            String pathKey = buildPathKey(group, key);
            zkClient.createOrUpdate(pathKey, content, false, ticket == null ? 0 : ((Stat) ticket).getVersion());
            return true;
        } catch (Exception e) {
            logger.warn(REGISTRY_ZOOKEEPER_EXCEPTION, "", "", "zookeeper publishConfigCas failed.", e);
            return false;
        }
    }

    @Override
    protected String doGetConfig(String pathKey) throws Exception {
        return zkClient.getContent(pathKey);
    }

    @Override
    public ConfigItem getConfigItem(String key, String group) {
        String pathKey = buildPathKey(group, key);
        return zkClient.getConfigItem(pathKey);
    }

    @Override
    protected boolean doRemoveConfig(String pathKey) throws Exception {
        zkClient.delete(pathKey);
        return true;
    }

    @Override
    protected Collection<String> doGetConfigKeys(String groupPath) {
        return zkClient.getChildren(groupPath);
    }

    @Override
<<<<<<< HEAD
    protected void doAddListener(String pathKey, ConfigurationListener listener) {
        cacheListener.addListener(pathKey, listener);
        zkClient.addDataListener(pathKey, cacheListener, executor);
=======
    protected void doAddListener(String pathKey, ConfigurationListener listener, String key, String group) {
        ZookeeperDataListener cachedListener = cacheListener.getCachedListener(pathKey);
        if (cachedListener != null) {
            cachedListener.addListener(listener);
        } else {
            ZookeeperDataListener addedListener = cacheListener.addListener(pathKey, listener, key, group);
            zkClient.addDataListener(pathKey, addedListener, executor);
        }
>>>>>>> origin/3.2
    }

    @Override
    protected void doRemoveListener(String pathKey, ConfigurationListener listener) {
<<<<<<< HEAD
        cacheListener.removeListener(pathKey, listener);
        Set<ConfigurationListener> configurationListeners = cacheListener.getConfigurationListeners(pathKey);
        if (CollectionUtils.isEmpty(configurationListeners)) {
            zkClient.removeDataListener(pathKey, cacheListener);
=======
        ZookeeperDataListener zookeeperDataListener = cacheListener.removeListener(pathKey, listener);
        if (zookeeperDataListener != null && CollectionUtils.isEmpty(zookeeperDataListener.getListeners())) {
            zkClient.removeDataListener(pathKey, zookeeperDataListener);
>>>>>>> origin/3.2
        }
    }
}
