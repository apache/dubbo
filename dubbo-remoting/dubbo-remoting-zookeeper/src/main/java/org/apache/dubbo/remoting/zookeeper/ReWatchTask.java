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

import org.apache.curator.framework.CuratorFramework;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.timer.Timeout;
import org.apache.dubbo.common.timer.TimerTask;
import org.apache.dubbo.remoting.zookeeper.curator.CuratorZookeeperClient;
import org.apache.zookeeper.KeeperException;


public class ReWatchTask implements TimerTask {

    protected static final Logger logger = LoggerFactory.getLogger(ReWatchTask.class);

    private CuratorFramework client;

    private String path;

    private ChildListener childListener;

    private CuratorZookeeperClient.CuratorWatcherImpl curatorWatcher;

    public ReWatchTask(CuratorFramework client, String path, ChildListener childListener, CuratorZookeeperClient.CuratorWatcherImpl curatorWatcher) {
        this.client = client;
        this.path = path;
        this.childListener = childListener;
        this.curatorWatcher = curatorWatcher;
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        if (timeout.isCancelled() || timeout.timer().isStop()) {
            // other thread cancel this timeout or stop the timer.
            return;
        }
        try {
            childListener.childChanged(path, client.getChildren().usingWatcher(curatorWatcher).forPath(path));
        } catch (KeeperException e) {
            logger.warn("Failed to watch " + path + ", waiting for retry, cause: " + e.getMessage(), e);
            curatorWatcher.addReWatchTask(client, path, childListener, curatorWatcher);
        }
    }
}
