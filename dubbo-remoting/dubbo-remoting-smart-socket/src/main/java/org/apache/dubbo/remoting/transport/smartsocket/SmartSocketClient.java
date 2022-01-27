package org.apache.dubbo.remoting.transport.smartsocket;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.transport.AbstractClient;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/1/25
 */
public class SmartSocketClient extends AbstractClient {
    private AioQuickClient client;
    private AioSession session;

    public SmartSocketClient(URL url, ChannelHandler handler) throws RemotingException {
        super(url, wrapChannelHandler(url, handler));
    }

    @Override
    protected void doOpen() throws Throwable {
        SmartSocketMessageProcessor processor = new SmartSocketMessageProcessor(getUrl(), this, getCodec());
        client = new AioQuickClient(getUrl().getHost(), getUrl().getPort(), new SmartSocketProtocol(getCodec()), processor);
    }

    @Override
    protected void doClose() throws Throwable {
//        client.shutdown();
    }

    @Override
    protected void doConnect() throws Throwable {
        session = client.start();
    }

    @Override
    protected void doDisConnect() throws Throwable {
//        client.shutdown();
    }

    @Override
    protected Channel getChannel() {
        if (session == null || session.isInvalid()) {
            return null;
        }
        return session.getAttachment();
    }
}
