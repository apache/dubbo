package org.apache.dubbo.rpc.protocol.rest.exception;

import org.apache.dubbo.metadata.rest.media.MediaType;

public class UnSupportContentTypeException extends RuntimeException {

    public UnSupportContentTypeException(String message) {

        super("Current Support content type: " + MediaType.getAllContentType() + message);
    }
}
