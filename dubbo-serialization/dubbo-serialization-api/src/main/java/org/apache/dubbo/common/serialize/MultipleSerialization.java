package org.apache.dubbo.common.serialize;

import org.apache.dubbo.common.extension.SPI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@SPI
public interface MultipleSerialization {

    void serialize(String serializeType, String clz, Object obj, OutputStream os) throws IOException;

    Object deserialize(String serializeType, String clz, InputStream os) throws IOException, ClassNotFoundException;

}
