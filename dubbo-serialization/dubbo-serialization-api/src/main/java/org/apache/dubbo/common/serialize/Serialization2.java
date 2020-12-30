package org.apache.dubbo.common.serialize;

import org.apache.dubbo.common.extension.SPI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@SPI
public interface Serialization2 {

    Object deserialize(InputStream in, Class<?> clz) throws IOException;

    /**
     * Serialize object
     *
     * @param obj object to serialize
     * @param os  output stream
     * @return serializedSize
     * @throws IOException when serialize error
     */
    int serialize(Object obj, OutputStream os) throws IOException;
}
