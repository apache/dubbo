package org.apache.dubbo.remoting;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture2;
import org.apache.dubbo.remoting.netty4.Connection;
import org.apache.dubbo.remoting.netty4.ConnectionHandler;
import org.apache.dubbo.remoting.netty4.NettyEventLoopFactory;
import org.apache.dubbo.remoting.netty4.WireProtocol;
import org.apache.dubbo.remoting.utils.UrlUtils;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_CLIENT_THREADPOOL;
import static org.apache.dubbo.common.constants.CommonConstants.LAZY_CONNECT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADPOOL_KEY;
import static org.apache.dubbo.remoting.netty4.NettyEventLoopFactory.socketChannelClass;

public class Client2 {
    public static final Timer TIMER = new HashedWheelTimer(
            new NamedThreadFactory("dubbo-network-timer", true), 30, TimeUnit.MILLISECONDS);
    private static final String CLIENT_THREAD_POOL_NAME = "DubboClientHandler";
    private static final Logger logger = LoggerFactory.getLogger(Client2.class);
    private final URL url;
    private final int connectTimeout;
    private final WireProtocol protocol;
    private final Lock connectLock = new ReentrantLock();
    private final ChannelGroup cg;
    private final boolean lazyConnect;
    protected ExecutorService executor;
    private Bootstrap bootstrap;
    private volatile Connection connection;

    public Client2(URL url) throws RemotingException {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        this.url = url;
        this.cg = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        this.connectTimeout = url.getPositiveParameter(Constants.CONNECT_TIMEOUT_KEY, Constants.DEFAULT_CONNECT_TIMEOUT);
        this.protocol = ExtensionLoader.getExtensionLoader(WireProtocol.class).getExtension(url.getProtocol());
        initExecutor(url);
        open();
        this.lazyConnect = url.getParameter(LAZY_CONNECT_KEY, false);
        if (!lazyConnect) {
            connectWithoutGuard();
        }
    }

    public void open() {
        bootstrap = new Bootstrap();
        bootstrap.group(NettyEventLoopFactory.NIO_EVENT_LOOP_GROUP)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .channel(socketChannelClass());

        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.max(3000, connectTimeout));
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                int heartbeatInterval = UrlUtils.getHeartbeat(getUrl());

                // TODO support SSL

                final ChannelPipeline p = ch.pipeline();//.addLast("logging",new LoggingHandler(LogLevel.INFO))//for debug
                p.addLast(new ConnectionHandler(bootstrap, cg, TIMER));
                p.addLast("client-idle-handler", new IdleStateHandler(heartbeatInterval, 0, 0, MILLISECONDS));
                // TODO support ssl
                protocol.configClientPipeline(p, null);
                // TODO support Socks5
            }
        });
    }

    private void concurrentConnect() throws RemotingException {
        if (connection != null) {
            return;
        }
        connectLock.lock();
        try {
            if (connection != null) {
                return;
            }
            connectWithoutGuard();
        } finally {
            connectLock.unlock();
        }
    }

    protected void connectWithoutGuard() throws RemotingException {
        long start = System.currentTimeMillis();
        final Future<Connection> connectFuture = connectAsync();
        connectFuture.addListener(future -> {
            if (future.isSuccess()) {
                Client2.this.connection = (Connection) future.get();
            }
        });
        connectFuture.awaitUninterruptibly(getConnectTimeout());
        if (!connectFuture.isSuccess()) {
            if (connectFuture.isDone()) {
                throw new RemotingException(null, null, "client(url: " + getUrl() + ") failed to connect to server .error message is:" + connectFuture.cause().getMessage(),
                        connectFuture.cause());
            } else {
                throw new RemotingException(null, null, "client(url: " + getUrl() + ") failed to connect to server. client-side timeout "
                        + getConnectTimeout() + "ms (elapsed: " + (System.currentTimeMillis() - start) + "ms) from netty client "
                        + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion());
            }
        }
    }

    public InetSocketAddress getConnectAddress() {
        return new InetSocketAddress(NetUtils.filterLocalHost(getUrl().getHost()), getUrl().getPort());
    }

    protected Future<Connection> connectAsync() {
        final Promise<Connection> promise = ImmediateEventExecutor.INSTANCE.newPromise();
        ChannelFuture future = bootstrap.connect(getConnectAddress());
        final ChannelFutureListener listener = new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (bootstrap.config().group().isShuttingDown()) {
                    promise.tryFailure(new IllegalStateException("Client is shutdown"));
                    return;
                }
                if (future.isSuccess()) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Succeed connect to server " + future.channel().remoteAddress() + " from " + getClass().getSimpleName() + " "
                                + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion()
                                + ", channel is " + future.channel());
                    }
                    bootstrap.config().group().execute(() -> promise.setSuccess(Connection.getConnectionFromChannel(future.channel())));
                } else {
                    bootstrap.config().group().execute(() -> {
                        final RemotingException cause = new RemotingException(future.channel(), "client(url: " + getUrl() + ") failed to connect to server "
                                + future.channel().remoteAddress() + ", error message is:" + future.cause().getMessage(), future.cause());
                        promise.tryFailure(cause);
                    });
                }
            }
        };
        future.addListener(listener);
        return promise;
    }

    /**
     * get url.
     *
     * @return url
     */
    public URL getUrl() {
        return url;
    }

    public CompletableFuture<Object> write(Object request, int timeout, ExecutorService executor) throws RemotingException {
        if (lazyConnect && connection == null) {
            concurrentConnect();
        }
        if (connection == null || !connection.isAvailable()) {
            throw new RemotingException(null, null, "Failed to send request " + request + ", cause: The channel " + this + " is closed!");
        }
        // create request.
        Request req = new Request();
        req.setVersion(Version.getProtocolVersion());
        req.setTwoWay(true);
        req.setData(request);
        DefaultFuture2 future = DefaultFuture2.newFuture(connection, req, timeout, executor);

        final ChannelFuture writeFuture = connection.write(req);
        writeFuture.addListener(future1 -> {
            if (future1.isSuccess()) {
                DefaultFuture2.sent(req);
            } else {
                Response response = new Response(req.getId(),req.getVersion());
                response.setStatus(Response.CHANNEL_INACTIVE);
                response.setErrorMessage(future1.cause().getMessage());
                DefaultFuture2.received(connection,response);
            }
        });
        return future;
    }

    private void initExecutor(URL url) {
        url = ExecutorUtil.setThreadName(url, CLIENT_THREAD_POOL_NAME)
                .addParameterIfAbsent(THREADPOOL_KEY, DEFAULT_CLIENT_THREADPOOL);
        ExecutorRepository executorRepository = ExtensionLoader.getExtensionLoader(ExecutorRepository.class).getDefaultExtension();

        executor = executorRepository.createExecutorIfAbsent(url);
    }

    private int getConnectTimeout() {
        return connectTimeout;
    }

    public boolean isActive() {
        if (connection != null && connection.isAvailable()) {
            return true;
        }
        connectAsync();
        return false;
    }

    public void close(int timeout) {
        cg.close();
    }
}
