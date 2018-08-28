package org.apache.dubbo.remoting.exchange.support.header;


import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.*;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class HeaderExchangeChannelTest {

    private MockChannel channel;
    private HeaderExchangeChannel exchangeChannel;
    static String CHANNEL_KEY = HeaderExchangeChannel.class.getName() + ".CHANNEL";

    @Before
    public void setup() {
        this.channel = new MockChannel();
        this.exchangeChannel = new HeaderExchangeChannel(this.channel);
    }

    @Test
    public void testRequest() throws RemotingException {
        String req = "req str";
        ResponseFuture future =  this.exchangeChannel.request(req, 3442);
        List<Object> sentObjects = this.channel.getSentObjects();
        future.setCallback(new ResponseCallback() {
            @Override
            public void done(Object response) {
                Assert.assertTrue(sentObjects.contains(response));
            }

            @Override
            public void caught(Throwable exception) {
                throw new BizException();
            }
        });
    }

    private class BizException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    @Test(expected = RemotingException.class)
    public void testRequestErr() throws RemotingException {
        channel = new MockChannel() {
            @Override
            public void send(Object message) throws RemotingException {
                throw new RemotingException(new MockChannel(), "test");
            }
        };

        exchangeChannel = new HeaderExchangeChannel(channel);
        exchangeChannel.request("test request", 1000);
    }

    @Test
    public void testRequestAfterClose() throws RemotingException {
        exchangeChannel.close();
        exchangeChannel.request("test", 100);
    }

    @Test(expected = RemotingException.class)
    public void testSendAfterClose() throws RemotingException {
        exchangeChannel.close(0);
        exchangeChannel.send("test");
    }

    @Test(expected = RemotingException.class)
    public void testSendErr() throws RemotingException {
        channel = new MockChannel() {
            @Override
            public void send(Object message) throws RemotingException {
                throw new RemotingException(new MockChannel(), "test");
            }

            @Override
            public void send(Object message, boolean sent) throws RemotingException {
                throw new RemotingException(new MockChannel(), "test");
            }
        };

        exchangeChannel = new HeaderExchangeChannel(channel);
        exchangeChannel.send("test");
    }

    @Test
    public void testSendRequest() throws RemotingException {
        String requestdata = "test data";
        final Request request = new Request();
        request.setTwoWay(false);
        request.setData(requestdata);

        final AtomicInteger count = new AtomicInteger(0);
        final Channel mchannel = new MockChannel() {
            @Override
            public void send(Object message, boolean sent) {
            Request res = (Request) message;
            Assert.assertEquals(request, res);
            count.incrementAndGet();
            }
        };

        HeaderExchangeChannel exchangeChannel = new HeaderExchangeChannel(mchannel);
        exchangeChannel.send(request);
        Assert.assertEquals(1, count.get());
    }

    @Test
    public void testClose()  {
        this.exchangeChannel.close();
        Assert.assertTrue(this.channel.isClosed());
    }

    @Test
    public void testCloseWithTimeOut() throws RemotingException {
        MockChannel channel = new MockChannel();
        HeaderExchangeChannel exchangeChannel = new HeaderExchangeChannel(channel);

        exchangeChannel.request("test data", 1000);

        List<Object> sentObjects = channel.getSentObjects();
        Request request = (Request) sentObjects.get(0);
        Response response = new Response();
        response.setId(request.getId());
        DefaultFuture.received(channel, response);

        Assert.assertTrue(!DefaultFuture.hasFuture(channel));

        exchangeChannel.close(500);

        Assert.assertTrue(exchangeChannel.isClosed());
    }

    @Test
    public void testRemoveChannelIfDisconnected_channelClosed(){
        MockChannel channel = new MockChannel();
        HeaderExchangeChannel exchangeChannel = new HeaderExchangeChannel(channel);
        exchangeChannel.close();
        HeaderExchangeChannel.removeChannelIfDisconnected(exchangeChannel);
    }

    @Test
    public void testRemoveChannelIfDisconnected_channelIsNull(){
        HeaderExchangeChannel exchangeChannel = null;
        HeaderExchangeChannel.removeChannelIfDisconnected(exchangeChannel);
    }

    @Test
    public void testIsConnected(){
        MockChannel channel = new MockChannel(){
            @Override
            public boolean isConnected() {
                return true;
            }
        };
        HeaderExchangeChannel exchangeChannel = new HeaderExchangeChannel(channel);
        Assert.assertTrue(exchangeChannel.isConnected());
    }

    @Test
    public void testIsConnected_channelClosed(){
        MockChannel channel = new MockChannel(){
            @Override
            public boolean isConnected() {
                if (isClosed()){
                    return false;
                }else {
                    return true;
                }
            }
        };
        HeaderExchangeChannel exchangeChannel = new HeaderExchangeChannel(channel);
        exchangeChannel.close();
    }

    @Test
    public void testGetAttribute(){
        this.exchangeChannel.setAttribute("ss", "ss");
        Assert.assertEquals(this.exchangeChannel.getAttribute("ss"),"ss");
    }

    @Test
    public void testGetAttributeNotExist(){
        Assert.assertNull(this.exchangeChannel.getAttribute("ss"));
    }

    @Test
    public void testGetOrAddChannel() {
        MockChannel channel = new MockChannel(){
            @Override
            public boolean isConnected() {
                return true;
            }
        };

        HeaderExchangeChannel ret = exchangeChannel.getOrAddChannel(channel);

        Assert.assertTrue(channel.getAttribute(CHANNEL_KEY) != null);
        Assert.assertTrue(ret == channel.getAttribute(CHANNEL_KEY));
    }

    @Test
    public void testGetOrAddChannelDisconnected() {
        MockChannel channel = new MockChannel();
        HeaderExchangeChannel ret = exchangeChannel.getOrAddChannel(channel);

        Assert.assertTrue(channel.getAttribute(CHANNEL_KEY) == null);
        Assert.assertTrue(ret != null);
    }

    @Test
    public void testGetOrAddChannelAlreadyHas() {
        MockChannel channel = new MockChannel();
        HeaderExchangeChannel exchangeChannel = new HeaderExchangeChannel(channel);
        channel.setAttribute(CHANNEL_KEY, exchangeChannel);

        HeaderExchangeChannel ret = exchangeChannel.getOrAddChannel(channel);

        Assert.assertTrue(ret == channel.getAttribute(CHANNEL_KEY));
    }

    @Test
    public void testRemoveAttribute() {
        MockChannel channel = new MockChannel();
        HeaderExchangeChannel exchangeChannel = new HeaderExchangeChannel(channel);
        channel.setAttribute(CHANNEL_KEY,  new Object());

        Assert.assertTrue(channel.getAttribute(CHANNEL_KEY) != null);

        exchangeChannel.removeAttribute(CHANNEL_KEY);

        Assert.assertTrue(channel.getAttribute(CHANNEL_KEY) == null);
    }
}
