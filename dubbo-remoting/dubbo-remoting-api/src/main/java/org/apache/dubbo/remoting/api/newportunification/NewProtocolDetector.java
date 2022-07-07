package org.apache.dubbo.remoting.api.newportunification;

import org.apache.dubbo.remoting.api.ProtocolDetector;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;


public interface NewProtocolDetector {
    Result detect(ChannelWithHandler channel, ChannelBuffer in);

    enum Result {
        RECOGNIZED, UNRECOGNIZED, NEED_MORE_DATA
    }
}
