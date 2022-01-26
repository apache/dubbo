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
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class ZookeeperDynamicConfiguration extends TreePathDynamicConfiguration {

    private Executor executor;
    private ZookeeperClient zkClient;

    private CacheListener cacheListener;
    private static final int DEFAULT_ZK_EXECUTOR_THREADS_NUM = 1;
    private static final int DEFAULT_QUEUE = 10000;
    private static final Long THREAD_KEEP_ALIVE_TIME = 0L;

    ZookeeperDynamicConfiguration(URL url, ZookeeperTransporter zookeeperTransporter) {
        super(url);

        this.cacheListener = new CacheListener(rootPath);

        final String threadName = this.getClass().getSimpleName();
        this.executor = new ThreadPoolExecutor(DEFAULT_ZK_EXECUTOR_THREADS_NUM, DEFAULT_ZK_EXECUTOR_THREADS_NUM,
            THREAD_KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(DEFAULT_QUEUE),
            new NamedThreadFactory(threadName, true),
            new AbortPolicyWithReport(threadName, url));

        zkClient = zookeeperTransporter.connect(url);
        boolean isConnected = zkClient.isConnected();
        if (!isConnected) {
            throw new IllegalStateException("Failed to connect with zookeeper, pls check if url " + url + " is correct.");
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
        // zkClient is shared in framework, should not close it here
        // zkClient.close();
        // See: org.apache.dubbo.remoting.zookeeper.AbstractZookeeperTransporter#destroy()
        // All zk clients is created and destroyed in ZookeeperTransporter.
        zkClient = null;
    }

    @Override
    protected boolean doPublishConfig(String pathKey, String content) throws Exception {
        zkClient.create(pathKey, content, false);
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
            logger.warn("zookeeper publishConfigCas failed.", e);
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
    protected void doAddListener(String pathKey, ConfigurationListener listener) {
        cacheListener.addListener(pathKey, listener);
        zkClient.addDataListener(pathKey, cacheListener, executor);
    }

    @Override
    protected void doRemoveListener(String pathKey, ConfigurationListener listener) {
        cacheListener.removeListener(pathKey, listener);
        Set<ConfigurationListener> configurationListeners = cacheListener.getConfigurationListeners(pathKey);
        if (CollectionUtils.isNotEmpty(configurationListeners)) {
            zkClient.removeDataListener(pathKey, cacheListener);
        }
    }
}
