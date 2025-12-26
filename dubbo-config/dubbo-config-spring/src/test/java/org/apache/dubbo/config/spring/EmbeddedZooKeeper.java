package org.apache.dubbo.config.spring;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.SystemPropertyConfigUtils;
import org.apache.dubbo.test.common.utils.TestSocketUtils;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.ErrorHandler;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.UUID;

import static org.apache.dubbo.common.constants.CommonConstants.SystemProperty.SYSTEM_JAVA_IO_TMPDIR;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.TESTING_INIT_ZOOKEEPER_SERVER_ERROR;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.TESTING_REGISTRY_FAILED_TO_STOP_ZOOKEEPER;

public class EmbeddedZooKeeper implements SmartLifecycle {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(EmbeddedZooKeeper.class);

    // 1. MUST NOT BE FINAL
    private int clientPort;

    private boolean autoStartup = true;
    private int phase = 0;
    private volatile Thread zkServerThread;
    private volatile ZooKeeperServerMain zkServer;
    private ErrorHandler errorHandler;
    private boolean daemon = true;

    // 2. SIMPLEST CONSTRUCTOR
    public EmbeddedZooKeeper() {
        this.clientPort = 2181;
    }

    // 3. SECOND CONSTRUCTOR
    public EmbeddedZooKeeper(int clientPort, boolean daemon) {
        this.clientPort = clientPort;
        this.daemon = daemon;
    }

    public int getClientPort() {
        return this.clientPort;
    }

    @Override
    public boolean isAutoStartup() {
        return this.autoStartup;
    }

    public void setAutoStartup(boolean autoStartup) {
        this.autoStartup = autoStartup;
    }

    @Override
    public int getPhase() {
        return this.phase;
    }

    public void setPhase(int phase) {
        this.phase = phase;
    }

    @Override
    public boolean isRunning() {
        return (zkServerThread != null);
    }

    @Override
    public synchronized void start() {
        if (zkServerThread == null) {
            zkServerThread = new Thread(new ServerRunnable(), "ZooKeeper Server Starter");
            zkServerThread.setDaemon(daemon);
            zkServerThread.start();

            // NEW: Wait up to 10 seconds for the port to actually open
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 10000) {
                try (java.net.Socket s = new java.net.Socket("localhost", clientPort)) {
                    return; // Port is open, we can proceed!
                } catch (Exception e) {
                    try { Thread.sleep(200); } catch (InterruptedException ie) { break; }
                }
            }
        }
    }

    @Override
    public synchronized void stop() {
        if (zkServerThread != null) {
            try {
                if (zkServer != null) {
                    // Forcefully close the server
                    Method shutdown = ZooKeeperServerMain.class.getDeclaredMethod("shutdown");
                    shutdown.setAccessible(true);
                    shutdown.invoke(zkServer);
                }
            } catch (Exception e) {
                // Silently ignore shutdown errors
            }
            zkServerThread.interrupt(); // Kill the thread
            zkServerThread = null;
        }
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    private class ServerRunnable implements Runnable {
        @Override
        public void run() {
            try {
                Properties properties = new Properties();
                File file = new File(SystemPropertyConfigUtils.getSystemProperty(SYSTEM_JAVA_IO_TMPDIR)
                        + File.separator + "zkdata-" + UUID.randomUUID());
                file.mkdirs();

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
                }
            }
        }
    }
}
