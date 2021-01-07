package org.apache.dubbo.remoting;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture2;
import org.apache.dubbo.remoting.netty4.NettyEventLoopFactory;
import org.apache.dubbo.remoting.netty4.WireProtocol;
import org.apache.dubbo.remoting.utils.UrlUtils;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_CLIENT_THREADPOOL;
import static org.apache.dubbo.common.constants.CommonConstants.THREADPOOL_KEY;
import static org.apache.dubbo.remoting.netty4.NettyEventLoopFactory.socketChannelClass;

public class Client2 {
    private static final String CLIENT_THREAD_POOL_NAME = "DubboClientHandler";
    private static final Logger logger = LoggerFactory.getLogger(Client2.class);

    private final ChannelStatus status;
    private final URL url;
    private final int connectTimeout;
    private final WireProtocol protocol;
    private final Lock connectLock = new ReentrantLock();
    protected ExecutorService executor;
    private Bootstrap bootstrap;
    private volatile Channel channel;


    public Client2(URL url) throws RemotingException {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        this.url = url;
        this.status = new ChannelStatus();
        this.connectTimeout = url.getPositiveParameter(Constants.CONNECT_TIMEOUT_KEY, Constants.DEFAULT_CONNECT_TIMEOUT);
        this.protocol = ExtensionLoader.getExtensionLoader(WireProtocol.class).getExtension(url.getProtocol());
        initExecutor(url);
        open();
        connect();
    }

    public Channel getChannel() {
        return channel;
    }

    public void open() {
        bootstrap = new Bootstrap();
        bootstrap.group(NettyEventLoopFactory.NIO_EVENT_LOOP_GROUP)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                //.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getTimeout())
                .channel(socketChannelClass());

        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.max(3000, connectTimeout));
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                int heartbeatInterval = UrlUtils.getHeartbeat(getUrl());

                // TODO support SSL

                final ChannelPipeline p = ch.pipeline();//.addLast("logging",new LoggingHandler(LogLevel.INFO))//for debug
                p.addLast("client-idle-handler", new IdleStateHandler(heartbeatInterval, 0, 0, MILLISECONDS));
                // TODO support ssl
                protocol.configClientPipeline(p, null);
                // TODO support Socks5
            }
        });
    }

    protected void connect() throws RemotingException {

        connectLock.lock();

        try {

            if (status.isConnected()) {
                return;
            }

            doConnect();

            if (!status.isConnected()) {
                throw new RemotingException(this.channel, "Failed connect to server " + getRemoteAddress() + " from " + getClass().getSimpleName() + " "
                        + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion()
                        + ", cause: Connect wait timeout: " + getConnectTimeout() + "ms.");

            } else {
                if (logger.isInfoEnabled()) {
                    logger.info("Successed connect to server " + getRemoteAddress() + " from " + getClass().getSimpleName() + " "
                            + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion()
                            + ", channel is " + this.getChannel());
                }
            }

        } catch (RemotingException e) {
            throw e;

        } catch (Throwable e) {
            throw new RemotingException(this.channel, "Failed connect to server " + getRemoteAddress() + " from " + getClass().getSimpleName() + " "
                    + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion()
                    + ", cause: " + e.getMessage(), e);

        } finally {
            connectLock.unlock();
        }
    }

    public InetSocketAddress getConnectAddress() {
        return new InetSocketAddress(NetUtils.filterLocalHost(getUrl().getHost()), getUrl().getPort());
    }

    protected void doConnect() throws Throwable {
        long start = System.currentTimeMillis();
        ChannelFuture future = bootstrap.connect(getConnectAddress());
        try {
            boolean ret = future.awaitUninterruptibly(getConnectTimeout(), MILLISECONDS);

            if (ret && future.isSuccess()) {
                Channel newChannel = future.channel();
                try {
                    // Close old channel
                    // copy reference
                    Channel oldChannel = Client2.this.channel;
                    if (oldChannel != null) {
                        try {
                            if (logger.isInfoEnabled()) {
                                logger.info("Close old netty channel " + oldChannel + " on create new netty channel " + newChannel);
                            }
                            oldChannel.close();
                        } finally {
                            //TODO fill me
//                            NettyChannel.removeChannelIfDisconnected(oldChannel);
                        }
                    }
                } finally {
                    if (Client2.this.status.isClosed()) {
                        try {
                            if (logger.isInfoEnabled()) {
                                logger.info("Close new netty channel " + newChannel + ", because the client closed.");
                            }
                            newChannel.close();
                        } finally {
//                            NettyClient.this.channel = null;
                            //TODO fill me
//                            NettyChannel.removeChannelIfDisconnected(newChannel);
                        }
                    } else {
                        Client2.this.channel = newChannel;
                        status.connected();
                    }
                }
            } else if (future.cause() != null) {
                throw new RemotingException(this.channel, "client(url: " + getUrl() + ") failed to connect to server "
                        + getRemoteAddress() + ", error message is:" + future.cause().getMessage(), future.cause());
            } else {
                throw new RemotingException(this.channel, "client(url: " + getUrl() + ") failed to connect to server "
                        + getRemoteAddress() + " client-side timeout "
                        + getConnectTimeout() + "ms (elapsed: " + (System.currentTimeMillis() - start) + "ms) from netty client "
                        + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion());
            }
        } finally {
            // just add new valid channel to NettyChannel's cache
            if (!status.isConnected()) {
                //future.cancel(true);
            }
        }
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
        if (status.isClosed()) {
            throw new RemotingException((InetSocketAddress) getChannel().localAddress(), null, "Failed to send request " + request + ", cause: The channel " + this + " is closed!");
        }
        // create request.
        Request req = new Request();
        req.setVersion(Version.getProtocolVersion());
        req.setTwoWay(true);
        req.setData(request);
        DefaultFuture2 future = DefaultFuture2.newFuture(this, req, timeout, executor);

        final ChannelFuture writeFuture = getChannel().writeAndFlush(req);
        writeFuture.addListener(future1 -> {
            if (future1.isSuccess()) {
                DefaultFuture2.sent(req);
            } else {
                future.cancel();
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

    private SocketAddress getRemoteAddress() {
        return channel.remoteAddress();
    }

    private SocketAddress getLocalAddress() {
        return channel.localAddress();
    }

    private int getConnectTimeout() {
        return connectTimeout;
    }

    public boolean isActive() {
        return channel != null && channel.isActive();
    }

    public void close(int timeout) {

    }
}
