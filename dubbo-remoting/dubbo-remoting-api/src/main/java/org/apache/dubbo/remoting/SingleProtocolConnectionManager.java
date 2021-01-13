package org.apache.dubbo.remoting;

import org.apache.dubbo.common.URL;

import io.netty.channel.group.ChannelGroup;
import io.netty.util.Timer;
import io.netty.util.concurrent.Future;

public class SingleProtocolConnectionManager implements ConnectionManager {
    @Override
    public Future<Channel> connect(URL url, ChannelGroup channels, Timer timer) {
        return null;
    }
}
