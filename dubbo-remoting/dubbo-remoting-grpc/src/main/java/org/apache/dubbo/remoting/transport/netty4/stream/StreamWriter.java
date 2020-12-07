package org.apache.dubbo.remoting.transport.netty4.stream;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;


public class StreamWriter {

    public int streamId;
    private final Channel channel;

    public StreamWriter(Channel channel, int streamId){
        this.channel=channel;
        this.streamId=streamId;
    }
    public StreamWriter( Channel channel) {
        this.channel = channel;
    }

    public ChannelFuture write(Object obj, boolean flush) {
        if (flush) {
            return channel.writeAndFlush(obj);
        } else {
            return channel.write(obj);
        }
    }

    public int getStreamId() {
        return streamId;
    }

    public Channel getChannel() {
        return channel;
    }
}
