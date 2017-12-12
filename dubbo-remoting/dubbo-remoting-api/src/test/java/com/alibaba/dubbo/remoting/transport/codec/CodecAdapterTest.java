package com.alibaba.dubbo.remoting.transport.codec;

import com.alibaba.dubbo.remoting.codec.ExchangeCodecTest;

import org.junit.Before;

/**
 * @author <a href="mailto:gang.lvg@taobao.com">kimi</a>
 */
public class CodecAdapterTest extends ExchangeCodecTest {

    @Before
    public void setUp() throws Exception {
        codec = new CodecAdapter(new DeprecatedExchangeCodec());
    }

}
