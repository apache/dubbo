package org.apache.dubbo.remoting.http12.message.codec;

import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.message.MediaType;

public class OnlyDecodeStrategy extends DefaultSupportStrategy {

    public OnlyDecodeStrategy(MediaType mediaType) {
        super(mediaType);
    }

    @Override
    public boolean supportEncode(HttpHeaders headers) {
        return false;
    }
}
