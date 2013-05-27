package com.alibaba.dubbo.remoting.transport.codec;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.io.UnsafeByteArrayInputStream;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.Codec;
import com.alibaba.dubbo.remoting.buffer.ChannelBuffer;
import com.alibaba.dubbo.remoting.buffer.ChannelBuffers;
import com.alibaba.dubbo.remoting.codec.ExchangeCodecTest;
import com.alibaba.dubbo.remoting.telnet.codec.TelnetCodec;

import junit.framework.Assert;

/**
 * @author <a href="mailto:gang.lvg@taobao.com">kimi</a>
 */
public class CodecAdapterTest extends ExchangeCodecTest {

    @Before
    public void setUp() throws Exception {
        codec = new CodecAdapter(new DeprecatedExchangeCodec());
    }

}
