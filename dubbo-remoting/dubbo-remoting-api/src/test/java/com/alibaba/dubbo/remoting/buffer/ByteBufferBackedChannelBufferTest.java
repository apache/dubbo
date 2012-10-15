package com.alibaba.dubbo.remoting.buffer;

import java.nio.ByteBuffer;

/**
 * @author <a href="mailto:gang.lvg@taobao.com">kimi</a>
 */
public class ByteBufferBackedChannelBufferTest extends AbstractChannelBufferTest {

    private ChannelBuffer buffer;

    @Override
    protected ChannelBuffer newBuffer(int capacity) {
        buffer = new ByteBufferBackedChannelBuffer(ByteBuffer.allocate(capacity));
        return buffer;
    }

    @Override
    protected ChannelBuffer[] components() {
        return new ChannelBuffer[]{buffer};
    }
}
