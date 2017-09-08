package com.alibaba.dubbo.remoting.buffer;

import junit.framework.Assert;

/**
 * @author <a href="mailto:gang.lvg@taobao.com">kimi</a>
 */
public class DirectChannelBufferTest extends AbstractChannelBufferTest {

    private ChannelBuffer buffer;

    @Override
    protected ChannelBuffer newBuffer(int capacity) {
        buffer = ChannelBuffers.directBuffer(capacity);
        Assert.assertEquals(0, buffer.writerIndex());
        return buffer;
    }

    @Override
    protected ChannelBuffer[] components() {
        return new ChannelBuffer[]{buffer};
    }
}
