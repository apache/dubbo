package com.alibaba.dubbo.remoting.transport.netty;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class ThreadNameTest {

    private NettyServer server;
    private NettyClient client;

    private URL serverURL;
    private URL clientURL;

    private ThreadNameVerifyHandler serverHandler;
    private ThreadNameVerifyHandler clientHandler;

    @Before
    public void before() throws Exception {
        int port = 55555;
        serverURL = URL.valueOf("netty://localhost").setPort(port);
        clientURL = URL.valueOf("netty://localhost").setPort(port);

        serverHandler = new ThreadNameVerifyHandler(String.valueOf(port), false);
        clientHandler = new ThreadNameVerifyHandler(String.valueOf(port), true);

        server = new NettyServer(serverURL, serverHandler);
        client = new NettyClient(clientURL, clientHandler);
    }

    @After
    public void after() throws Exception {
        if (client != null) {
            client.close();
            client = null;
        }

        if (server != null) {
            server.close();
            server = null;
        }
    }

    @Test
    public void testThreadName() throws Exception {
        client.send("hello");
        Thread.sleep(1000L * 5L);
        if (!serverHandler.isSuccess() || !clientHandler.isSuccess()) {
            Assert.fail();
        }
    }

    class ThreadNameVerifyHandler implements ChannelHandler {

        private String message;
        private boolean success;
        private boolean client;

        public boolean isSuccess() {
            return success;
        }

        ThreadNameVerifyHandler(String msg, boolean client) {
            message = msg;
            this.client = client;
        }

        private void checkThreadName() {
            if (!success) {
                success = Thread.currentThread().getName().contains(message);
            }
        }

        private void output(String method) {
            System.out.println(Thread.currentThread().getName()
                                   + " " + (client ? "client " + method : "server " + method));
        }

        @Override
        public void connected(Channel channel) throws RemotingException {
            output("connected");
            checkThreadName();
        }

        @Override
        public void disconnected(Channel channel) throws RemotingException {
            output("disconnected");
            checkThreadName();
        }

        @Override
        public void sent(Channel channel, Object message) throws RemotingException {
            output("sent");
            checkThreadName();
        }

        @Override
        public void received(Channel channel, Object message) throws RemotingException {
            output("received");
            checkThreadName();
        }

        @Override
        public void caught(Channel channel, Throwable exception) throws RemotingException {
            output("caught");
            checkThreadName();
        }
    }

}
