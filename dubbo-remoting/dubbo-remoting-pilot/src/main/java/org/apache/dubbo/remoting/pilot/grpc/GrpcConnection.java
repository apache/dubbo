package org.apache.dubbo.remoting.pilot.grpc;

import io.grpc.Channel;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.pilot.Constants;
import org.apache.dubbo.remoting.pilot.StateListener;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * grpc connection manager
 *
 * @author hzj
 * @date 2019/03/20
 */
public class GrpcConnection {

    private Logger logger = LoggerFactory.getLogger(GrpcConnection.class);

    private final URL url;
    private volatile boolean started = false;
    private volatile boolean closed = false;
    private volatile boolean connectState = false;
    private final AtomicReference<ManagedChannel> channel = new AtomicReference<>();
    private ScheduledFuture reconnectFuture;
    private final ScheduledExecutorService reconnectNotify;
    private long expirePeriod;
    private CompletableFuture<ManagedChannel> completableFuture;
    private ConnectionStateListener connectionStateListener;


    public GrpcConnection(URL url) {
        this.url = url;
        this.expirePeriod = url.getParameter(Constants.SESSION_TIMEOUT_KEY, Constants.DEFAULT_SESSION_TIMEOUT);
        if (expirePeriod <= 0) {
            this.expirePeriod = Constants.DEFAULT_SESSION_TIMEOUT;
        }
        this.completableFuture = CompletableFuture.supplyAsync(() -> prepareClient(url));
        this.reconnectNotify = Executors.newScheduledThreadPool(1,
                new NamedThreadFactory("pilot-reconnect-notify", true));
    }

    private ManagedChannel prepareClient(URL url) {
        int maxInboundMessageSize = DEFAULT_INBOUND_SIZE;
        if (StringUtils.isNotEmpty(System.getProperty(GRPC_MAX_INBOUD_SIZE_KEY))) {
            maxInboundMessageSize = Integer.valueOf(System.getProperty(GRPC_MAX_INBOUD_SIZE_KEY));
        }

        long keepAliveTime = DEFAULT_KEEPALIVE_TIME;
        if (StringUtils.isNotEmpty(System.getProperty(GRPC_KEEPALIVE_TIME_KEY))) {
            keepAliveTime = Integer.valueOf(System.getProperty(GRPC_KEEPALIVE_TIME_KEY));
        }
        ManagedChannel managedChannel = ManagedChannelBuilder
                .forTarget(url.getBackupAddress())
                .maxInboundMessageSize(maxInboundMessageSize)
                .enableFullStreamDecompression()
                .usePlaintext(true)
                .keepAliveTime(keepAliveTime, TimeUnit.MILLISECONDS)
                .keepAliveTimeout(DEFAULT_KEEPALIVE_TIMEOUT, TimeUnit.MILLISECONDS)
                .keepAliveWithoutCalls(true)
                .build();
        return managedChannel;
    }

    public void close() {
        if (closed) {
            return;
        }
        closed = true;

        try {
            if (started && reconnectFuture != null) {
                started = false;
                reconnectFuture.cancel(true);
                reconnectNotify.shutdownNow();
            }
        } catch (Exception e) {
            logger.warn("Failed to close grpc-reconnect-notify, url: " + url, e);
        }

        if (getChannel() != null) {
            getChannel().shutdown();
        }
    }

    public void start() {
        if (!started) {
            try {
                ManagedChannel channel = completableFuture.get(expirePeriod, TimeUnit.MILLISECONDS);
                this.setChannel(channel);
                this.connectState = isConnected();
                this.started = true;
            } catch (Throwable t) {
                logger.error("Failed to start pilot client in :[" + expirePeriod + "] milliseconds!, url: " + url, t);
                completableFuture.whenComplete((c, e) -> {
                    setChannel(c);
                    if (e != null) {
                        logger.error("Got an exception when trying to start pilot client, can not connect to pilot server, url: " + url, e);
                    }
                });
            }
        }
        try {
            this.reconnectFuture = reconnectNotify.scheduleWithFixedDelay(() -> {
                boolean connected = isConnected();
                if (connectState != connected) {
                    int notifyState = connected ? StateListener.CONNECTED : StateListener.DISCONNECTED;
                    if (connectionStateListener != null) {
                        logger.warn("Grpc connective state changed from [" + !connected + "] to [" + connected + "]");
                        connectionStateListener.stateChanged(getChannel(), notifyState);
                    }
                    connectState = connected;
                }
            }, 5 * Constants.DEFAULT_REGISTRY_RECONNECT_PERIOD, Constants.DEFAULT_REGISTRY_RECONNECT_PERIOD, TimeUnit.MILLISECONDS);
        } catch (Throwable t) {
            logger.error("Grpc monitor connect status failed", t);
        }
    }

    public boolean isConnected() {
        if (getChannel() == null) {
            return false;
        }
        return ConnectivityState.READY == (getChannel().getState(true))
                || ConnectivityState.IDLE == (getChannel().getState(true));
    }

    public ManagedChannel getChannel() {
        if (channel.get() == null || (channel.get().isShutdown() || channel.get().isTerminated())) {
            return null;
        }
        return this.channel.get();
    }

    public void setChannel(ManagedChannel channel) {
        this.channel.set(channel);
    }

    public interface ConnectionStateListener {
        /**
         * Called when there is a state change in the connection
         *
         * @param channel  the channel
         * @param newState the new state
         */
        void stateChanged(Channel channel, int newState);
    }

    public ConnectionStateListener getConnectionStateListener() {
        return connectionStateListener;
    }

    public void setConnectionStateListener(ConnectionStateListener connectionStateListener) {
        this.connectionStateListener = connectionStateListener;
    }

    public static final String GRPC_MAX_INBOUD_SIZE_KEY = "grpc.max.inbound.size";
    public static final String GRPC_KEEPALIVE_TIME_KEY = "grpc.keepalive.time";
    public static final int DEFAULT_INBOUND_SIZE = 100 * 1024 * 1024;
    public static final int DEFAULT_KEEPALIVE_TIME = 60 * 1000;
    public static final int DEFAULT_KEEPALIVE_TIMEOUT = 5 * 1000;
}
