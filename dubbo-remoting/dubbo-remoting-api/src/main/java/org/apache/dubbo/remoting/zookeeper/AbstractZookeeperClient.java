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
package org.apache.dubbo.remoting.zookeeper;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigItem;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashSet;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;

public abstract class AbstractZookeeperClient<TargetDataListener, TargetChildListener> implements ZookeeperClient {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractZookeeperClient.class);

    // may hang up to wait name resolution up to 10s
    protected int DEFAULT_CONNECTION_TIMEOUT_MS = 30 * 1000;
    protected int DEFAULT_SESSION_TIMEOUT_MS = 60 * 1000;

    private final URL url;

    private final Set<StateListener> stateListeners = new CopyOnWriteArraySet<StateListener>();

    private final ConcurrentMap<String, ConcurrentMap<ChildListener, TargetChildListener>> childListeners =
            new ConcurrentHashMap<String, ConcurrentMap<ChildListener, TargetChildListener>>();

    private final ConcurrentMap<String, ConcurrentMap<DataListener, TargetDataListener>> listeners =
            new ConcurrentHashMap<String, ConcurrentMap<DataListener, TargetDataListener>>();

    private volatile boolean closed = false;

    private final Set<String> persistentExistNodePath = new ConcurrentHashSet<>();

    public AbstractZookeeperClient(URL url) {
        this.url = url;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public void delete(String path) {
        //never mind if ephemeral
        persistentExistNodePath.remove(path);
        deletePath(path);
    }


    @Override
    public void create(String path, boolean ephemeral) {
        if (!ephemeral) {
            if (persistentExistNodePath.contains(path)) {
                return;
            }
            if (checkExists(path)) {
                persistentExistNodePath.add(path);
                return;
            }
        }
        int i = path.lastIndexOf('/');
        if (i > 0) {
            create(path.substring(0, i), false);
        }
        if (ephemeral) {
            createEphemeral(path);
        } else {
            createPersistent(path);
            persistentExistNodePath.add(path);
        }
    }

    @Override
    public void addStateListener(StateListener listener) {
        stateListeners.add(listener);
    }

    @Override
    public void removeStateListener(StateListener listener) {
        stateListeners.remove(listener);
    }

    public Set<StateListener> getSessionListeners() {
        return stateListeners;
    }

    @Override
    public List<String> addChildListener(String path, final ChildListener listener) {
        ConcurrentMap<ChildListener, TargetChildListener> listeners = childListeners.computeIfAbsent(path, k -> new ConcurrentHashMap<>());
        TargetChildListener targetListener = listeners.computeIfAbsent(listener, k -> createTargetChildListener(path, k));
        return addTargetChildListener(path, targetListener);
    }

    @Override
    public void addDataListener(String path, DataListener listener) {
        this.addDataListener(path, listener, null);
    }

    @Override
    public void addDataListener(String path, DataListener listener, Executor executor) {
        ConcurrentMap<DataListener, TargetDataListener> dataListenerMap = listeners.computeIfAbsent(path, k -> new ConcurrentHashMap<>());
        TargetDataListener targetListener = dataListenerMap.computeIfAbsent(listener, k -> createTargetDataListener(path, k));
        addTargetDataListener(path, targetListener, executor);
    }

    @Override
    public void removeDataListener(String path, DataListener listener) {
        ConcurrentMap<DataListener, TargetDataListener> dataListenerMap = listeners.get(path);
        if (dataListenerMap != null) {
            TargetDataListener targetListener = dataListenerMap.remove(listener);
            if (targetListener != null) {
                removeTargetDataListener(path, targetListener);
            }
        }
    }

    @Override
    public void removeChildListener(String path, ChildListener listener) {
        ConcurrentMap<ChildListener, TargetChildListener> listeners = childListeners.get(path);
        if (listeners != null) {
            TargetChildListener targetListener = listeners.remove(listener);
            if (targetListener != null) {
                removeTargetChildListener(path, targetListener);
            }
        }
    }

    protected void stateChanged(int state) {
        for (StateListener sessionListener : getSessionListeners()) {
            sessionListener.stateChanged(state);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            doClose();
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
    }

    @Override
    public void create(String path, String content, boolean ephemeral) {
        if (checkExists(path)) {
            delete(path);
        }
        int i = path.lastIndexOf('/');
        if (i > 0) {
            create(path.substring(0, i), false);
        }
        if (ephemeral) {
            createEphemeral(path, content);
        } else {
            createPersistent(path, content);
        }
    }

    @Override
    public void createOrUpdate(String path, String content, boolean ephemeral, int version) {
        int i = path.lastIndexOf('/');
        if (i > 0) {
            create(path.substring(0, i), false);
        }
        if (ephemeral) {
            createOrUpdateEphemeral(path, content, version);
        } else {
            createOrUpdatePersistent(path, content, version);
        }
    }

    @Override
    public String getContent(String path) {
        if (!checkExists(path)) {
            return null;
        }
        return doGetContent(path);
    }

    @Override
    public ConfigItem getConfigItem(String path) {
        return doGetConfigItem(path);
    }

    protected void doClose() {
        // Break circular reference of zk client
        stateListeners.clear();
    }

    protected abstract void createPersistent(String path);

    protected abstract void createEphemeral(String path);

    protected abstract void createPersistent(String path, String data);

    protected abstract void createEphemeral(String path, String data);

    protected abstract void update(String path, String data, int version);

    protected abstract void createOrUpdatePersistent(String path, String data, int version);

    protected abstract void createOrUpdateEphemeral(String path, String data, int version);

    @Override
    public abstract boolean checkExists(String path);

    protected abstract TargetChildListener createTargetChildListener(String path, ChildListener listener);

    protected abstract List<String> addTargetChildListener(String path, TargetChildListener listener);

    protected abstract TargetDataListener createTargetDataListener(String path, DataListener listener);

    protected abstract void addTargetDataListener(String path, TargetDataListener listener);

    protected abstract void addTargetDataListener(String path, TargetDataListener listener, Executor executor);

    protected abstract void removeTargetDataListener(String path, TargetDataListener listener);

    protected abstract void removeTargetChildListener(String path, TargetChildListener listener);

    protected abstract String doGetContent(String path);

    protected abstract ConfigItem doGetConfigItem(String path);

    /**
     * we invoke the zookeeper client to delete the node
     *
     * @param path the node path
     */
    protected abstract void deletePath(String path);

}
