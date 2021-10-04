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
package org.apache.dubbo.config.spring.registrycenter;

import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingServer;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.RpcException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The default implementation of registry center can support single and multiple registry center.
 * <p>Each port represents an instance. You can provide multiple ports to build multiple registry center,
 * if you want to create multiple registry center
 */
class ZookeeperRegistryCenter extends AbstractRegistryCenter {

    /**
     * Initialize the default registry center.
     *
     * @param ports the registry center's ports.
     */
    public ZookeeperRegistryCenter(int... ports) {
        this.ports = ports;
        this.instanceSpecs = new ArrayList<>(this.ports.length);
        this.zookeeperServers = new ArrayList<>(this.ports.length);
    }

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperRegistryCenter.class);

    /**
     * The type of the registry center.
     */
    private static final String DEFAULT_REGISTRY_CENTER_TYPE = "zookeeper";

    private int[] ports;

    private List<InstanceSpec> instanceSpecs;

    private List<TestingServer> zookeeperServers;

    private AtomicBoolean started = new AtomicBoolean(false);

    /**
     * {@inheritDoc}
     */
    @Override
    public void startup() throws RpcException {
        try {
            if (started.compareAndSet(false, true)) {
                logger.info("The ZookeeperRegistryCenter is starting...");
                for (int port : this.ports) {
                    InstanceSpec instanceSpec = this.createInstanceSpec(port);
                    this.instanceSpecs.add(instanceSpec);
                    this.zookeeperServers.add(new TestingServer(instanceSpec, true));
                }
                logger.info("The ZookeeperRegistryCenter is started successfully");
            }
        } catch (Exception exception) {
            started.set(false);
            throw new RpcException("Failed to initialize ZookeeperRegistryCenter instance", exception);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Instance> getRegistryCenterInstance() throws RpcException {
        this.startup();
        List<Instance> instances = new ArrayList<>(this.instanceSpecs.size());
        for (InstanceSpec instanceSpec : this.instanceSpecs) {
            instances.add(new Instance() {
                @Override
                public String getType() {
                    return DEFAULT_REGISTRY_CENTER_TYPE;
                }

                @Override
                public String getHostname() {
                    return instanceSpec.getHostname();
                }

                @Override
                public int getPort() {
                    return instanceSpec.getPort();
                }

                @Override
                public String toURL() {
                    return String.format("%s://%s:%d", getType(), getHostname(), getPort());
                }
            });
        }
        return instances;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() throws RpcException {
        logger.info("The ZookeeperRegistryCenter is stopping...");
        List<RpcException> exceptions = new ArrayList<>(this.zookeeperServers.size());
        for (TestingServer testingServer : this.zookeeperServers) {
            try {
                testingServer.close();
                logger.info(String.format("The zookeeper instance of %s is shutdown successfully",
                    testingServer.getConnectString()));
            } catch (IOException exception) {
                RpcException rpcException = new RpcException(String.format("Failed to close zookeeper instance of %s",
                    testingServer.getConnectString()),
                    exception);
                exceptions.add(rpcException);
                logger.error(rpcException);
            }
        }
        this.instanceSpecs.clear();
        this.zookeeperServers.clear();
        if (!exceptions.isEmpty()) {
            logger.info("The ZookeeperRegistryCenter failed to close.");
            // throw any one of exceptions
            throw exceptions.get(0);
        } else {
            logger.info("The ZookeeperRegistryCenter close successfully.");
        }
    }
}
