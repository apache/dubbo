package org.apache.dubbo.remoting;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;

import io.netty.channel.group.ChannelGroup;
import io.netty.util.Timer;
import io.netty.util.concurrent.Future;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MultiplexProtocolConnectionManager implements ConnectionManager {
    private final ConcurrentMap<String, ConnectionManager> protocols = new ConcurrentHashMap<>();

    @Override
    public Future<Channel> connect(URL url, ChannelGroup channels, Timer timer) {
        final ConnectionManager manager = protocols.computeIfAbsent(url.getProtocol(), this::createSingleProtocolConnectionManager);
        return manager.connect(url, channels, timer);
    }

    private ConnectionManager createSingleProtocolConnectionManager(String protocol) {
        return ExtensionLoader.getExtensionLoader(ConnectionManager.class).getExtension("single");
    }
}
