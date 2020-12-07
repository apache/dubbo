package org.apache.dubbo.remoting.transport.netty4.marshaller;


import java.io.InputStream;

/**
 * convert Object from/to InputStream
 */
public interface Marshaller {

    Object unmarshal(InputStream in);

    InputStream marshal(Object arg);

}
