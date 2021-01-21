package org.apache.dubbo.remoting.exchange;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.api.PortUnificationServer;

import io.netty.util.internal.PlatformDependent;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentMap;

public class PortUnificationExchanger {

    private static final Logger log = LoggerFactory.getLogger(PortUnificationExchanger.class);
    private static final ConcurrentMap<String, PortUnificationServer> servers = PlatformDependent.newConcurrentHashMap();

    public static void bind(URL url) {
        servers.computeIfAbsent(url.getAddress(), addr -> {
            final PortUnificationServer server = new PortUnificationServer(url);
            server.bind();
            return server;
        });
    }

    public static void close() {
        final ArrayList<PortUnificationServer> toClose = new ArrayList<>(servers.values());
        servers.clear();
        for (PortUnificationServer server : toClose) {
            try {
                server.close();
            } catch (Throwable throwable) {
                log.error("Close all port unification server failed", throwable);
            }
        }
    }
}
