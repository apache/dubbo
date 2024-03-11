package org.apache.dubbo.remoting.transport.netty4;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.remoting.api.connection.ConnectionManager;

import java.util.function.Consumer;

public class NettyHttp3ConnectionManager implements ConnectionManager {
    public static final String NAME = "netty4";

    @Override
    public AbstractConnectionClient connect(URL url, ChannelHandler handler) {
        try {
            return new NettyHttp3ConnectionClient(url, handler);
        } catch (RemotingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void forEachConnection(Consumer<AbstractConnectionClient> connectionConsumer) {
        // Do nothing.
    }
}
