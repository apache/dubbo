package org.apache.dubbo.remoting.transport.netty4;

import io.netty.channel.ChannelHandler;

public interface SessionHandler extends ChannelHandler {

    //void notifyGoAway(Status status);

    void close();

}
