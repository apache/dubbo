package org.apache.dubbo.remoting.netty4;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;

public class NettyServerTest {

    public static void main(String[] args) throws RemotingException {
        URL url = new URL("transport", "localhost", 8898,
                new String[]{Constants.BIND_PORT_KEY, String.valueOf(8898)});

        final PortUnificationNettyServer server = new PortUnificationNettyServer(url, null);
        System.out.println(server.isBound());
    }
}
