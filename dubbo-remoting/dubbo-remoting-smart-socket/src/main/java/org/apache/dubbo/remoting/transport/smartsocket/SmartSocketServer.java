package org.apache.dubbo.remoting.transport.smartsocket;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.transport.AbstractServer;
import org.apache.dubbo.remoting.transport.dispatcher.ChannelHandlers;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.transport.AioQuickServer;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.util.ArrayList;
import java.util.Collection;

import static org.apache.dubbo.common.constants.CommonConstants.BACKLOG_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.IO_THREADS_KEY;
import static org.apache.dubbo.remoting.Constants.*;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/1/25
 */
public class SmartSocketServer extends AbstractServer {
    private AioQuickServer server;
    private SmartSocketMessageProcessor processor;
    private boolean bound;
    private BufferPagePool pagePool;

    public SmartSocketServer(URL url, ChannelHandler handler) throws RemotingException {
        super(ExecutorUtil.setThreadName(url, SERVER_THREAD_POOL_NAME), ChannelHandlers.wrap(handler, url));
    }

    @Override
    public boolean isBound() {
        return bound;
    }

    @Override
    public Collection<Channel> getChannels() {
        Collection<Channel> chs = new ArrayList<>(processor.getChannels().size());
        chs.addAll(processor.getChannels().values());
        return chs;
    }

    @Override
    public Channel getChannel(InetSocketAddress remoteAddress) {
        return processor.getChannels().get(NetUtils.toAddressString(remoteAddress));
    }

    @Override
    protected void doOpen() throws Throwable {
        int threadNum = getUrl().getPositiveParameter(IO_THREADS_KEY, Constants.DEFAULT_IO_THREADS);
        BufferPagePool pagePool = new BufferPagePool(11, 1, true);
        this.processor = new SmartSocketMessageProcessor(getUrl(), this, getCodec());
        int b = getUrl().getPositiveParameter(BUFFER_KEY, DEFAULT_BUFFER_SIZE);
        int bufferSize = b >= MIN_BUFFER_SIZE && b <= MAX_BUFFER_SIZE ? b : DEFAULT_BUFFER_SIZE;

        server = new AioQuickServer(getUrl().getPort(), new SmartSocketProtocol(getCodec()), processor);
        server.setReadBufferSize(bufferSize)
                .setThreadNum(threadNum)
                .setBufferPagePool(pagePool)
                .setBacklog(getUrl().getPositiveParameter(BACKLOG_KEY, Constants.DEFAULT_BACKLOG))
                .setOption(StandardSocketOptions.SO_REUSEADDR, true);
        server.start();
        bound = true;
    }

    @Override
    protected void doClose() throws Throwable {
        bound = false;
        pagePool.release();
        server.shutdown();
    }
}
