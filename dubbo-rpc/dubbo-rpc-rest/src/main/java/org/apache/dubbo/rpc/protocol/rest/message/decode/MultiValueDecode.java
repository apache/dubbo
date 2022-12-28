package org.apache.dubbo.rpc.protocol.rest.message.decode;

import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.rpc.protocol.rest.message.AbstractMessageDecode;
import org.apache.dubbo.rpc.protocol.rest.message.MediaTypeMatcher;
import org.apache.dubbo.rpc.protocol.rest.util.DataParseUtils;

import java.io.InputStream;
@Adaptive("multiValue")
public class MultiValueDecode extends AbstractMessageDecode {


    @Override
    public Object decode(InputStream inputStream, Class targetType) throws Exception {
        return DataParseUtils.multipartFormConvert(inputStream);
    }

    @Override
    public boolean contentTypeSupport(MediaType mediaType) {
        return MediaTypeMatcher.MULTI_VALUE.mediaSupport(mediaType);
    }
}
