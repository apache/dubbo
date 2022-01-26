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
package org.apache.dubbo.test.check.registrycenter.processor;

import org.apache.dubbo.test.check.exception.DubboTestException;
import org.apache.dubbo.test.check.registrycenter.Context;
import org.apache.dubbo.test.check.registrycenter.Processor;
import org.apache.dubbo.test.check.registrycenter.context.ZookeeperContext;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;

import java.util.concurrent.TimeUnit;

/**
 * Create {@link Process} to reset zookeeper.
 */
public class ResetZookeeperProcessor implements Processor {

    @Override
    public void process(Context context) throws DubboTestException {
        ZookeeperContext zookeeperContext = (ZookeeperContext)context;
        for (int clientPort : zookeeperContext.getClientPorts()) {
            CuratorFramework client;
            try {
                CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                    .connectString("127.0.0.1:" + clientPort)
                    .retryPolicy(new RetryNTimes(1, 1000));
                client = builder.build();
                client.start();
                boolean connected = client.blockUntilConnected(1000, TimeUnit.MILLISECONDS);
                if (!connected) {
                    throw new IllegalStateException("zookeeper not connected");
                }
                client.delete().deletingChildrenIfNeeded().forPath("/dubbo");
            } catch (Exception e) {
                throw new DubboTestException(e.getMessage(), e);
            }
        }
    }
}
