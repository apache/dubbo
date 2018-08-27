package org.apache.dubbo.remoting.exchange.support.header;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.RemotingException;
import org.junit.Before;
import org.junit.Test;

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
    public void setup(){
        channel = new MockChannel() {

            @Override
            public URL getUrl() {
                return url;
            }
        };

        headerExchangeChannel = new HeaderExchangeChannel(channel);
    }

    @Test
    public void send() {
        try {
            headerExchangeChannel.send("hello dubbo");
        } catch (RemotingException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void send1() {
    }

    @Test
    public void request() {
    }

    @Test
    public void request1() {
    }

    @Test
    public void isClosed() {
    }

    @Test
    public void close() {
    }

    @Test
    public void close1() {
    }

    @Test
    public void startClose() {
    }

    @Test
    public void getLocalAddress() {
    }

    @Test
    public void getRemoteAddress() {
    }

    @Test
    public void getUrl() {
    }

    @Test
    public void isConnected() {
    }

    @Test
    public void getChannelHandler() {
    }

    @Test
    public void getExchangeHandler() {
    }

    @Test
    public void getAttribute() {
    }

    @Test
    public void setAttribute() {
    }

    @Test
    public void removeAttribute() {
    }

    @Test
    public void hasAttribute() {
    }
}