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
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 */
public class ZooKeeperConfigurationSource implements WatchedConfigurationSource, Closeable {
    public static final String ARCHAIUS_SOURCE_ADDRESS_KEY = "archaius.configurationSource.zk.address";
    public static final String ARCHAIUS_CONFIG_ROOT_PATH_KEY = "archaius.configurationSource.zk.rootpath";
    public static final String DEFAULT_CONFIG_ROOT_PATH = "/dubbo/config";

    private static final Logger logger = LoggerFactory.getLogger(com.netflix.config.source.ZooKeeperConfigurationSource.class);

    private final CuratorFramework client;
    private final String configRootPath;
    private final PathChildrenCache pathChildrenCache;

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
        this.client = client;
        this.configRootPath = configRootPath;
        this.pathChildrenCache = new PathChildrenCache(client, configRootPath, true);
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
        this.pathChildrenCache = new PathChildrenCache(client, configRootPath, true);
    }

    /**
     * Adds a listener to the pathChildrenCache, initializes the cache, then starts the cache-management background thread
     *
     * @throws Exception
     */
    public void start() throws Exception {
        // create the watcher for future configuration updatess
        pathChildrenCache.getListenable().addListener(new PathChildrenCacheListener() {
            public void childEvent(CuratorFramework aClient, PathChildrenCacheEvent event)
                    throws Exception {
                PathChildrenCacheEvent.Type eventType = event.getType();
                ChildData data = event.getData();

                String path = null;
                if (data != null) {
                    path = data.getPath();

                    // scrub configRootPath out of the key name
                    String key = removeRootPath(path);

                    byte[] value = data.getData();
                    String stringValue = new String(value, charset);

                    logger.debug("received update to pathName [{}], eventType [{}]", path, eventType);
                    logger.debug("key [{}], and value [{}]", key, stringValue);

                    // fire event to all listeners
                    Map<String, Object> added = null;
                    Map<String, Object> changed = null;
                    Map<String, Object> deleted = null;
                    if (eventType == PathChildrenCacheEvent.Type.CHILD_ADDED) {
                        added = new HashMap<String, Object>(1);
                        added.put(key, stringValue);
                    } else if (eventType == PathChildrenCacheEvent.Type.CHILD_UPDATED) {
                        changed = new HashMap<String, Object>(1);
                        changed.put(key, stringValue);
                    } else if (eventType == PathChildrenCacheEvent.Type.CHILD_REMOVED) {
                        deleted = new HashMap<String, Object>(1);
                        deleted.put(key, stringValue);
                    }

                    WatchedUpdateResult result = WatchedUpdateResult.createIncremental(added,
                            changed, deleted);

                    fireEvent(result);
                }
            }
        });

        // passing true to trigger an initial rebuild upon starting.  (blocking call)
        pathChildrenCache.start(true);
    }

    @Override
    public Map<String, Object> getCurrentData() throws Exception {
        logger.debug("getCurrentData() retrieving current data.");

        List<ChildData> children = pathChildrenCache.getCurrentData();
        Map<String, Object> all = new HashMap<String, Object>(children.size());
        for (ChildData child : children) {
            String path = child.getPath();
            String key = removeRootPath(path);
            byte[] value = child.getData();

            all.put(key, new String(value, charset));
        }

        logger.debug("getCurrentData() retrieved [{}] config elements.", children.size());

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

    /**
     * This is used to convert a configuration nodePath into a key
     *
     * @param nodePath
     * @return key (nodePath less the config root path)
     */
    private String removeRootPath(String nodePath) {
        return nodePath.replace(configRootPath + "/", "");
    }

    //@VisibleForTesting
    synchronized void setZkProperty(String key, String value) throws Exception {
        final String path = configRootPath + "/" + key;

        byte[] data = value.getBytes(charset);

        try {
            // attempt to create (intentionally doing this instead of checkExists())
            client.create().creatingParentsIfNeeded().forPath(path, data);
        } catch (KeeperException.NodeExistsException exc) {
            // key already exists - update the data instead
            client.setData().forPath(path, data);
        }
    }

    //@VisibleForTesting
    synchronized String getZkProperty(String key) throws Exception {
        final String path = configRootPath + "/" + key;

        byte[] bytes = client.getData().forPath(path);

        return new String(bytes, charset);
    }

    //@VisibleForTesting
    synchronized void deleteZkProperty(String key) throws Exception {
        final String path = configRootPath + "/" + key;

        try {
            client.delete().forPath(path);
        } catch (KeeperException.NoNodeException exc) {
            // Node doesn't exist - NoOp
            logger.warn("Node doesn't exist", exc);
        }
    }

    public void close() {
        try {
            Closeables.close(pathChildrenCache, true);
        } catch (IOException exc) {
            logger.error("IOException should not have been thrown.", exc);
        }
    }
}
