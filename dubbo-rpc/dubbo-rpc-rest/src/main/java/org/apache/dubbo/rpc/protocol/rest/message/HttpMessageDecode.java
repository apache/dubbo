package org.apache.dubbo.rpc.protocol.rest.message;

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.metadata.rest.media.MediaType;

import java.io.InputStream;

@SPI
public interface HttpMessageDecode {

    Object decode(InputStream inputStream, Class targetType) throws Exception;

    boolean contentTypeSupport(MediaType mediaType);


}
