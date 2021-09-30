/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.config.spring;

import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.ErrorHandler;
import org.springframework.util.SocketUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.UUID;

/**
 * from: https://github.com/spring-projects/spring-xd/blob/v1.3.1.RELEASE/spring-xd-dirt/src/main/java/org/springframework/xd/dirt/zookeeper/ZooKeeperUtils.java
 * <p>
 * Helper class to start an embedded instance of standalone (non clustered) ZooKeeper.
 * <p>
 * NOTE: at least an external standalone server (if not an ensemble) are recommended, even for
 * {@link org.springframework.xd.dirt.server.singlenode.SingleNodeApplication}
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 * @author David Turanski
 */
public class EmbeddedZooKeeper implements SmartLifecycle {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedZooKeeper.class);

    /**
     * ZooKeeper client port. This will be determined dynamically upon startup.
     */
    private final int clientPort;

    /**
     * Whether to auto-start. Default is true.
     */
    private boolean autoStartup = true;

    /**
     * Lifecycle phase. Default is 0.
     */
    private int phase = 0;

    /**
     * Thread for running the ZooKeeper server.
     */
    private volatile Thread zkServerThread;

    /**
     * ZooKeeper server.
     */
    private volatile ZooKeeperServerMain zkServer;

    /**
     * {@link ErrorHandler} to be invoked if an Exception is thrown from the ZooKeeper server thread.
     */
    private ErrorHandler errorHandler;

    private boolean daemon = true;

    /**
     * Construct an EmbeddedZooKeeper with a random port.
     */
    public EmbeddedZooKeeper() {
        clientPort = SocketUtils.findAvailableTcpPort();
    }

    /**
     * Construct an EmbeddedZooKeeper with the provided port.
     *
     * @param clientPort port for ZooKeeper server to bind to
     */
    public EmbeddedZooKeeper(int clientPort, boolean daemon) {
        this.clientPort = clientPort;
        this.daemon = daemon;
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
     * Specify whether to start automatically. Default is true.
     *
     * @param autoStartup whether to start automatically
     */
    public void setAutoStartup(boolean autoStartup) {
        this.autoStartup = autoStartup;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAutoStartup() {
        return this.autoStartup;
    }

    /**
     * Specify the lifecycle phase for the embedded server.
     *
     * @param phase the lifecycle phase
     */
    public void setPhase(int phase) {
        this.phase = phase;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPhase() {
        return this.phase;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRunning() {
        return (zkServerThread != null);
    }

    /**
     * Start the ZooKeeper server in a background thread.
     * <p>
     * Register an error handler via {@link #setErrorHandler} in order to handle
     * any exceptions thrown during startup or execution.
     */
    @Override
    public synchronized void start() {
        if (zkServerThread == null) {
            zkServerThread = new Thread(new ServerRunnable(), "ZooKeeper Server Starter");
            zkServerThread.setDaemon(daemon);
            zkServerThread.start();
        }
    }

    /**
     * Shutdown the ZooKeeper server.
     */
    @Override
    public synchronized void stop() {
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

    /**
     * Stop the server if running and invoke the callback when complete.
     */
    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    /**
     * Provide an {@link ErrorHandler} to be invoked if an Exception is thrown from the ZooKeeper server thread. If none
     * is provided, only error-level logging will occur.
     *
     * @param errorHandler the {@link ErrorHandler} to be invoked
     */
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Runnable implementation that starts the ZooKeeper server.
     */
    private class ServerRunnable implements Runnable {

        @Override
        public void run() {
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
                if (errorHandler != null) {
                    errorHandler.handleError(e);
                } else {
                    logger.error("Exception running embedded ZooKeeper", e);
                }
            }
        }
    }

}
