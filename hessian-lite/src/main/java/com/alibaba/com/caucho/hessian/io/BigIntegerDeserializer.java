package com.alibaba.com.caucho.hessian.io;

import java.math.BigInteger;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class BigIntegerDeserializer extends JavaDeserializer {

    public BigIntegerDeserializer() {
        super(BigInteger.class);
    }

    @Override
    protected Object instantiate() throws Exception {
        return new BigInteger("0");
    }
}
