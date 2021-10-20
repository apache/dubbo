package org.apache.dubbo.remoting.transport;

import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Decodeable;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * {@link DecodeHandler}
 */
public class DecodeHandlerTest {

    @Test
    public void test() throws Exception {
        ChannelHandler handler = Mockito.mock(ChannelHandler.class);
        Channel channel = Mockito.mock(Channel.class);
        DecodeHandler decodeHandler = new DecodeHandler(handler);

        MockData mockData = new MockData();
        decodeHandler.received(channel, mockData);
        Assertions.assertTrue(mockData.isDecoded());

        MockData mockRequestData = new MockData();
        Request request = new Request(1);
        request.setData(mockRequestData);
        decodeHandler.received(channel, request);
        Assertions.assertTrue(mockRequestData.isDecoded());

        MockData mockResponseData = new MockData();
        Response response = new Response(1);
        response.setResult(mockResponseData);
        decodeHandler.received(channel, response);
        Assertions.assertTrue(mockResponseData.isDecoded());

        mockData.setThrowEx(true);
        decodeHandler.received(channel, mockData);
    }

    class MockData implements Decodeable {

        private boolean isDecoded = false;

        private boolean throwEx = false;

        @Override
        public void decode() throws Exception {
            if (throwEx) {
                throw new RuntimeException();
            }
            isDecoded = true;
        }

        public boolean isDecoded() {
            return isDecoded;
        }

        public void setThrowEx(boolean throwEx) {
            this.throwEx = throwEx;
        }
    }
}
