package org.apache.dubbo.rpc.protocol.rest.message;

import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.protocol.rest.exception.UnSupportContentTypeException;

import java.io.InputStream;
import java.util.Set;

public class HttpMessageCodec {
    private static final Set<HttpMessageDecode> httpMessageDecodes =
        ApplicationModel.defaultModel().getExtensionLoader(HttpMessageDecode.class).getSupportedExtensionInstances();


    public static Object httpMessageDecode(InputStream inputStream, Class type, MediaType mediaType) throws Exception {
        for (HttpMessageDecode httpMessageDecode : httpMessageDecodes) {
            if (httpMessageDecode.contentTypeSupport(mediaType)) {
                return httpMessageDecode.decode(inputStream, type);
            }
        }
        throw new UnSupportContentTypeException("UnSupport content-type :" + mediaType.value);
    }

}
