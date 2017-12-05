package com.alibaba.dubbo.common.serialize.serialization;

import com.alibaba.dubbo.common.serialize.support.kryo.KryoSerialization;

/**
 * @author lishen
 */
public class FstSerializationTest extends AbstractSerializationTest {

    {
        serialization = new KryoSerialization();
    }
}
