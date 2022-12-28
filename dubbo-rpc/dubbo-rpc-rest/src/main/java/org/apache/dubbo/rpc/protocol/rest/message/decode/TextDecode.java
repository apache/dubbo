package org.apache.dubbo.rpc.protocol.rest.message.decode;

import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.rpc.protocol.rest.message.AbstractMessageDecode;
import org.apache.dubbo.rpc.protocol.rest.message.MediaTypeMatcher;
import org.apache.dubbo.rpc.protocol.rest.util.DataParseUtils;
import org.apache.dubbo.rpc.protocol.rest.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.Charset;

@Adaptive("text")
public class TextDecode extends AbstractMessageDecode {


    @Override
    public Object decode(InputStream inputStream, Class targetType) throws Exception {
        return DataParseUtils.StringTypeConvert(targetType, StreamUtils.copyToString(inputStream, Charset.defaultCharset()));
    }

    @Override
    public boolean contentTypeSupport(MediaType mediaType) {
        return MediaTypeMatcher.TEXT_PLAIN.mediaSupport(mediaType);
    }
}
