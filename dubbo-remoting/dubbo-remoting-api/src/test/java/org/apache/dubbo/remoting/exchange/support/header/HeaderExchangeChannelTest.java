package org.apache.dubbo.remoting.exchange.support.header;

import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ResponseFuture;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;


public class HeaderExchangeChannelTest {

    private HeaderExchangeChannel headerExchangeChannel;


    private Channel mchannel = Mockito.mock(Channel.class);

    private static final String CHANNEL_KEY = HeaderExchangeChannel.class.getName() + ".CHANNEL";

    @Before
    public void before(){

        headerExchangeChannel = new HeaderExchangeChannel(mchannel);

    }

    @Test
    public void sendTest() throws RemotingException{
        Assert.assertFalse(headerExchangeChannel.isClosed());

        headerExchangeChannel.send("Test", false);
        Mockito.verify(mchannel).send("Test",false);

    }

    @Test
    public void requestTest() throws RemotingException{
        Object request = new Object();
        int timeout = 11;
        ResponseFuture responseFuture = headerExchangeChannel.request(request, timeout);
    }

    @Test
    public void closeTest(){
        int timeout1 = -2000;
        int timeout2 = 0;
        int timeout3 = 2000;

        headerExchangeChannel.close(timeout1);
        headerExchangeChannel.close(timeout2);
        headerExchangeChannel.close(timeout3);

        Assert.assertFalse(DefaultFuture.hasFuture(mchannel));
        Mockito.verify(mchannel).close();
    }

    @Test
    public void getOrAddChannelTest(){
        Mockito.when(mchannel.isConnected()).thenReturn(true);

        HeaderExchangeChannel.getOrAddChannel(mchannel);


        Mockito.verify(mchannel).getAttribute(CHANNEL_KEY);


        Mockito.verify(mchannel).setAttribute(CHANNEL_KEY, new HeaderExchangeChannel(mchannel));
    }

    @Test
    public void isConnectedTest(){
        headerExchangeChannel.isConnected();

        Mockito.verify(mchannel).isConnected();
    }

    @Test
    public void getAttributeTest(){
        headerExchangeChannel.getAttribute("Key");

        Mockito.verify(mchannel).getAttribute("Key");
    }

    @Test
    public void removeAttributeTest(){
        headerExchangeChannel.removeAttribute("Key");

        Mockito.verify(mchannel).removeAttribute("Key");
    }

    @Test
    public void removeChannelIfDisconnectedTest(){
        Mockito.when(mchannel.isConnected()).thenReturn(false);

        HeaderExchangeChannel.removeChannelIfDisconnected(mchannel);

        Mockito.verify(mchannel).removeAttribute(CHANNEL_KEY);
    }
}
