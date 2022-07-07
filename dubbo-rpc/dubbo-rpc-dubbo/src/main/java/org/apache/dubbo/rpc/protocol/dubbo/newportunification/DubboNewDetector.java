package org.apache.dubbo.rpc.protocol.dubbo.newportunification;

import org.apache.dubbo.remoting.api.newportunification.ChannelWithHandler;
import org.apache.dubbo.remoting.api.newportunification.NewProtocolDetector;
import org.apache.dubbo.remoting.buffer.AbstractChannelBuffer;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.buffer.ChannelBuffers;
import org.apache.dubbo.remoting.buffer.HeapChannelBufferFactory;


import static java.lang.Math.min;

public class DubboNewDetector implements NewProtocolDetector {

    // 0xda, 0xbb
    private final ChannelBuffer Preface = HeapChannelBufferFactory.getInstance().getBuffer(
        new byte[]{(byte) 0xda, (byte) 0xbb},
        0, 2
    );

    @Override
    public Result detect(ChannelWithHandler channel, ChannelBuffer in) {
        int prefaceLen = Preface.readableBytes();
        // don't consume byte array in ChannelBuffer, just read
        int bytesRead = min(in.readableBytes(), prefaceLen);

        if (bytesRead ==0 || !ChannelBuffers.equals(in, Preface, bytesRead)) {
            return Result.UNRECOGNIZED;
        }
        if (bytesRead == prefaceLen) {
            return Result.RECOGNIZED;
        }

        return Result.NEED_MORE_DATA;
    }
}
