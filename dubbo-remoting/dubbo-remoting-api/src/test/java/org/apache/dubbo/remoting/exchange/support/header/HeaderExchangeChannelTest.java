package org.apache.dubbo.remoting.exchange.support.header;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ExchangeHandler;
import org.apache.dubbo.remoting.exchange.Request;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.List;

public class HeaderExchangeChannelTest {
    private MockChannel channel;
    private HeaderExchangeChannel headerExchangeChannel;
    private URL url = URL.valueOf("dubbo://localhost:20880");
    private static final String CHANNEL_KEY = HeaderExchangeChannel.class.getName() + ".CHANNEL";

    @Before
    public void setup() {
        channel = new MockChannel() {
            @Override
            public URL getUrl() {
                return url;
            }

            @Override
            public boolean isConnected() {
                return true;
            }
        };
        headerExchangeChannel = new HeaderExchangeChannel(channel);
    }


    @Test
    public void sendTest00() {
        boolean sent = true;
        String message = "this is a test message";
        try {
            headerExchangeChannel.close(1);
            headerExchangeChannel.send(message, sent);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof RemotingException);
        }
    }

    @Test
    public void sendTest01() throws RemotingException {
        boolean sent = true;
        String message = "this is a test message";
        headerExchangeChannel.send(message, sent);
        List<Object> objects = channel.getSentObjects();
        Assert.assertEquals(objects.get(0), "this is a test message");
    }

    @Test
    public void sendTest02() throws RemotingException {
        boolean sent = true;
        int message = 1;
        headerExchangeChannel.send(message, sent);
        List<Object> objects = channel.getSentObjects();
        Assert.assertEquals(objects.get(0).getClass(), Request.class);
        Request request = (Request) objects.get(0);
        Assert.assertEquals(request.getVersion(), "2.0.2");
    }

    @Test
    public void sendTest04() throws RemotingException {
        String message = "this is a test message";
        headerExchangeChannel.send(message);
        List<Object> objects = channel.getSentObjects();
        Assert.assertEquals(objects.get(0), "this is a test message");
    }

    @Test
    public void getOrAddChannelTest00() {
        channel.setAttribute("CHANNEL_KEY", "attribute");
        HeaderExchangeChannel ret = headerExchangeChannel.getOrAddChannel(channel);
        Assert.assertNotNull(ret);
    }

    @Test
    public void getOrAddChannelTest01() {
        Assert.assertNull(channel.getAttribute(CHANNEL_KEY));
        HeaderExchangeChannel ret = headerExchangeChannel.getOrAddChannel(channel);
        Assert.assertNotNull(ret);
        Assert.assertNotNull(channel.getAttribute(CHANNEL_KEY));
        Assert.assertEquals(channel.getAttribute(CHANNEL_KEY).getClass(), HeaderExchangeChannel.class);
    }

    @Test
    public void getOrAddChannelTest02() {
        Channel channel1 = null;
        HeaderExchangeChannel ret = headerExchangeChannel.getOrAddChannel(channel1);
        Assert.assertNull(ret);
    }


    @Test
    public void closeTest() {
        headerExchangeChannel.close();
        Assert.assertTrue(channel.isClosed());
    }

    @Test
    public void closeTimeoutTest() {
        headerExchangeChannel.close(100);
        Assert.assertTrue(channel.isClosed());
    }

    @Test
    public void isConnectedTest() {
        boolean ret = headerExchangeChannel.isConnected();
        Assert.assertTrue(ret);
    }

    @Test
    public void removeAttributeTest() {
        channel.setAttribute("test", "this is a test key");
        Assert.assertNotNull(channel.getAttribute("test"));
        headerExchangeChannel.removeAttribute("test");
        Assert.assertNull(channel.getAttribute("test"));
    }

    @Test
    public void removeChannelIfDisconnectedTest() {
        channel = new MockChannel() {
            @Override
            public URL getUrl() {
                return url;
            }

            @Override
            public boolean isConnected() {
                return false;
            }
        };
        channel.setAttribute(CHANNEL_KEY, "test");
        headerExchangeChannel.removeChannelIfDisconnected(channel);
        Assert.assertNull(channel.getAttribute(CHANNEL_KEY));
    }

    @Test
    public void requestTest00() {
        String message = "this is a test message";
        int timeout=1;
        try {
            headerExchangeChannel.close(1);
            headerExchangeChannel.request(message, timeout);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof RemotingException);
        }
    }

    @Test
    public void requestTest01() {
        String message = "this is a test message";
        int timeout = 1;
        try {
            headerExchangeChannel.request(message, timeout);
            List<Object> objects = channel.getSentObjects();
            Assert.assertEquals(objects.get(0).getClass(), Request.class);
            Request request = (Request) objects.get(0);
            Assert.assertEquals(request.getVersion(), "2.0.2");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof RemotingException);
        }
    }

    @Test
    public void getAttributeTest()  {
        String res1 = (String) headerExchangeChannel.getAttribute("test");
        Assert.assertNull(res1);
        headerExchangeChannel.setAttribute("test", "this is a test key");
        String res2 = (String) headerExchangeChannel.getAttribute("test");
        Assert.assertEquals(res2, "this is a test key");
    }

    @Test
    public void startCloseTest() {
        Assert.assertFalse(channel.isClosing());
        headerExchangeChannel.startClose();
        Assert.assertTrue(channel.isClosing());
    }

    @Test
    public void getLocalAddressTest() {
        InetSocketAddress address = headerExchangeChannel.getLocalAddress();
        Assert.assertNull(address);
    }

    @Test
    public void getRemoteAddressTest() {
        InetSocketAddress address = headerExchangeChannel.getRemoteAddress();
        Assert.assertNull(address);
    }

    @Test
    public void getChannelHandler() {
        ChannelHandler channelHandler = headerExchangeChannel.getChannelHandler();
        Assert.assertNull(channelHandler);
    }

    @Test
    public void ExchangeHandlerTest() {
        ExchangeHandler exchangeHandler = headerExchangeChannel.getExchangeHandler();
        Assert.assertNull(exchangeHandler);
    }

    @Test
    public void isClosed(){
        boolean result;
        result = headerExchangeChannel.isClosed();
        Assert.assertFalse(result);

        headerExchangeChannel.close(1);
        result = headerExchangeChannel.isClosed();
        Assert.assertTrue(result);

    }
    @Test
    public void getUrlTest() {
        URL res = headerExchangeChannel.getUrl();
        Assert.assertEquals(res, URL.valueOf("dubbo://localhost:20880"));
    }

    @Test
    public void setAttributeTest() {
        headerExchangeChannel.setAttribute("test", "this is a test message");
        String res = (String) headerExchangeChannel.getAttribute("test");
        Assert.assertEquals(res, "this is a test message");
    }

    @Test
    public void hasAttributeTest() {
        headerExchangeChannel.setAttribute("test", "this is a test message");
        boolean res = headerExchangeChannel.hasAttribute("test");
        Assert.assertTrue(res);

        res = headerExchangeChannel.hasAttribute("test1");
        Assert.assertFalse(res);
    }

    @Test
    public void hashCodeTest(){
        int result = headerExchangeChannel.hashCode();
        int num = 31 + channel.hashCode();
        Assert.assertEquals(result,num);
    }

    @Test
    public void equals(){
        boolean result;

        result = headerExchangeChannel.equals(headerExchangeChannel);
        Assert.assertTrue(result);

        result = headerExchangeChannel.equals(null);
        Assert.assertFalse(result);

        result = headerExchangeChannel.equals("test");
        Assert.assertFalse(result);

        HeaderExchangeChannel cmp = new HeaderExchangeChannel(channel);
        result = headerExchangeChannel.equals(cmp);
        Assert.assertTrue(result);

        MockChannel channelTest = new MockChannel() {
            @Override
            public URL getUrl() {
                return url;
            }

            @Override
            public boolean isConnected() {
                return false;
            }
        };
        HeaderExchangeChannel cmp1 = new HeaderExchangeChannel(channelTest);
        result = headerExchangeChannel.equals(cmp1);
        Assert.assertFalse(result);
    }
}

