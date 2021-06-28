package org.apache.dubbo.remoting.api;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class MultiplexProtocolConnectionManagerTest {
    private final ConnectionManager multiplexProtocolConnectionManager = ExtensionLoader.getExtensionLoader(
        ConnectionManager.class).getExtension("multiple");

    @Test
    public void testConnectAndForeach() throws RemotingException {
        int port1 = NetUtils.getAvailablePort();
        URL url1 = new ServiceConfigURL("empty", "localhost", port1,
            new String[]{Constants.BIND_PORT_KEY, String.valueOf(port1)});

        int port2 = NetUtils.getAvailablePort();
        URL url2 = new ServiceConfigURL("empty", "localhost", port2,
            new String[]{Constants.BIND_PORT_KEY, String.valueOf(port2)});

        new PortUnificationServer(url1).bind();
        new PortUnificationServer(url2).bind();

        multiplexProtocolConnectionManager.connect(url1);
        multiplexProtocolConnectionManager.connect(url2);

        multiplexProtocolConnectionManager.forEachConnection(conn -> {
            Assertions.assertNotNull(conn);
            Assertions.assertDoesNotThrow(conn::connectSync);
            Assertions.assertTrue(conn.isAvailable());
        });
    }
}
