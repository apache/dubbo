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
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.configcenter.ConfigurationListener;
import org.apache.dubbo.configcenter.DynamicConfiguration;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;
import static org.apache.dubbo.configcenter.Constants.CONFIG_NAMESPACE_KEY;

/**
 *
 */
public class ZookeeperDynamicConfiguration implements DynamicConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperDynamicConfiguration.class);

    private Executor executor;
    // The final root path would be: /configRootPath/"config"
    private String rootPath;
    private final ZookeeperClient zkClient;
    private CountDownLatch initializedLatch;

    private CacheListener cacheListener;
    private URL url;


    ZookeeperDynamicConfiguration(URL url, ZookeeperTransporter zookeeperTransporter) {
        this.url = url;
        rootPath = PATH_SEPARATOR + url.getParameter(CONFIG_NAMESPACE_KEY, DEFAULT_GROUP) + "/config";

        initializedLatch = new CountDownLatch(1);
        this.cacheListener = new CacheListener(rootPath, initializedLatch);
        this.executor = Executors.newFixedThreadPool(1, new NamedThreadFactory(this.getClass().getSimpleName(), true));

        zkClient = zookeeperTransporter.connect(url);
        zkClient.addDataListener(rootPath, cacheListener, executor);
        try {
            // Wait for connection
            long timeout = url.getParameter("init.timeout", 5000);
            boolean isCountDown = this.initializedLatch.await(timeout, TimeUnit.MILLISECONDS);
            if (!isCountDown) {
                throw new IllegalStateException("Failed to receive INITIALIZED event from zookeeper, pls. check if url "
                        + url + " is correct");
            }
        } catch (InterruptedException e) {
            logger.warn("Failed to build local cache for config center (zookeeper)." + url);
        }
    }

    /**
     * @param key e.g., {service}.configurators, {service}.tagrouters, {group}.dubbo.properties
     * @return
     */
    @Override
    public Object getInternalProperty(String key) {
        return zkClient.getContent(key);
    }

    /**
     * For service governance, multi group is not supported by this implementation. So group is not used at present.
     */
    @Override
    public void addListener(String key, String group, ConfigurationListener listener) {
        cacheListener.addListener(getPathKey(group, key), listener);
    }

    @Override
    public void removeListener(String key, String group, ConfigurationListener listener) {
        cacheListener.removeListener(getPathKey(group, key), listener);
    }

    @Override
    public String getRule(String key, String group, long timeout) throws IllegalStateException {
        return (String) getInternalProperty(getPathKey(group, key));
    }

    @Override
    public String getProperties(String key, String group, long timeout) throws IllegalStateException {
        // use global group 'dubbo' if no group specified
        if (StringUtils.isEmpty(group)) {
            group = DEFAULT_GROUP;
        }
        return (String) getInternalProperty(getPathKey(group, key));
    }

    private String getPathKey(String group, String key) {
        return rootPath + PATH_SEPARATOR + group + PATH_SEPARATOR + key;
    }
}
