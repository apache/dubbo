package com.alibaba.dubbo.common.serialize.support.hessian;

import java.math.BigInteger;

import com.alibaba.com.caucho.hessian.io.AbstractSerializerFactory;
import com.alibaba.com.caucho.hessian.io.Deserializer;
import com.alibaba.com.caucho.hessian.io.HessianProtocolException;
import com.alibaba.com.caucho.hessian.io.JavaDeserializer;
import com.alibaba.com.caucho.hessian.io.Serializer;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
class BigIntegerSerializerFactory extends AbstractSerializerFactory {

    static final BigIntegerSerializerFactory INSTANCE =
        new BigIntegerSerializerFactory();

    private static final Deserializer bigIntegerDeserializer =
        new JavaDeserializer(BigInteger.class) {

            @Override
            protected Object instantiate() throws Exception {
                return new BigInteger("0");
            }
        };

    public BigIntegerSerializerFactory() {
        super();
    }

    @Override
    public Serializer getSerializer(Class cl) throws HessianProtocolException {
        return null;
    }

    @Override
    public Deserializer getDeserializer(Class cl) throws HessianProtocolException {
        if (cl == BigInteger.class) {
            return bigIntegerDeserializer;
        }
        return null;
    }

}

