package com.alibaba.dubbo.remoting.buffer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:gang.lvg@taobao.com">kimi</a>
 */
public class DynamicChannelBufferTest extends AbstractChannelBufferTest {

    private ChannelBuffer buffer;

    @Override
    protected ChannelBuffer newBuffer(int length) {
        buffer = ChannelBuffers.dynamicBuffer(length);

        assertEquals(0, buffer.readerIndex());
        assertEquals(0, buffer.writerIndex());
        assertEquals(length, buffer.capacity());

        return buffer;
    }

    @Override
    protected ChannelBuffer[] components() {
        return new ChannelBuffer[]{buffer};
    }

    @Test
    public void shouldNotFailOnInitialIndexUpdate() {
        new DynamicChannelBuffer(10).setIndex(0, 10);
    }

    @Test
    public void shouldNotFailOnInitialIndexUpdate2() {
        new DynamicChannelBuffer(10).writerIndex(10);
    }

    @Test
    public void shouldNotFailOnInitialIndexUpdate3() {
        ChannelBuffer buf = new DynamicChannelBuffer(10);
        buf.writerIndex(10);
        buf.readerIndex(10);
    }
}

