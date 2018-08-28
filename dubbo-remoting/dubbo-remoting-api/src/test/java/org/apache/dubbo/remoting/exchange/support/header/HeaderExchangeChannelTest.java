package org.apache.dubbo.remoting.exchange.support.header;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.ResponseFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.util.List;

import static org.junit.Assert.*;

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
        Assert.assertEquals(true, channel.getClosed());
    }

    @Test
    public void close1() {
        //todo 如何验证延迟关闭
        headerExchangeChannel.close(3000);
        boolean channelStatue = headerExchangeChannel.isClosed();
        Assert.assertEquals(false, channelStatue);
    }

    @Test
    public void startClose() {
        //todo 关闭函数的运行
        Channel a = Mockito.mock(MockChannel.class);
        a.close();
        //headerExchangeChannel=Mockito.mock(MockChannel.class);

        headerExchangeChannel.startClose();
    }

    @Test
    public void getLocalAddress() {
        //HeaderExchangeChannel headerExchangeChannel=new MockChannel();

        InetSocketAddress channelAddress = headerExchangeChannel.getLocalAddress();
        // System.out.print(channelAddress.getHostString()+" "+channelAddress.getPort());
        //  Assert.assertArrayEquals(,inetSocketAddress.getHostName(),inetSocketAddress.getPort());
    }

    @Test
    public void getRemoteAddress() {
        headerExchangeChannel.getRemoteAddress();
    }

    @Test
    public void getUrl() {
        URL channelUrl = headerExchangeChannel.getUrl();
        Assert.assertEquals(url, channelUrl);
    }

    @Test
    public void isConnected() {
        boolean channelConnnected = headerExchangeChannel.isConnected();
    }

    @Test
    public void getChannelHandler() {
        headerExchangeChannel.getChannelHandler();
    }

    @Test
    public void getExchangeHandler() {
        headerExchangeChannel.getExchangeHandler();
    }

    @Test
    public void getAttribute() {

        headerExchangeChannel.getAttribute("key");
    }

    @Test
    public void setAttribute() {
        headerExchangeChannel.setAttribute("key", "value");
    }

    @Test
    public void removeAttribute() {
        headerExchangeChannel.removeAttribute("key");
    }

    @Test
    public void hasAttribute() {
        boolean hasAttribute = headerExchangeChannel.hasAttribute("key");
    }
}