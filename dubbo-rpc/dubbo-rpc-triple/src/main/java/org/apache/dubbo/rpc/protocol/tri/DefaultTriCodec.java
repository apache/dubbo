package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.Codec2;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;

import java.io.IOException;

public class DefaultTriCodec implements Codec2 {

    @Override
    public void encode(Channel channel, ChannelBuffer buffer, Object message) throws IOException {

    }

    @Override
    public Object decode(Channel channel, ChannelBuffer buffer) throws IOException {
        return null;
    }
}
