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
package org.apache.dubbo.remoting.zookeeper.zkclient;


import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.exception.ZkNoNodeException;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.zookeeper.ChildListener;
import org.apache.dubbo.remoting.zookeeper.StateListener;
import org.apache.dubbo.remoting.zookeeper.support.AbstractZookeeperClient;
import org.apache.zookeeper.Watcher.Event.KeeperState;

import java.util.List;

public class ZkclientZookeeperClient extends AbstractZookeeperClient<IZkChildListener> {

    private Logger logger = LoggerFactory.getLogger(ZkclientZookeeperClient.class);

    private final ZkClientWrapper client;

    private volatile KeeperState state = KeeperState.SyncConnected;

    public ZkclientZookeeperClient(URL url) {
        super(url);
        long timeout = url.getParameter(Constants.TIMEOUT_KEY, 30000L);
        client = new ZkClientWrapper(url.getBackupAddress(), timeout);
        client.addListener(new IZkStateListener() {
            @Override
            public void handleStateChanged(KeeperState state) throws Exception {
                ZkclientZookeeperClient.this.state = state;
                if (state == KeeperState.Disconnected) {
                    stateChanged(StateListener.DISCONNECTED);
                } else if (state == KeeperState.SyncConnected) {
                    stateChanged(StateListener.CONNECTED);
                }
            }

            @Override
            public void handleNewSession() throws Exception {
                stateChanged(StateListener.RECONNECTED);
            }
        });
        client.start();
    }

    @Override
    public void createPersistent(String path) {
        try {
            client.createPersistent(path);
        } catch (ZkNodeExistsException e) {
            logger.error("zookeeper failed to create persistent node with " + path + ": ", e);
        }
    }

    @Override
    public void createEphemeral(String path) {
        try {
            client.createEphemeral(path);
        } catch (ZkNodeExistsException e) {
            logger.error("zookeeper failed to create ephemeral node with " + path + ": ", e);
        }
    }

    @Override
    protected void createPersistent(String path, String data) {
        try {
            client.createPersistent(path, data);
        } catch (ZkNodeExistsException e) {
            logger.error("zookeeper failed to create persistent node with " +
                    path + " and " + data + " : ", e);
        }
    }

    @Override
    protected void createEphemeral(String path, String data) {
        try {
            client.createEphemeral(path, data);
        } catch (ZkNodeExistsException e) {
            logger.error("zookeeper failed to create ephemeral node with " +
                    path + " and " + data + " : ", e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            client.delete(path);
        } catch (ZkNoNodeException e) {
            logger.error("zookeeper failed to delete node with " + path + ": ", e);
        }
    }

    @Override
    public List<String> getChildren(String path) {
        try {
            return client.getChildren(path);
        } catch (ZkNoNodeException e) {
            logger.error("zookeeper failed to get children node with " + path + ": ", e);
            return null;
        }
    }

    @Override
    public boolean checkExists(String path) {
        try {
            return client.exists(path);
        } catch (Throwable t) {
            logger.error("zookeeper failed to check node existing with " + path + ": ", t);
        }
        return false;
    }

    @Override
    public boolean isConnected() {
        return state == KeeperState.SyncConnected;
    }

    @Override
    public String doGetContent(String path) {
        try {
            return client.getData(path);
        } catch (ZkNoNodeException e) {
            logger.error("zookeeper failed to get data with " + path + ": ", e);
            return null;
        }
    }

    @Override
    public void doClose() {
        client.close();
    }

    @Override
    public IZkChildListener createTargetChildListener(String path, final ChildListener listener) {
        return listener::childChanged;
    }

    @Override
    public List<String> addTargetChildListener(String path, final IZkChildListener listener) {
        return client.subscribeChildChanges(path, listener);
    }

    @Override
    public void removeTargetChildListener(String path, IZkChildListener listener) {
        client.unsubscribeChildChanges(path, listener);
    }

}
