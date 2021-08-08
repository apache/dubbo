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
package org.apache.dubbo.registrycenter;

import org.apache.curator.test.TestingServer;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.rpc.RpcException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The default multiple registry center.
 */
public class DefaultMultipleRegistryCenter implements MultipleRegistryCenter {

    private static final Logger logger = LoggerFactory.getLogger(DefaultMultipleRegistryCenter.class);

    /**
     * Define a zookeeper server.
     */
    private volatile TestingServer zookeeperServer1;

    /**
     * Define a zookeeper server.
     */
    private volatile TestingServer zookeeperServer2;

    /**
     * The zookeeper server's default port.
     */
    private static final int DEFAULT_PORT = 2181;

    /**
     * {@inheritDoc}
     */
    @Override
    public void startup() throws RpcException {
        try {
            logger.info("The DefaultMultipleRegistryCenter is starting...");
            this.zookeeperServer1 = new TestingServer(DEFAULT_PORT);
            this.zookeeperServer2 = new TestingServer();
            logger.info("The DefaultMultipleRegistryCenter is started successfully");
        } catch (Exception exception) {
            try {
                if (this.zookeeperServer1 != null) {
                    this.zookeeperServer1.close();
                }
                if (this.zookeeperServer2 != null) {
                    this.zookeeperServer2.close();
                }
            } catch (IOException e) {
                // ignore
            } finally {
                this.zookeeperServer1 = null;
                this.zookeeperServer2 = null;
            }
            throw new RpcException("Failed to initialize DefaultMultipleRegistryCenter instance", exception);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<RegistryConfig> getRegistryConfigs() {
        List<RegistryConfig> registryConfigs = new ArrayList<>(2);
        registryConfigs.add(new RegistryConfig("zookeeper://" + this.zookeeperServer1.getConnectString()));
        registryConfigs.add(new RegistryConfig("zookeeper://" + this.zookeeperServer2.getConnectString()));
        return registryConfigs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() throws RpcException {
        try {
            logger.info("The DefaultMultipleRegistryCenter is stopping...");
            if (this.zookeeperServer1 != null) {
                this.zookeeperServer1.close();
            }
            if (this.zookeeperServer2 != null) {
                this.zookeeperServer2.close();
            }
            logger.info("The DefaultMultipleRegistryCenter is shutdown successfully");
        } catch (IOException exception) {
            throw new RpcException("Failed to close DefaultMultipleRegistryCenter instance", exception);
        } finally {
            this.zookeeperServer1 = null;
            this.zookeeperServer2 = null;
        }
    }
}
