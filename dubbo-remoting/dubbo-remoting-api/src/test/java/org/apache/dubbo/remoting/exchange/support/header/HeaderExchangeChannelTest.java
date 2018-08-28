package org.apache.dubbo.remoting.exchange.support.header;

import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;


public class HeaderExchangeChannelTest {

    private static final String CHANNEL_KEY = HeaderExchangeChannel.class.getName() + ".CHANNEL";

    private Channel mockChannel;
    private HeaderExchangeChannel mHeaderExchangeChannel;

    @Before
    public void runBeforeTestMethod() {
        mockChannel = Mockito.mock(Channel.class);
        mHeaderExchangeChannel = new HeaderExchangeChannel(mockChannel);
    }


    @Test
    public void getAttribute() {
        Mockito.when(mockChannel.getAttribute("mock")).thenReturn("mockAttr");
        Assert.assertEquals("mockAttr", mHeaderExchangeChannel.getAttribute("mock"));
    }


    @Test
    public void removeChannelIfDisconnected_one() {
        Mockito.when(mockChannel.isConnected()).thenReturn(true);
        Mockito.doThrow(new RuntimeException("removeChannelIfDisconnected_one"))
                .when(mockChannel)
                .removeAttribute(CHANNEL_KEY);
        try {
            HeaderExchangeChannel.removeChannelIfDisconnected(mockChannel);
        }catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void removeChannelIfDisconnected_three() {
        Mockito.when(mockChannel.isConnected()).thenReturn(false);
        Mockito.doThrow(new RuntimeException("removeChannelIfDisconnected_one"))
                .when(mockChannel)
                .removeAttribute(CHANNEL_KEY);
        try {
            HeaderExchangeChannel.removeChannelIfDisconnected(mockChannel);
            Assert.fail();
        }catch (Exception e) {

        }
    }

    @Test
    public void removeAttribute() {
        Mockito.doThrow(new RuntimeException("removeAttribute"))
                .when(mockChannel)
                .removeAttribute("mock");
        try {
            mHeaderExchangeChannel.removeAttribute("mock");
            Assert.fail();
        }catch (Exception e) {

        }
    }

    @Test
    public void isConnected() {
        Mockito.when(mockChannel.isConnected()).thenReturn(true);
        boolean result = mHeaderExchangeChannel.isConnected();
        Assert.assertEquals(true, result);
    }

    @Test
    public void getOrAddChannelTest1() {
        // 参数为null
        Assert.assertEquals(null, HeaderExchangeChannel.getOrAddChannel(null));
    }

    @Test
    public void getOrAddChannelTest2() {
        // 参数非null
        String CHANNEL_KEY = HeaderExchangeChannel.class.getName() + ".CHANNEL";
        Mockito.when(mockChannel.getAttribute(CHANNEL_KEY)).thenReturn(mHeaderExchangeChannel);

        Assert.assertEquals(mHeaderExchangeChannel, HeaderExchangeChannel.getOrAddChannel(mockChannel));
    }

    @Test
    public void getOrAddChannelTest3() {
        String CHANNEL_KEY = HeaderExchangeChannel.class.getName() + ".CHANNEL";

        // 属性返回null
        // isConnected返回false
        Mockito.when(mockChannel.getAttribute(CHANNEL_KEY)).thenReturn(null);

        Mockito.when(mockChannel.isConnected()).thenReturn(false);

        Assert.assertEquals(null, HeaderExchangeChannel.getOrAddChannel(mockChannel).getAttribute(CHANNEL_KEY));
    }


    @Test
    public void getOrAddChannelTest4() {
        String CHANNEL_KEY = HeaderExchangeChannel.class.getName() + ".CHANNEL";

        // 属性返回null
        Mockito.when(mockChannel.getAttribute(CHANNEL_KEY)).thenReturn(null);

        // isConnected返回true
        Mockito.when(mockChannel.isConnected()).thenReturn(true);

        Mockito.doThrow(new RuntimeException("setAttribute执行")).when(mockChannel).setAttribute(CHANNEL_KEY, mHeaderExchangeChannel);

        try {
            HeaderExchangeChannel.getOrAddChannel(mockChannel);
        } catch (Exception e) {
            System.out.println(e);
            System.out.println("setAttribute执行成功");
        }
    }

    @Test
    public void test_send_one() throws RemotingException {
        Request request = Mockito.mock(Request.class);
        Mockito.doThrow(new RuntimeException("send_request"))
                .when(mockChannel)
                .send(request, false);
        try {
            mHeaderExchangeChannel.send(request);
            Assert.fail();
        } catch (Exception e) {

        }
    }

    @Test
    public void test_send_two() throws RemotingException{
        Response response = Mockito.mock(Response.class);
        Mockito.doThrow(new RuntimeException())
                .when(mockChannel)
                .send(response, false);
        try {
            mHeaderExchangeChannel.send(response);
            Assert.fail();
        } catch (Exception e) {

        }
    }

    @Test
    public void test_send_three() throws RemotingException {
        String string = "test_send_three";
        Mockito.doThrow(new RuntimeException())
                .when(mockChannel)
                .send(string, false);
        try {
            mHeaderExchangeChannel.send(string);
            Assert.fail();
        } catch (Exception e) {

        }
    }

    @Test
    public void requestTest() throws RemotingException {
        Object obj = Mockito.mock(Object.class);

        Assert.assertNotEquals(null, mHeaderExchangeChannel.request(obj, 1000));
    }

    @Test
    public void close_test_timeout_one(){
        int timeout = 0;
        Mockito.doThrow(new RuntimeException("close_test_timeout_two")).when(mockChannel).close();
        try{
            mHeaderExchangeChannel.close(timeout);
        } catch (Exception e){
            Assert.fail();
        }
    }

}