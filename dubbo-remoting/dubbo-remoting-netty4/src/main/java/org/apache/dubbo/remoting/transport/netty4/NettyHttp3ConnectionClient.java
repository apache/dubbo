package org.apache.dubbo.remoting.transport.netty4;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3ClientConnectionHandler;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicChannelBootstrap;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.netty.util.NetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_RECONNECT;

public class NettyHttp3ConnectionClient extends NettyConnectionClient {
    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(NettyHttp3ConnectionClient.class);

    private AtomicReference<io.netty.channel.Channel> datagramChannel;
    private QuicConnectionListener quicConnectionListener;
    private QuicChannelBootstrap quicBootstrap;

    protected NettyHttp3ConnectionClient(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
    }

    @Override
    public String getConnectionType() {
        return "http3";
    }

    @Override
    protected void doOpen() throws Throwable {
        initConnectionClient();
        initQuicChannel();
    }

    @Override
    protected void initConnectionClient() {
        super.initConnectionClient();
        this.quicConnectionListener = new QuicConnectionListener();
        this.datagramChannel = new AtomicReference<>();
    }

    private void initQuicChannel() {
        QuicSslContext context = QuicSslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .applicationProtocols(Http3.supportedApplicationProtocols()).build();
        io.netty.channel.ChannelHandler codec = Http3.newQuicClientCodecBuilder()
                .sslContext(context)
                .maxIdleTimeout(30, TimeUnit.MINUTES)
                .initialMaxData(10000000)
                .initialMaxStreamDataBidirectionalLocal(1000000)
                .build();

        NioEventLoopGroup group = new NioEventLoopGroup(1);
        Bootstrap bootstrap = new Bootstrap();
        io.netty.channel.Channel datagramCh = null;
        try {
            datagramCh = bootstrap.group(group)
                    .channel(NioDatagramChannel.class)
                    .handler(new ChannelInitializer<NioDatagramChannel>() {
                        @Override
                        protected void initChannel(NioDatagramChannel ch) throws Exception {
                            NettyChannel nettyChannel = NettyChannel.getOrAddChannel(ch, getUrl(), getChannelHandler());

                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(codec);
                        }
                    })
                    .bind(0).sync().channel();
        } catch (InterruptedException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(e.getMessage());
            }
            return;
        }
        datagramChannel.set(datagramCh);
        // set null but do not close this client, it will be reconnect in the future
        datagramCh.closeFuture().addListener(channelFuture -> datagramChannel.set(null));

        quicBootstrap = QuicChannel.newBootstrap(datagramCh)
                .handler(new ChannelInitializer<QuicChannel>() {
                    @Override
                    protected void initChannel(QuicChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();

                        pipeline.addLast(new NettyHttp3ConnectionHandler(NettyHttp3ConnectionClient.this));
                        pipeline.addLast(new Http3ClientConnectionHandler());

                        ch.closeFuture().addListener(channelFuture -> channel.set(null));
                    }
                })
                .remoteAddress(getConnectAddress());
    }

    @Override
    protected void doConnect() throws RemotingException {
        if (isClosed()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        String.format("%s aborted to reconnect cause connection closed. ", NettyHttp3ConnectionClient.this));
            }
        }
        long start = System.currentTimeMillis();
        createConnectingPromise();

        Future<QuicChannel> promise = quicBootstrap.connect();

        promise.addListener(quicConnectionListener);
        waitTimeoutAndHandleFailure(promise, start);
    }

    @Override
    protected void doClose() {
        // AbstractPeer close can set closed true.
        if (isClosed()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Connection:%s freed ", this));
            }

            final QuicChannel currentQuic = (QuicChannel) channel.get();
            if (currentQuic != null) {
                try {
                    currentQuic.close().sync();
                } catch (InterruptedException e) {
                    if (LOGGER.isErrorEnabled()) {
                        LOGGER.error(e.getMessage());
                    }
                    return;
                }
            }
            this.channel.set(null);

            final io.netty.channel.Channel currentDatagram = datagramChannel.get();
            if (currentDatagram != null) {
                currentDatagram.close();
            }
            this.datagramChannel.set(null);

            closePromise.setSuccess(null);
        }
    }

    @Override
    protected Channel getChannel() {
        io.netty.channel.Channel c = getNettyChannel();
        if (c == null) {
            return null;
        }
        return NettyChannel.getOrAddChannel(new QuicToNettyChannelAdapter(c), getUrl(), this);
    }

    class QuicConnectionListener implements GenericFutureListener<Future<QuicChannel>> {
        @Override
        public void operationComplete(Future<QuicChannel> future) throws Exception {
            if (future.isSuccess()) {
                return;
            }
            final NettyHttp3ConnectionClient connectionClient = NettyHttp3ConnectionClient.this;
            if (connectionClient.isClosed() || connectionClient.getCounter() == 0) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format(
                            "%s aborted to reconnect. %s",
                            connectionClient, future.cause().getMessage()));
                }
                return;
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format(
                        "%s is reconnecting, attempt=%d cause=%s",
                        connectionClient, 0, future.cause().getMessage()));
            }
            final EventLoop loop = future.get().eventLoop();
            loop.schedule(
                    () -> {
                        try {
                            connectionClient.doConnect();
                        } catch (RemotingException e) {
                            LOGGER.error(
                                    TRANSPORT_FAILED_RECONNECT,
                                    "",
                                    "",
                                    "Failed to connect to server: " + getConnectAddress());
                        }
                    },
                    1L,
                    TimeUnit.SECONDS);
        }
    }
}
