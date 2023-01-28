package org.apache.dubbo.rpc.protocol.rest.util;

import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.rpc.protocol.rest.exception.UnSupportContentTypeException;

import java.util.Arrays;
import java.util.List;

public class MediaTypeUtil {
    private static final List<MediaType> mediaTypes = Arrays.asList(MediaType.values());

    public static MediaType convertMediaType(String contentType) {

        for (MediaType mediaType : mediaTypes) {

            if (contentType != null && contentType.contains(mediaType.value)) {
                return mediaType;
            }
        }

        throw new UnSupportContentTypeException(contentType);

    }
}
