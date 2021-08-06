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
package org.apache.dubbo.integration.multiple;

import org.apache.dubbo.common.utils.NetUtils;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.UUID;

/**
 * The zookeeper server is just for testing.
 * Also there is only one static instance, which can be created.
 * <p>
 * Note: It can only be used if the following conditions are satisfied
 * <p>1. Integration testcase instead of unit testcase
 * <p>2. You can use only one zookeeper instance per Package, because the zookeeper is a static global instance.
 */
public class MultipleZooKeeperServer {

    private static final Logger logger = LoggerFactory.getLogger(MultipleZooKeeperServer.class);

    /**
     * Define a static zookeeper instance.
     */
    private static volatile InnerZooKeeper INSTANCE_ONE;

    /**
     * Define a static zookeeper instance.
     */
    private static volatile InnerZooKeeper INSTANCE_TWO;

    /**
     * Start the zookeeper instances.
     */
    public static void start() {
        if (INSTANCE_ONE == null) {
            INSTANCE_ONE = new InnerZooKeeper("multiple-zookeeper-server-one-for-test",
                NetUtils.getAvailablePort());
            INSTANCE_ONE.start();
        }
        if (INSTANCE_TWO == null) {
            INSTANCE_TWO = new InnerZooKeeper("multiple-zookeeper-server-two-for-test",
                NetUtils.getAvailablePort());
            INSTANCE_TWO.start();
        }
    }

    /**
     * Returns the zookeeper-one server's port.
     */
    public static int getPortOne() {
        return INSTANCE_ONE.getClientPort();
    }

    /**
     * Returns the zookeeper-two server's port.
     */
    public static int getPortTwo() {
        return INSTANCE_TWO.getClientPort();
    }

    /**
     * Checks if all zookeeper servers are running.
     */
    public static boolean isRunning() {
        return INSTANCE_ONE.isRunning() && INSTANCE_TWO.isRunning();
    }

    /**
     * Shutdown all zookeeper instances.
     */
    public static void shutdown() {
        if (INSTANCE_ONE != null) {
            INSTANCE_ONE.shutdown();
            INSTANCE_ONE = null;
        }

        if (INSTANCE_TWO != null) {
            INSTANCE_TWO.shutdown();
            INSTANCE_TWO = null;
        }
    }

    /**
     * from: https://github.com/spring-projects/spring-xd/blob/v1.3.1.RELEASE/spring-xd-dirt/src/main/java/org/springframework/xd/dirt/zookeeper/ZooKeeperUtils.java
     * <p>
     * Helper class to start an embedded instance of standalone (non clustered) ZooKeeper.
     * <p>
     * NOTE: at least an external standalone server (if not an ensemble) are recommended, even for
     */
    private static class InnerZooKeeper {

        /**
         * ZooKeeper client port. This will be determined dynamically upon startup.
         */
        private final int clientPort;

        /**
         * ZooKeeper name.
         */
        private final String zookeeperName;

        /**
         * Thread for running the ZooKeeper server.
         */
        private volatile Thread zkServerThread;

        /**
         * ZooKeeper server.
         */
        private volatile ZooKeeperServerMain zkServer;

        /**
         * Construct an EmbeddedZooKeeper with the provided port.
         *
         * @param zookeeperName zookeeper name
         * @param clientPort port for ZooKeeper server to bind to
         */
        public InnerZooKeeper(String zookeeperName, int clientPort) {
            this.zookeeperName = zookeeperName;
            this.clientPort = clientPort;
        }

        /**
         * Returns the port that clients should use to connect to this embedded server.
         *
         * @return dynamically determined client port
         */
        public int getClientPort() {
            return this.clientPort;
        }

        /**
         * Checks if the zookeeper server is running.
         */
        public boolean isRunning() {
            return (zkServerThread != null);
        }

        /**
         * Start the ZooKeeper server in a background thread.
         * <p>
         * any exceptions thrown during startup or execution.
         */
        private synchronized void start() {
            if (zkServerThread == null) {
                zkServerThread = new Thread(() -> {
                    try {
                        Properties properties = new Properties();
                        File file = new File(System.getProperty("java.io.tmpdir")
                            + File.separator + UUID.randomUUID());
                        file.deleteOnExit();
                        properties.setProperty("dataDir", file.getAbsolutePath());
                        properties.setProperty("clientPort", String.valueOf(clientPort));

                        QuorumPeerConfig quorumPeerConfig = new QuorumPeerConfig();
                        quorumPeerConfig.parseProperties(properties);

                        zkServer = new ZooKeeperServerMain();
                        ServerConfig configuration = new ServerConfig();
                        configuration.readFrom(quorumPeerConfig);

                        zkServer.runFromConfig(configuration);
                    } catch (Exception e) {
                        logger.error("Exception running embedded ZooKeeper", e);
                    }
                }, zookeeperName);
                zkServerThread.setDaemon(true);
                zkServerThread.start();
            }
        }

        /**
         * Shutdown the ZooKeeper server.
         */
        private synchronized void shutdown() {
            if (zkServerThread != null) {
                // The shutdown method is protected...thus this hack to invoke it.
                // This will log an exception on shutdown; see
                // https://issues.apache.org/jira/browse/ZOOKEEPER-1873 for details.
                try {
                    Method shutdown = ZooKeeperServerMain.class.getDeclaredMethod("shutdown");
                    shutdown.setAccessible(true);
                    shutdown.invoke(zkServer);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                // It is expected that the thread will exit after
                // the server is shutdown; this will block until
                // the shutdown is complete.
                try {
                    zkServerThread.join(5000);
                    zkServerThread = null;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted while waiting for embedded ZooKeeper to exit");
                    // abandoning zk thread
                    zkServerThread = null;
                }
            }
        }
    }
}
