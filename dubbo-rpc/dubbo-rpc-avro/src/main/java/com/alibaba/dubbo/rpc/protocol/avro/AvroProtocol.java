package com.alibaba.dubbo.rpc.protocol.avro;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProtocol;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;
import org.apache.avro.ipc.NettyServer;
import org.apache.avro.ipc.NettyTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.DefaultChannelPipeline;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by wuyu on 2016/6/14.
 */
public class AvroProtocol extends AbstractProxyProtocol {

    public static final int DEFAULT_PORT = 30990;

    private Map<String, Server> serverMap = new ConcurrentHashMap<>();

    private Map<String, MultiplexedResponder> multiplexedResponderMap = new ConcurrentHashMap<>();

    private Map<String, NettyTransceiver> nettyTransceiverMap = new ConcurrentHashMap<>();

    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    protected <T> Runnable doExport(final T impl, final Class<T> type, URL url) throws RpcException {
        final String addr = url.getIp() + ":" + url.getPort();
        int threads = url.getPositiveParameter(Constants.THREADPOOL_KEY, 200);

        String bindHost = url.getHost();
        if (url.getParameter("anyhost", false)) {
            bindHost = "0.0.0.0";
        }

        if (serverMap.get(addr) == null) {
            ExecutorService boss = Executors.newCachedThreadPool(new NamedThreadFactory("NettyServerBoss", true));
            ExecutorService worker = Executors.newCachedThreadPool(new NamedThreadFactory("NettyServerWorker", true));
            ChannelFactory channelFactory = new NioServerSocketChannelFactory(boss, worker, threads);
            MultiplexedResponder multiplexedResponder = new MultiplexedResponder(type, impl);
            ChannelPipelineFactory pipelineFactory = new ChannelPipelineFactory() {
                @Override
                public ChannelPipeline getPipeline() throws Exception {
                    DefaultChannelPipeline pipeline = new DefaultChannelPipeline();
                    pipeline.addLast("responderHolderHandler", new ResponderHolderHandler());
                    return pipeline;
                }
            };
            NettyServer server = new NettyServer(multiplexedResponder, new InetSocketAddress(bindHost, url.getPort()), channelFactory, pipelineFactory, null);
            serverMap.put(addr, server);
            multiplexedResponderMap.put(addr, multiplexedResponder);
            server.start();
        } else {
            multiplexedResponderMap.get(addr).registerResponder(new SpecificResponder(type, impl));
        }


        return new Runnable() {
            @Override
            public void run() {
                MultiplexedResponder multiplexedResponder = multiplexedResponderMap.get(addr);
                if (multiplexedResponder != null) {
                    multiplexedResponder.unRegisterResponder(new SpecificResponder(type, impl));
                }
            }
        };
    }

    @Override
    protected <T> T doRefer(Class<T> type, URL url) throws RpcException {
        final String addr = url.getIp() + ":" + url.getPort();
        int timeout = url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        ExecutorService boss = Executors.newCachedThreadPool(new NamedThreadFactory("Avro-Boss-" + NettyTransceiver.class.getName(), true));
        ExecutorService worker = Executors.newCachedThreadPool(new NamedThreadFactory("Avro-Worker-" + NettyTransceiver.class.getName(), true));
        ChannelFactory channelFactory = new NioClientSocketChannelFactory(boss, worker, url.getParameter(Constants.THREADS_KEY, Constants.DEFAULT_THREADS), timeout);


        try {
            NettyTransceiver nettyTransceiver = new NettyTransceiver(new InetSocketAddress(url.getIp(), url.getPort()), channelFactory, (long) timeout);
            nettyTransceiverMap.put(addr, nettyTransceiver);
            return SpecificRequestor.getClient(type, nettyTransceiver);
        } catch (IOException e) {
            throw new RpcException(e);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        for (String key : nettyTransceiverMap.keySet()) {
            nettyTransceiverMap.remove(key).close();
        }
        for (String key : serverMap.keySet()) {
            multiplexedResponderMap.remove(key);
            Server server = serverMap.remove(key);
            server.close();
        }

    }
}
