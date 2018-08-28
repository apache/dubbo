package org.apache.dubbo.remoting.exchange.support.header;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ExchangeHandler;
import org.apache.dubbo.remoting.exchange.Request;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.util.List;
/**
 * @author buyi.sl
 * @date 2018/8/27 下午7:19
 */
public class HeaderExchangeChannelTest {

    private URL url = URL.valueOf("dubbo://localhost:20880");

    private MockChannel channel;
    private HeaderExchangeChannel headerExchangeChannel;

    @Before
    public void setup() {
        channel = new MockChannel() {

            @Override
            public URL getUrl() {
                return url;
            }
        };
        headerExchangeChannel = new HeaderExchangeChannel(channel);
    }

    @Test
    public void send() throws RemotingException {
        Object message = Mockito.mock(Object.class);
        headerExchangeChannel.send(message);
        List<Object> objects = channel.getSentObjects();
        Assert.assertTrue(objects.size() > 0);
        Object obj = objects.get(0);
        Assert.assertTrue(obj instanceof Request);
        Request request = (Request) obj;
        Assert.assertEquals(message, request.getData());

    }

    @Test
    public void sendWithSent() throws RemotingException {
        Object message = Mockito.mock(Object.class);
        headerExchangeChannel.send(message,true);
        List<Object> objects = channel.getSentObjects();
        Assert.assertTrue(objects.size() > 0);
        Object obj = objects.get(0);
        Assert.assertTrue(obj instanceof Request);
        Request request = (Request) obj;
        Assert.assertEquals(message, request.getData());
    }

    @Test
    public void request() throws RemotingException {
        Object message = Mockito.mock(Object.class);
        headerExchangeChannel.request(message);
        List<Object> objects = channel.getSentObjects();
        Assert.assertTrue(objects.size() > 0);
        Object obj = objects.get(0);
        Assert.assertTrue(obj instanceof Request);
        Request request = (Request) obj;
        Assert.assertEquals(message, request.getData());
    }

    @Test
    public void requestWithTimeout() throws RemotingException {
        Object message = Mockito.mock(Object.class);
        Number timeout  = Mockito.mock(Number.class);
        Mockito.when(timeout.intValue()).thenReturn(1000);
        headerExchangeChannel.request(message, timeout.intValue());
        List<Object> objects = channel.getSentObjects();
        Assert.assertTrue(objects.size() > 0);
        Object obj = objects.get(0);
        Assert.assertTrue(obj instanceof Request);
        Request request = (Request) obj;
        Assert.assertEquals(message, request.getData());
    }

    @Test
    public void isClosed() {
        boolean channelStatue = headerExchangeChannel.isClosed();
        Assert.assertEquals(false, channelStatue);

    }

    @Test
    public void close() {
        headerExchangeChannel.close();
        Assert.assertEquals(true, channel.isClosed());
    }

    @Test
    public void closeWithTimeout() throws InterruptedException {
        Number timeout  = Mockito.mock(Number.class);
        Mockito.when(timeout.intValue()).thenReturn(1000);
        Assert.assertEquals(false, channel.isClosed());
        new Thread( () -> headerExchangeChannel.close(timeout.intValue()) ).start();
        Thread.sleep(10);
        Assert.assertEquals(false, channel.isClosed());
        Thread.sleep(990);
        Assert.assertEquals(true, channel.isClosed());
    }

    @Test
    public void startClose() {
        headerExchangeChannel.startClose();
        Assert.assertEquals(true, channel.isClosing());
    }

    @Test
    public void getLocalAddress() {
        InetSocketAddress inetSocketAddress = headerExchangeChannel.getLocalAddress();
        Assert.assertEquals(channel.getLocalAddress(),inetSocketAddress);
    }

    @Test
    public void getRemoteAddress() {
        InetSocketAddress inetSocketAddress = headerExchangeChannel.getRemoteAddress();
        Assert.assertEquals(channel.getLocalAddress(),inetSocketAddress);
    }

    @Test
    public void getUrl() {
        URL channelUrl = headerExchangeChannel.getUrl();
        Assert.assertEquals(url, channelUrl);
    }

    @Test
    public void isConnected() {
        boolean isConnected = headerExchangeChannel.isConnected();
        Assert.assertEquals(isConnected, channel.isConnected());
    }

    @Test
    public void getChannelHandler() {
        ChannelHandler channelHandler=headerExchangeChannel.getChannelHandler();
        Assert.assertEquals(null,channelHandler);
    }

    @Test
    public void getExchangeHandler() {
        ExchangeHandler exchangeHandler=headerExchangeChannel.getExchangeHandler();
        Assert.assertEquals(null,exchangeHandler);
    }

    @Test
    public void getAttribute() {
        Object attrBeforeSet=headerExchangeChannel.getAttribute("testKey");
        Assert.assertEquals(null,attrBeforeSet);
        headerExchangeChannel.setAttribute("testKey", "testValue");
        Object attrAfterSet=headerExchangeChannel.getAttribute("testKey");
        Assert.assertEquals("testValue",attrAfterSet);

    }

    @Test
    public void setAttribute() {
        headerExchangeChannel.setAttribute("testKey", "testValue");
        Object attrAfterSet=headerExchangeChannel.getAttribute("testKey");
        Assert.assertEquals("testValue",attrAfterSet);
    }

    @Test
    public void removeAttribute() {
        headerExchangeChannel.setAttribute("testKey", "testValue");
        Object attrAfterSet=headerExchangeChannel.getAttribute("testKey");
        Assert.assertEquals("testValue",attrAfterSet);
        headerExchangeChannel.removeAttribute("testKey");
        Object attrAfterRemove=headerExchangeChannel.getAttribute("testKey");
        Assert.assertEquals(null,attrAfterRemove);
    }

    @Test
    public void hasAttribute() {
        boolean attrBeforeSet = headerExchangeChannel.hasAttribute("testKey");
        Assert.assertEquals(false,attrBeforeSet);
        headerExchangeChannel.setAttribute("testKey", "testValue");
        Object attrAfterSet=headerExchangeChannel.getAttribute("testKey");
        Assert.assertEquals("testValue",attrAfterSet);
    }
}