package org.apache.dubbo.remoting.exchange.support.header;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.*;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class HeaderExchangeChannelTest {

    private URL url = URL.valueOf("dubbo://localhost:20880");
    private MockChannel channel;
    private HeaderExchangeChannel exchangeChannel;

    @Before
    public void setup() throws Exception {
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
                // ignore
            }
        });
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


        // Assert.assertTrue(exchangeChannel.isClosed());

    }

}

