package com.alibaba.dubbo.remoting.http;

import java.io.IOException;
import java.net.ServerSocket;

public class TestUtil {
    public static Integer getFreePort() throws IOException {
        ServerSocket socket = new ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        return port;
    }
}
