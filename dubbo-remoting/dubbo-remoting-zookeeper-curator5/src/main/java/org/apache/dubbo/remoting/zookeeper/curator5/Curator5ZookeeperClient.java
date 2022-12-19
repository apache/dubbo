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
package org.apache.dubbo.remoting.zookeeper.curator5;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigItem;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.zookeeper.AbstractZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.ChildListener;
import org.apache.dubbo.remoting.zookeeper.DataListener;
import org.apache.dubbo.remoting.zookeeper.EventType;
import org.apache.dubbo.remoting.zookeeper.StateListener;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.SESSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FAILED_CONNECT_REGISTRY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ZOOKEEPER_EXCEPTION;


public class Curator5ZookeeperClient extends AbstractZookeeperClient<Curator5ZookeeperClient.NodeCacheListenerImpl, Curator5ZookeeperClient.CuratorWatcherImpl> {

    protected static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(Curator5ZookeeperClient.class);

    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private final CuratorFramework client;
    private static Map<String, NodeCache> nodeCacheMap = new ConcurrentHashMap<>();

    public Curator5ZookeeperClient(URL url) {
        super(url);
        try {
            int timeout = url.getParameter(TIMEOUT_KEY, DEFAULT_CONNECTION_TIMEOUT_MS);
            int sessionExpireMs = url.getParameter(SESSION_KEY, DEFAULT_SESSION_TIMEOUT_MS);
            CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString(url.getBackupAddress())
                .retryPolicy(new RetryNTimes(1, 1000))
                .connectionTimeoutMs(timeout)
                .sessionTimeoutMs(sessionExpireMs);
            String userInformation = url.getUserInformation();
            if (userInformation != null && userInformation.length() > 0) {
                builder = builder.authorization("digest", userInformation.getBytes());
                builder.aclProvider(new ACLProvider() {
                    @Override
                    public List<ACL> getDefaultAcl() {
                        return ZooDefs.Ids.CREATOR_ALL_ACL;
                    }

                    @Override
                    public List<ACL> getAclForPath(String path) {
                        return ZooDefs.Ids.CREATOR_ALL_ACL;
                    }
                });
            }
            client = builder.build();
            client.getConnectionStateListenable().addListener(new CuratorConnectionStateListener(url));
            client.start();
            boolean connected = client.blockUntilConnected(timeout, TimeUnit.MILLISECONDS);

            if (!connected) {
                IllegalStateException illegalStateException = new IllegalStateException("zookeeper not connected, the address is: " + url);

                // 5-1 Failed to connect to configuration center.
                logger.error(CONFIG_FAILED_CONNECT_REGISTRY, "Zookeeper server offline", "",
                    "Failed to connect with zookeeper", illegalStateException);

                throw illegalStateException;
            }

        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public void createPersistent(String path, boolean faultTolerant) {
        try {
            client.create().forPath(path);
        } catch (NodeExistsException e) {
            if (!faultTolerant) {
                logger.warn(REGISTRY_ZOOKEEPER_EXCEPTION, "", "", "ZNode " + path + " already exists.", e);
                throw new IllegalStateException(e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public void createEphemeral(String path, boolean faultTolerant) {
        try {
            client.create().withMode(CreateMode.EPHEMERAL).forPath(path);
        } catch (NodeExistsException e) {
            if (faultTolerant) {
                logger.info("ZNode " + path + " already exists, since we will only try to recreate a node on a session expiration" +
                    ", this duplication might be caused by a delete delay from the zk server, which means the old expired session" +
                    " may still holds this ZNode and the server just hasn't got time to do the deletion. In this case, " +
                    "we can just try to delete and create again.");
                deletePath(path);
                createEphemeral(path, true);
            } else {
                logger.warn(REGISTRY_ZOOKEEPER_EXCEPTION, "", "", "ZNode " + path + " already exists.", e);
                throw new IllegalStateException(e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    protected void createPersistent(String path, String data, boolean faultTolerant) {
        byte[] dataBytes = data.getBytes(CHARSET);
        try {
            client.create().forPath(path, dataBytes);
        } catch (NodeExistsException e) {
            if (faultTolerant) {
                logger.info("ZNode " + path + " already exists. Will be override with new data.");
                try {
                    client.setData().forPath(path, dataBytes);
                } catch (Exception e1) {
                    throw new IllegalStateException(e.getMessage(), e1);
                }
            } else {
                logger.warn(REGISTRY_ZOOKEEPER_EXCEPTION, "", "", "ZNode " + path + " already exists.", e);
                throw new IllegalStateException(e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    protected void createEphemeral(String path, String data, boolean faultTolerant) {
        byte[] dataBytes = data.getBytes(CHARSET);
        try {
            client.create().withMode(CreateMode.EPHEMERAL).forPath(path, dataBytes);
        } catch (NodeExistsException e) {
            if (faultTolerant) {
                logger.info("ZNode " + path + " already exists, since we will only try to recreate a node on a session expiration" +
                    ", this duplication might be caused by a delete delay from the zk server, which means the old expired session" +
                    " may still holds this ZNode and the server just hasn't got time to do the deletion. In this case, " +
                    "we can just try to delete and create again.");
                deletePath(path);
                createEphemeral(path, data, true);
            } else {
                logger.warn(REGISTRY_ZOOKEEPER_EXCEPTION, "", "", "ZNode " + path + " already exists.", e);
                throw new IllegalStateException(e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    protected void update(String path, String data, int version) {
        byte[] dataBytes = data.getBytes(CHARSET);
        try {
            client.setData().withVersion(version).forPath(path, dataBytes);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    protected void update(String path, String data) {
        byte[] dataBytes = data.getBytes(CHARSET);
        try {
            client.setData().forPath(path, dataBytes);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    protected void createOrUpdatePersistent(String path, String data) {
        try {
            if (checkExists(path)) {
                update(path, data);
            } else {
                createPersistent(path, data, true);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

    }

    @Override
    protected void createOrUpdateEphemeral(String path, String data) {
        try {
            if (checkExists(path)) {
                update(path, data);
            } else {
                createEphemeral(path, data, true);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

    }

    @Override
    protected void createOrUpdatePersistent(String path, String data, Integer version) {
        try {
            if (checkExists(path) && version != null) {
                update(path, data, version);
            } else {
                createPersistent(path, data, false);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    protected void createOrUpdateEphemeral(String path, String data, Integer version) {
        try {
            if (checkExists(path) && version != null) {
                update(path, data, version);
            } else {
                createEphemeral(path, data, false);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    protected void deletePath(String path) {
        try {
            client.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (NoNodeException ignored) {
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public List<String> getChildren(String path) {
        try {
            return client.getChildren().forPath(path);
        } catch (NoNodeException e) {
            return null;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public boolean checkExists(String path) {
        try {
            if (client.checkExists().forPath(path) != null) {
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public boolean isConnected() {
        return client.getZookeeperClient().isConnected();
    }

    @Override
    public String doGetContent(String path) {
        try {
            byte[] dataBytes = client.getData().forPath(path);
            return (dataBytes == null || dataBytes.length == 0) ? null : new String(dataBytes, CHARSET);
        } catch (NoNodeException e) {
            // ignore NoNode Exception.
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public ConfigItem doGetConfigItem(String path) {
        String content;
        Stat stat;
        try {
            stat = new Stat();
            byte[] dataBytes = client.getData().storingStatIn(stat).forPath(path);
            content = (dataBytes == null || dataBytes.length == 0) ? null : new String(dataBytes, CHARSET);
        } catch (NoNodeException e) {
            return new ConfigItem();
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return new ConfigItem(content, stat);
    }

    @Override
    public void doClose() {
        super.doClose();
        client.close();
    }

    @Override
    public Curator5ZookeeperClient.CuratorWatcherImpl createTargetChildListener(String path, ChildListener listener) {
        return new Curator5ZookeeperClient.CuratorWatcherImpl(client, listener, path);
    }

    @Override
    public List<String> addTargetChildListener(String path, CuratorWatcherImpl listener) {
        try {
            return client.getChildren().usingWatcher(listener).forPath(path);
        } catch (NoNodeException e) {
            return null;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    protected Curator5ZookeeperClient.NodeCacheListenerImpl createTargetDataListener(String path, DataListener listener) {
        return new NodeCacheListenerImpl(client, listener, path);
    }

    @Override
    protected void addTargetDataListener(String path, Curator5ZookeeperClient.NodeCacheListenerImpl nodeCacheListener) {
        this.addTargetDataListener(path, nodeCacheListener, null);
    }

    @Override
    protected void addTargetDataListener(String path, Curator5ZookeeperClient.NodeCacheListenerImpl nodeCacheListener, Executor executor) {
        try {
            NodeCache nodeCache = new NodeCache(client, path);
            if (nodeCacheMap.putIfAbsent(path, nodeCache) != null) {
                return;
            }
            if (executor == null) {
                nodeCache.getListenable().addListener(nodeCacheListener);
            } else {
                nodeCache.getListenable().addListener(nodeCacheListener, executor);
            }

            nodeCache.start();
        } catch (Exception e) {
            throw new IllegalStateException("Add nodeCache listener for path:" + path, e);
        }
    }

    @Override
    protected void removeTargetDataListener(String path, Curator5ZookeeperClient.NodeCacheListenerImpl nodeCacheListener) {
        NodeCache nodeCache = nodeCacheMap.get(path);
        if (nodeCache != null) {
            nodeCache.getListenable().removeListener(nodeCacheListener);
        }
        nodeCacheListener.dataListener = null;
    }

    @Override
    public void removeTargetChildListener(String path, CuratorWatcherImpl listener) {
        listener.unwatch();
    }

    static class NodeCacheListenerImpl implements NodeCacheListener {

        private CuratorFramework client;

        private volatile DataListener dataListener;

        private String path;

        protected NodeCacheListenerImpl() {
        }

        public NodeCacheListenerImpl(CuratorFramework client, DataListener dataListener, String path) {
            this.client = client;
            this.dataListener = dataListener;
            this.path = path;
        }

        @Override
        public void nodeChanged() throws Exception {
            ChildData childData = nodeCacheMap.get(path).getCurrentData();
            String content = null;
            EventType eventType;
            if (childData == null) {
                eventType = EventType.NodeDeleted;
            } else if (childData.getStat().getVersion() == 0) {
                content = new String(childData.getData(), CHARSET);
                eventType = EventType.NodeCreated;
            } else {
                content = new String(childData.getData(), CHARSET);
                eventType = EventType.NodeDataChanged;
            }
            dataListener.dataChanged(path, content, eventType);
        }
    }

    static class CuratorWatcherImpl implements CuratorWatcher {

        private CuratorFramework client;
        private volatile ChildListener childListener;
        private String path;

        public CuratorWatcherImpl(CuratorFramework client, ChildListener listener, String path) {
            this.client = client;
            this.childListener = listener;
            this.path = path;
        }

        protected CuratorWatcherImpl() {
        }

        public void unwatch() {
            this.childListener = null;
        }

        @Override
        public void process(WatchedEvent event) throws Exception {
            // if client connect or disconnect to server, zookeeper will queue
            // watched event(Watcher.Event.EventType.None, .., path = null).
            if (event.getType() == Watcher.Event.EventType.None) {
                return;
            }

            if (childListener != null) {
                childListener.childChanged(path, client.getChildren().usingWatcher(this).forPath(path));
            }
        }
    }

    private class CuratorConnectionStateListener implements ConnectionStateListener {
        private final long UNKNOWN_SESSION_ID = -1L;

        private long lastSessionId;
        private int timeout;
        private int sessionExpireMs;

        public CuratorConnectionStateListener(URL url) {
            this.timeout = url.getParameter(TIMEOUT_KEY, DEFAULT_CONNECTION_TIMEOUT_MS);
            this.sessionExpireMs = url.getParameter(SESSION_KEY, DEFAULT_SESSION_TIMEOUT_MS);
        }

        @Override
        public void stateChanged(CuratorFramework client, ConnectionState state) {
            long sessionId = UNKNOWN_SESSION_ID;
            try {
                sessionId = client.getZookeeperClient().getZooKeeper().getSessionId();
            } catch (Exception e) {
                logger.warn(REGISTRY_ZOOKEEPER_EXCEPTION, "", "", "Curator client state changed, but failed to get the related zk session instance.");
            }

            if (state == ConnectionState.LOST) {
                logger.warn(REGISTRY_ZOOKEEPER_EXCEPTION, "", "", "Curator zookeeper session " + Long.toHexString(lastSessionId) + " expired.");
                Curator5ZookeeperClient.this.stateChanged(StateListener.SESSION_LOST);
            } else if (state == ConnectionState.SUSPENDED) {
                logger.warn(REGISTRY_ZOOKEEPER_EXCEPTION, "", "", "Curator zookeeper connection of session " + Long.toHexString(sessionId) + " timed out. " +
                    "connection timeout value is " + timeout + ", session expire timeout value is " + sessionExpireMs);
                Curator5ZookeeperClient.this.stateChanged(StateListener.SUSPENDED);
            } else if (state == ConnectionState.CONNECTED) {
                lastSessionId = sessionId;
                logger.info("Curator zookeeper client instance initiated successfully, session id is " + Long.toHexString(sessionId));
                Curator5ZookeeperClient.this.stateChanged(StateListener.CONNECTED);
            } else if (state == ConnectionState.RECONNECTED) {
                if (lastSessionId == sessionId && sessionId != UNKNOWN_SESSION_ID) {
                    logger.warn(REGISTRY_ZOOKEEPER_EXCEPTION, "", "", "Curator zookeeper connection recovered from connection lose, " +
                        "reuse the old session " + Long.toHexString(sessionId));
                    Curator5ZookeeperClient.this.stateChanged(StateListener.RECONNECTED);
                } else {
                    logger.warn(REGISTRY_ZOOKEEPER_EXCEPTION, "", "", "New session created after old session lost, " +
                        "old session " + Long.toHexString(lastSessionId) + ", new session " + Long.toHexString(sessionId));
                    lastSessionId = sessionId;
                    Curator5ZookeeperClient.this.stateChanged(StateListener.NEW_SESSION_CREATED);
                }
            }
        }

    }

    /**
     * just for unit test
     *
     * @return
     */
    CuratorFramework getClient() {
        return client;
    }
}
