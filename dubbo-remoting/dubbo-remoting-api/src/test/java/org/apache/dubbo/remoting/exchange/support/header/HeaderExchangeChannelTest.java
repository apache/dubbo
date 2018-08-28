package org.apache.dubbo.remoting.exchange.support.header;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.handler.MockedChannel;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * @author yisheng
 * @date 2018/08/27
 */
public class HeaderExchangeChannelTest {

    private URL url = URL.valueOf("dubbo://localhost:20880");

    private MockChannel channel;
    private HeaderExchangeChannel header;
    private static final String CHANNEL_KEY = HeaderExchangeChannel.class.getName() + ".CHANNEL";

    private MockChannel channelConnectedTrue;

    private HeaderExchangeChannel defaultChannel() {
        Channel channel = new MockedChannel();
        return new HeaderExchangeChannel(channel);
    }

    private void initHeader(){

        header = new HeaderExchangeChannel(channel);
    }

    private void initChannel(){
        channel = new MockChannel() {

            @Override
            public URL getUrl() {
                return url;
            }
        };


        channelConnectedTrue = new MockChannel(){
            @Override
            public URL getUrl(){
                return url;
            }

            @Override
            public boolean isConnected(){
                return true;
            }

        };
    }



    @Test(expected = IllegalArgumentException.class)
    public void headerExchangeChannelTest(){
        HeaderExchangeChannel test = new HeaderExchangeChannel(null);
    }

    @Test
    public void getOrAddChannel() {
        initChannel();
        HeaderExchangeChannel testChWhetherNull = HeaderExchangeChannel.getOrAddChannel(null);
        Assert.assertNull("params ch is null",testChWhetherNull);
        testChWhetherNull = HeaderExchangeChannel.getOrAddChannel(channelConnectedTrue);
        HeaderExchangeChannel testRetWhetherNull =  (HeaderExchangeChannel) channelConnectedTrue.getAttribute(CHANNEL_KEY);
        Assert.assertSame("ret is null",testChWhetherNull,testRetWhetherNull);
    }



    @Test
    public void removeChannelIfDisconnected() {
        initChannel();
        Assert.assertNull(channel.getAttribute(CHANNEL_KEY));
        initHeader();
        channel.setAttribute(CHANNEL_KEY, header);
        channel.close();
        HeaderExchangeChannel.removeChannelIfDisconnected(channel);
        Assert.assertNull(channel.getAttribute(CHANNEL_KEY));
    }

    @Test(expected = RemotingException.class)
    public void send() throws RemotingException {
        Channel channel = Mockito.mock(MockChannel.class);
        header = new HeaderExchangeChannel(channel);

        Request request = new Request();
        header.send(request);
        verify(channel).send(request, false);

        Response response = new Response();
        header.send(response);
        verify(channel).send(response, false);

        String st = "anything";
        header.send(st);
        verify(channel).send(st, false);

        Object testParam = new Object();
        header.send(testParam);
        ArgumentCaptor<Request> argumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(channel, times(4)).send(argumentCaptor.capture(), eq(false));
        Assert.assertEquals(argumentCaptor.getValue().getVersion(), Version.getProtocolVersion());
        Assert.assertEquals(argumentCaptor.getValue().getData(), testParam);

        initChannel();
        initHeader();
        header.close(1000);
        header.send(request);
    }

    @Test(expected = RemotingException.class)
    public void testRequest() throws RemotingException {
        Channel channel = Mockito.mock(MockChannel.class);
        header = new HeaderExchangeChannel(channel);
        when(channel.getUrl()).thenReturn(url);
        Object requestob = new Object();
        header.request(requestob);
        ArgumentCaptor<Request> argumentCaptor = ArgumentCaptor.forClass(Request.class);
        verify(channel, times(1)).send(argumentCaptor.capture());
        Assert.assertEquals(argumentCaptor.getValue().getData(), requestob);

        initChannel();
        initHeader();
        header.close(1000);
        header.request(requestob);
    }

    @Test
    public void testClose() {
        initChannel();
        initHeader();

        Assert.assertFalse(channel.isClosed());
        header.close();
        Assert.assertTrue(channel.isClosed());
    }

    @Test
    public void testCloseWithTimeout() {
        initChannel();
        initHeader();
        Assert.assertFalse(channel.isClosed());

        header.close(100);
    }


    @Test
    public void testStartClose() {
        initChannel();
        initHeader();
        try {
            Field field = MockChannel.class.getDeclaredField("closing");
            field.setAccessible(true);
            boolean isCloseing = (boolean)field.get(channel);
            Assert.assertFalse(isCloseing);
            header.startClose();
            isCloseing = (boolean)field.get(channel);
            Assert.assertTrue(isCloseing);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetLocalAddress() {
        initChannel();
        initHeader();
        Assert.assertNull(header.getLocalAddress());
    }

    @Test
    public void testGetremoteAddress() {
        initChannel();
        initHeader();
        Assert.assertNull(header.getRemoteAddress());
    }

    @Test
    public void testGetUrl() {
        initChannel();
        initHeader();
        Assert.assertEquals(header.getUrl(), URL.valueOf("dubbo://localhost:20880"));
    }

    @Test
    public void testIsConnected() {
        initChannel();
        initHeader();
        Assert.assertFalse(header.isConnected());
    }


    @Test
    public void testGetChannelHandler() {
        initChannel();
        initHeader();
        Assert.assertNull(header.getChannelHandler());
    }

    @Test
    public void testGetExchangeHandler() {
        initChannel();
        initHeader();
        Assert.assertNull(header.getChannelHandler());
    }

    @Test
    public void testHasAttributeAndSetAttribute() {
        initChannel();
        initHeader();
        Assert.assertFalse(header.hasAttribute("test"));
        header.setAttribute("test", "test");
        Assert.assertTrue(header.hasAttribute("test"));

    }

    @Test
    public void testGetAttributeAndSetAttribute() {
        initChannel();
        initHeader();
        header.setAttribute("test", "test");
        Assert.assertEquals(header.getAttribute("test"), "test");
        Assert.assertTrue(header.hasAttribute("test"));
    }

    @Test
    public void testRemoveAttribute() {
        initChannel();
        initHeader();
        header.setAttribute("test", "test");
        Assert.assertEquals(header.getAttribute("test"), "test");
        header.removeAttribute("test");
        Assert.assertFalse(header.hasAttribute("test"));
    }
    @Test
    public void testIsColsed(){
        initChannel();
        initHeader();
        Assert.assertFalse(header.isClosed());
    }

    @Test
    public void testHahsCode(){
        initChannel();
        initHeader();

        final int prime = 31;
        int result = 1;
        result = prime * result + ((channel == null) ? 0 : channel.hashCode());

        Assert.assertEquals(header.hashCode(),result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEqual(){
        initChannel();
        initHeader();
        Assert.assertTrue(header.equals(new HeaderExchangeChannel(channel)));
        header = new HeaderExchangeChannel(null);
        Assert.assertFalse(header.equals(new HeaderExchangeChannel(channelConnectedTrue)));
    }


    @Test
    public void testToString(){
        initChannel();
        initHeader();
        Assert.assertEquals(header.toString(),channel.toString());
    }
}

