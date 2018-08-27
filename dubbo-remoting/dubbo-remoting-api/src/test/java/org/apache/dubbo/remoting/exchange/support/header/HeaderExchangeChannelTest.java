package org.apache.dubbo.remoting.exchange.support.header;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.Request;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

public class HeaderExchangeChannelTest {
    private URL url = URL.valueOf("dubbo://localhost:8888");

    private MockChannel channel;

    @Test
    public void testGetOrAddChannel(){
        channel = new MockChannel();
        MockChannel channelTest = new MockChannel();
        HeaderExchangeChannel headerExchangeChannel = new HeaderExchangeChannel(channel);
        Assert.assertEquals(new HeaderExchangeChannel(channelTest),headerExchangeChannel.getOrAddChannel(channelTest));
    }

    @Test
    public  void testClose(){
        channel = new MockChannel();
        HeaderExchangeChannel headerExchangeChannel = new HeaderExchangeChannel(channel);
        headerExchangeChannel.close(1000);
        Assert.assertTrue(headerExchangeChannel.isClosed());
    }

    @Test
    public void testRequest() throws RemotingException,InterruptedException{
        channel = new MockChannel() {

            @Override
            public URL getUrl() {
                return url;
            }
        };
        HeaderExchangeChannel headerExchangeChannel = new HeaderExchangeChannel(channel);
        Request request = new Request();
        request.setVersion(Version.getProtocolVersion());
        request.setTwoWay(false);
        request.setData("this is a Test");

        headerExchangeChannel.request(request);
        List<Object> sentObjects =channel.getSentObjects();
        List<Object>  result = sentObjects.stream().map(p->{
            Request requestRes = (Request) p;
            return  requestRes.getData();
        }).collect(Collectors.toList());
        Assert.assertEquals(request,result.get(0));
    }
}
