package org.apache.dubbo.remoting.exchange.support.header;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.util.List;


public class HeaderExchangeChannelTest {

    private URL url = URL.valueOf("dubbo://localhost:20880");

    private MockChannel channel;

    private HeaderExchangeChannel exchangeChannel;

    @Before
    public void setup() {
        channel = new MockChannel() {
            private boolean connected = true;
            @Override
            public URL getUrl() {
                return url;
            }

            @Override
            public boolean isConnected() {
                return connected;
            }
        };
        this.exchangeChannel = new HeaderExchangeChannel(channel);
    }

    @Test
    public void testSendRequest() throws RemotingException {
        Request request = Mockito.mock(Request.class);
        exchangeChannel.send(request);
        Assert.assertTrue(asserts(channel.getSentObjects()) instanceof Request);
    }

    @Test
    public void testSendString() throws RemotingException {
        String string = "";
        exchangeChannel.send(string);
        Assert.assertTrue(asserts(channel.getSentObjects()) instanceof String);
    }

    @Test
    public void testSendResponse() throws RemotingException {
        Response response = Mockito.mock(Response.class);
        exchangeChannel.send(response);
        Assert.assertTrue(asserts(channel.getSentObjects()) instanceof Response);
    }

    @Test
    public void testSendOther() throws RemotingException {
        Object object = Mockito.mock(Object.class);
        exchangeChannel.send(object);
        Object obj = asserts(channel.getSentObjects());
        Assert.assertTrue(obj instanceof Request);
        Assert.assertSame(((Request) obj).getData(), object);
    }

    @Test
    public void testSendClose() {
        exchangeChannel.close();
        Object object = Mockito.mock(Object.class);
        try {
            exchangeChannel.send(object);
        } catch (RemotingException e) {
            Assert.assertSame(e.getClass(), RemoteException.class);
        }
    }

    @Test
    public void testRequest() throws RemotingException {
        Object object = Mockito.mock(Object.class);
        exchangeChannel.request(object);
        Object obj = asserts(channel.getSentObjects());
        Assert.assertTrue(obj instanceof Request);
        Assert.assertSame(((Request) obj).getData(), object);
    }

    @Test
    public void testRequestClose() {
        exchangeChannel.close();
        Object object = Mockito.mock(Object.class);
        try {
            exchangeChannel.request(object);
        } catch (RemotingException e) {
            Assert.assertSame(e.getClass(), RemoteException.class);
        }
    }


    @Test
    public void testGetOrAddChannel(){
        HeaderExchangeChannel headerExchangeChannel = HeaderExchangeChannel.getOrAddChannel(channel);
        Assert.assertEquals(headerExchangeChannel.toString(),channel.toString());
    }

    @Test
    public void testClose() throws RemotingException {
        exchangeChannel.request(Mockito.mock(Object.class));
        exchangeChannel.close(5000);
        Assert.assertTrue(channel.isClosed());
        Assert.assertTrue(exchangeChannel.isClosed());
    }

    @Test
    public void testIsConnected(){
        Assert.assertEquals(exchangeChannel.isConnected(),channel.isConnected());
    }

    @Test
    public void testGetAttribute(){
        exchangeChannel.setAttribute("a","b");
        Assert.assertEquals(exchangeChannel.getAttribute("a"),"b");
    }

    @Test
    public void testRemoveAttribute(){
        exchangeChannel.setAttribute("a","b");
        Assert.assertNotNull(exchangeChannel.getAttribute("a"));
        exchangeChannel.removeAttribute("a");
        Assert.assertNull(exchangeChannel.getAttribute("a"));
    }

    @Test
    public void testRemoveChannelIfDisconnected(){
        HeaderExchangeChannel exchangeChannel = HeaderExchangeChannel.getOrAddChannel(channel);
        String key = getChannelKey();
        Assert.assertNotNull(key);
        Assert.assertNotNull(exchangeChannel.getAttribute(key));
        setValue(channel,false);
        HeaderExchangeChannel.removeChannelIfDisconnected(exchangeChannel);
        Assert.assertNull(exchangeChannel.getAttribute(key));
    }

    private Object asserts(List<Object> objects){
        Assert.assertTrue(objects.size() > 0);
        return objects.get(0);
    }

    private String getChannelKey(){
        try {
            Field field = HeaderExchangeChannel.class.getDeclaredField("CHANNEL_KEY");
            field.setAccessible(true);
            return (String) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void setValue(Object channel,boolean value){
        try {
            Field field = channel.getClass().getDeclaredField("connected");
            field.setAccessible(true);
            field.set(channel,value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
