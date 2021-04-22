package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.URL;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UnaryServerStreamTest {

    @Test
    @SuppressWarnings("all")
    public void testInit() {
        URL url = new URL("test", "1.2.3.4", 8080);
        final UnaryServerStream stream = (UnaryServerStream)UnaryServerStream.unary(url);
        final TransportObserver observer = stream.asTransportObserver();

    }
}