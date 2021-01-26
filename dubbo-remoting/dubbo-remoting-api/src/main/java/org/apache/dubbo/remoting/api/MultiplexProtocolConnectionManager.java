package org.apache.dubbo.remoting.api;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.remoting.RemotingException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public class MultiplexProtocolConnectionManager implements ConnectionManager {
    private final ConcurrentMap<String, ConnectionManager> protocols = new ConcurrentHashMap<>();

    @Override
    public Connection connect(URL url) throws RemotingException {
        final ConnectionManager manager = protocols.computeIfAbsent(url.getProtocol(), this::createSingleProtocolConnectionManager);
        return manager.connect(url);
    }

    @Override
    public void forEachConnection(Consumer<Connection> connectionConsumer) {
        protocols.values().forEach(p -> p.forEachConnection(connectionConsumer));
    }

    private ConnectionManager createSingleProtocolConnectionManager(String protocol) {
        return ExtensionLoader.getExtensionLoader(ConnectionManager.class).getExtension("single");
    }
}
