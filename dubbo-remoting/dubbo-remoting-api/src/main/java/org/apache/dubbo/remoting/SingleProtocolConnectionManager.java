package org.apache.dubbo.remoting;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.netty4.Connection;

import io.netty.util.internal.PlatformDependent;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public class SingleProtocolConnectionManager implements ConnectionManager {
    private final ConcurrentMap<String, Connection> connections = PlatformDependent.newConcurrentHashMap();

    @Override
    public Connection connect(URL url) throws RemotingException {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        final Connection connection = connections.compute(url.getAddress(), (address, conn) -> {
            if (conn == null) {
                final Connection created = new Connection(url);
                created.getCloseFuture().addListener(future -> connections.remove(address, created));
                return created;
            } else {
                conn.retain();
                return conn;
            }
        });
        connection.init();
        return connection;
    }

    @Override
    public void forEachConnection(Consumer<Connection> connectionConsumer) {
        connections.values().forEach(connectionConsumer);
    }
}
