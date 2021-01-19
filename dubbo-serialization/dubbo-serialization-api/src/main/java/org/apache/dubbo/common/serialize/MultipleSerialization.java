package org.apache.dubbo.common.serialize;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@SPI
public interface MultipleSerialization {

    void serialize(URL url, String serializeType, String clz, Object obj, OutputStream os) throws IOException;

    Object deserialize(URL url,String serializeType, String clz, InputStream os) throws IOException, ClassNotFoundException;

}
