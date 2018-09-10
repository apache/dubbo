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
package org.apache.dubbo.config.dynamic.support.archaius.sources;

import com.google.common.io.Closeables;
import com.netflix.config.WatchedConfigurationSource;
import com.netflix.config.WatchedUpdateListener;
import com.netflix.config.WatchedUpdateResult;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.dubbo.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class ZooKeeperConfigurationSource implements WatchedConfigurationSource, Closeable {
    public static final String ARCHAIUS_SOURCE_ADDRESS_KEY = "archaius.zk.address";
    public static final String ARCHAIUS_CONFIG_ROOT_PATH_KEY = "archaius.zk.rootpath";
    public static final String DEFAULT_CONFIG_ROOT_PATH = "/dubbo/config";

    private static final Logger logger = LoggerFactory.getLogger(com.netflix.config.source.ZooKeeperConfigurationSource.class);
    private Executor executor = Executors.newFixedThreadPool(1);
    private final CuratorFramework client;

    private final String configRootPath;
    private final TreeCache treeCache;
    private boolean connected = false;

    private final Charset charset = Charset.forName("UTF-8");

    private List<WatchedUpdateListener> listeners = new CopyOnWriteArrayList<WatchedUpdateListener>();

    public ZooKeeperConfigurationSource() {
        this(System.getProperty(ARCHAIUS_SOURCE_ADDRESS_KEY), 60 * 1000, 60 * 1000, System.getProperty(ARCHAIUS_CONFIG_ROOT_PATH_KEY, DEFAULT_CONFIG_ROOT_PATH));
    }

    public ZooKeeperConfigurationSource(int sessionTimeout, int connectTimeout, String configRootPath) {
        this(System.getProperty(ARCHAIUS_SOURCE_ADDRESS_KEY), sessionTimeout, connectTimeout, configRootPath);
    }


    public ZooKeeperConfigurationSource(String connectString, int sessionTimeout, int connectTimeout, String configRootPath) {
        if (connectString == null) {
            throw new IllegalArgumentException("connectString==null, must specify the address to connect for zookeeper archaius source.");
        }

        CuratorFramework client = CuratorFrameworkFactory.newClient(connectString, sessionTimeout, connectTimeout,
                new ExponentialBackoffRetry(1000, 3));
        client.start();
        try {
            connected = client.blockUntilConnected(connectTimeout * 4, TimeUnit.MILLISECONDS);
            if (!connected) {
                logger.warn("Cannot connect to ConfigCenter at zookeeper " + connectString + " in " + connectTimeout * 4 + "ms");
            }
        } catch (InterruptedException e) {
            logger.error("The thread was interrupted unexpectedly when try connecting to zookeeper " + connectString + " as ConfigCenter, ", e);
        }
        this.client = client;
        this.configRootPath = configRootPath;
        this.treeCache = new TreeCache(client, configRootPath);
    }

    /**
     * Creates the pathChildrenCache using the CuratorFramework client and ZK root path node for the config
     *
     * @param client
     * @param configRootPath path to ZK root parent node for the rest of the configuration properties (ie. /<my-app>/config)
     */
    public ZooKeeperConfigurationSource(CuratorFramework client, String configRootPath) {
        this.client = client;
        this.configRootPath = configRootPath;
        this.treeCache = new TreeCache(client, configRootPath);
    }

    /**
     * Adds a listener to the pathChildrenCache, initializes the cache, then starts the cache-management background thread
     *
     * @throws Exception
     */
    public void start() throws Exception {
        // create the watcher for future configuration updatess
        treeCache.getListenable().addListener(new TreeCacheListener() {
            public void childEvent(CuratorFramework aClient, TreeCacheEvent event)
                    throws Exception {

                TreeCacheEvent.Type type = event.getType();
                ChildData data = event.getData();
                if (type == TreeCacheEvent.Type.INITIALIZED || type == TreeCacheEvent.Type.CONNECTION_RECONNECTED) {
                    connected = true;
                }

                // TODO, ignore other event types
                if (data == null) {
                    return;
                }

                if (data.getPath().split("/").length == 5) {
                    byte[] value = data.getData();
                    String stringValue = new String(value, charset);

                    // fire event to all listeners
                    Map<String, Object> added = null;
                    Map<String, Object> changed = null;
                    Map<String, Object> deleted = null;

                    switch (type) {
                        case NODE_ADDED:
                            added = new HashMap<>(1);
                            added.put(pathToKey(data.getPath()), stringValue);
                            break;
                        case NODE_REMOVED:
                            deleted = new HashMap<>(1);
                            deleted.put(pathToKey(data.getPath()), stringValue);
                            break;
                        case NODE_UPDATED:
                            changed = new HashMap<>(1);
                            changed.put(pathToKey(data.getPath()), stringValue);
                    }

                    WatchedUpdateResult result = WatchedUpdateResult.createIncremental(added,
                            changed, deleted);

                    fireEvent(result);
                }
            }
        }, executor);

        // passing true to trigger an initial rebuild upon starting.  (blocking call)
        treeCache.start();
    }

    /**
     * This is used to convert a configuration nodePath into a key
     *
     * @param path
     * @return key (nodePath less the config root path)
     */
    private String pathToKey(String path) {
        if (StringUtils.isEmpty(path)) {
            return path;
        }
        return path.replace(configRootPath + "/", "").replaceAll("/", ".");
    }

    @Override
    public Map<String, Object> getCurrentData() throws Exception {
        logger.debug("getCurrentData() retrieving current data.");

        Map<String, Object> all = new HashMap<>();

        if (!connected) {
            logger.warn("ConfigServer is not connected yet, zookeeper don't support local snapshot yet, so there's no old data to use!");
            return all;
        }

        Map<String, ChildData> dataMap = treeCache.getCurrentChildren(configRootPath);
        if (dataMap != null && dataMap.size() > 0) {
            dataMap.forEach((childPath, v) -> {
                String fullChildPath = configRootPath + "/" + childPath;
                treeCache.getCurrentChildren(fullChildPath).forEach((subChildPath, childData) -> {
                    all.put(pathToKey(fullChildPath + "/" + subChildPath), new String(childData.getData(), charset));
                });
            });
        }

        logger.debug("getCurrentData() retrieved [{}] config elements.", all.size());

        return all;
    }

    @Override
    public void addUpdateListener(WatchedUpdateListener l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    @Override
    public void removeUpdateListener(WatchedUpdateListener l) {
        if (l != null) {
            listeners.remove(l);
        }
    }

    protected void fireEvent(WatchedUpdateResult result) {
        for (WatchedUpdateListener l : listeners) {
            try {
                l.updateConfiguration(result);
            } catch (Throwable ex) {
                logger.error("Error in invoking WatchedUpdateListener", ex);
            }
        }
    }

    public void close() {
        try {
            Closeables.close(treeCache, true);
        } catch (IOException exc) {
            logger.error("IOException should not have been thrown.", exc);
        }
    }

    public boolean isConnected() {
        return connected;
    }
}
