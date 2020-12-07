package org.apache.dubbo.remoting.transport.netty4;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ConnectionSession {
    private static final Set<SessionHandler> sessions = ConcurrentHashMap.newKeySet();

    private ConnectionSession() {
    }

    public static void remove(SessionHandler handler) {
        sessions.remove(handler);
    }

    //public static Http2ClientSessionHandler get(Channel ch) {
    //    return ch.pipeline().get(Http2ClientSessionHandler.class);
    //}

    public static boolean isEmpty() {
        return sessions.isEmpty();
    }

    public static void add(SessionHandler sessionHandler) {
        sessions.add(sessionHandler);
    }

    public static void closeAllServerSession() {
        sessions.forEach(SessionHandler::close);
    }
}
