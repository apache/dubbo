package org.apache.dubbo.remoting.api;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class SingleProtocolConnectionManagerTest {
    ConnectionManager singleProtocolConnectionManager = ExtensionLoader.getExtensionLoader(ConnectionManager.class).getExtension("single");

    @Test
    public void testConnectAndForeach() throws RemotingException {
        int port = NetUtils.getAvailablePort();

        URL url = new ServiceConfigURL("empty", "localhost", port,
            new String[]{Constants.BIND_PORT_KEY, String.valueOf(port)});

        PortUnificationServer server = new PortUnificationServer(url);
        server.bind();
        singleProtocolConnectionManager.connect(url);
        singleProtocolConnectionManager.forEachConnection(conn -> {
            Assertions.assertNotNull(conn);
            Assertions.assertDoesNotThrow(conn::connectSync);
            Assertions.assertTrue(conn.isAvailable());
        });
    }
}
