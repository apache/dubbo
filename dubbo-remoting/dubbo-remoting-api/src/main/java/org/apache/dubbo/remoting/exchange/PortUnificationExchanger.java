package org.apache.dubbo.remoting.exchange;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.netty4.PortUnificationServer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PortUnificationExchanger {

    private static final ConcurrentMap<String, PortUnificationServer> servers = new ConcurrentHashMap<>();

    public static void bind(URL url) {
        servers.computeIfAbsent(url.getAddress(), addr -> {
            final PortUnificationServer server = new PortUnificationServer(url);
            server.bind();
            return server;
        });
    }

    public static void close() {
        for (PortUnificationServer server : servers.values()) {
            try {
                server.close();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }
}
