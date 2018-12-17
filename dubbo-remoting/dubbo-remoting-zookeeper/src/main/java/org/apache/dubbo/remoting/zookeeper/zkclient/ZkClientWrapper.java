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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.Assert;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.Watcher.Event.KeeperState;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Zkclient wrapper class that can monitor the state of the connection automatically after the connection is out of time
 * It is also consistent with the use of curator
 *
 * @date 2017/10/29
 */
public class ZkClientWrapper {
    private Logger logger = LoggerFactory.getLogger(ZkClientWrapper.class);
    private long timeout;
    private ZkClient client;
    private volatile KeeperState state;
    private CompletableFuture<ZkClient> completableFuture;
    private volatile boolean started = false;

    public ZkClientWrapper(final String serverAddr, long timeout) {
        this.timeout = timeout;
        completableFuture = CompletableFuture.supplyAsync(() -> new ZkClient(serverAddr, Integer.MAX_VALUE));
    }

    public void start() {
        if (!started) {
            try {
                client = completableFuture.get(timeout, TimeUnit.MILLISECONDS);
//                this.client.subscribeStateChanges(IZkStateListener);
            } catch (Throwable t) {
                logger.error("Timeout! zookeeper server can not be connected in : " + timeout + "ms!", t);
                completableFuture.whenComplete(this::makeClientReady);
            }
            started = true;
        } else {
            logger.warn("Zkclient has already been started!");
        }
    }

    public void addListener(IZkStateListener listener) {
        completableFuture.whenComplete((value, exception) -> {
            this.makeClientReady(value, exception);
            if (exception == null) {
                client.subscribeStateChanges(listener);
            }
        });
    }

    public boolean isConnected() {
//        return client != null && state == KeeperState.SyncConnected;
        return client != null;
    }

    public void createPersistent(String path) {
        Assert.notNull(client, new IllegalStateException("Zookeeper is not connected yet!"));
        client.createPersistent(path, true);
    }

    public void createEphemeral(String path) {
        Assert.notNull(client, new IllegalStateException("Zookeeper is not connected yet!"));
        client.createEphemeral(path);
    }

    public void delete(String path) {
        Assert.notNull(client, new IllegalStateException("Zookeeper is not connected yet!"));
        client.delete(path);
    }

    public List<String> getChildren(String path) {
        Assert.notNull(client, new IllegalStateException("Zookeeper is not connected yet!"));
        return client.getChildren(path);
    }

    public boolean exists(String path) {
        Assert.notNull(client, new IllegalStateException("Zookeeper is not connected yet!"));
        return client.exists(path);
    }

    public void close() {
        Assert.notNull(client, new IllegalStateException("Zookeeper is not connected yet!"));
        client.close();
    }

    public List<String> subscribeChildChanges(String path, final IZkChildListener listener) {
        Assert.notNull(client, new IllegalStateException("Zookeeper is not connected yet!"));
        return client.subscribeChildChanges(path, listener);
    }

    public void unsubscribeChildChanges(String path, IZkChildListener listener) {
        Assert.notNull(client, new IllegalStateException("Zookeeper is not connected yet!"));
        client.unsubscribeChildChanges(path, listener);
    }

    private void makeClientReady(ZkClient client, Throwable e) {
        if (e != null) {
            logger.error("Got an exception when trying to create zkclient instance, can not connect to zookeeper server, please check!", e);
        } else {
            this.client = client;
//            this.client.subscribeStateChanges(IZkStateListener);
        }
    }


}
